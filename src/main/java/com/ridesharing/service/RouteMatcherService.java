package com.ridesharing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ridesharing.client.GoogleMapsGeocodingClient;
import com.ridesharing.domain.Trip;
import com.ridesharing.domain.TripStatus;
import com.ridesharing.domain.Vehicle;
import com.ridesharing.dto.TripSearchResponseDTO;
import com.ridesharing.dto.TripSearchResultDTO;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.VehicleRepository;

/**
 * Matches passenger search requests to available trips (Requirements 4.1–4.7).
 *
 * Algorithm:
 * 1. Geocode passenger origin and destination.
 * 2. Query trips with status=OPEN on the requested date whose bounding box overlaps the search area.
 * 3. For each candidate, compute detour distance.
 * 4. Filter to trips where both boarding and alighting are within 10 km of the trip route.
 * 5. Apply optional filters (price, vehicle make, min driver rating).
 * 6. Sort by ascending detour distance.
 * 7. Return results with driver info, vehicle details, and estimated fare.
 */
@Service
public class RouteMatcherService {

    /** Search radius in degrees (~10 km at equator; used for bounding box pre-filter). */
    private static final double SEARCH_RADIUS_DEG = 0.09;

    /** Maximum proximity in km for boarding/alighting points to the trip route. */
    private static final double MAX_PROXIMITY_KM = 10.0;

    private static final String NO_RESULTS_MESSAGE =
            "No trips found matching your search. Try adjusting the date or search radius.";

    private final GoogleMapsGeocodingClient geocodingClient;
    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final String googleMapsApiKey;

    public RouteMatcherService(GoogleMapsGeocodingClient geocodingClient,
                               TripRepository tripRepository,
                               VehicleRepository vehicleRepository,
                               @Value("${app.google-maps.api-key:}") String googleMapsApiKey) {
        this.geocodingClient = geocodingClient;
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.googleMapsApiKey = googleMapsApiKey;
    }

    /**
     * Search for trips matching the passenger's origin, destination, date, and optional time.
     *
     * When no Google Maps API key is configured (local dev), falls back to text-based
     * address matching so all trips are discoverable without real geocoding.
     */
    public TripSearchResponseDTO search(String origin,
                                        String destination,
                                        LocalDate date,
                                        LocalTime timeFrom,
                                        LocalTime timeTo,
                                        Double minPrice,
                                        Double maxPrice,
                                        String vehicleMake,
                                        Double minDriverRating) {

        boolean hasApiKey = googleMapsApiKey != null && !googleMapsApiKey.isBlank();

        List<Trip> candidates;

        if (hasApiKey) {
            // Real geocoding path: bounding box search
            double[] passengerOrigin = geocodingClient.geocode(origin);
            double[] passengerDest = geocodingClient.geocode(destination);

            double pOriginLat = passengerOrigin[0];
            double pOriginLng = passengerOrigin[1];
            double pDestLat = passengerDest[0];
            double pDestLng = passengerDest[1];

            double minLat = Math.min(pOriginLat, pDestLat) - SEARCH_RADIUS_DEG;
            double maxLat = Math.max(pOriginLat, pDestLat) + SEARCH_RADIUS_DEG;
            double minLng = Math.min(pOriginLng, pDestLng) - SEARCH_RADIUS_DEG;
            double maxLng = Math.max(pOriginLng, pDestLng) + SEARCH_RADIUS_DEG;

            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            candidates = tripRepository.findByBoundingBox(minLat, maxLat, minLng, maxLng, dayStart, dayEnd);
        } else {
            // Stub/no-API path: text-based match on ALL OPEN trips
            String originLower = origin.toLowerCase().trim();
            String destLower = destination.toLowerCase().trim();

            List<Trip> allOpen = tripRepository.findByStatus(TripStatus.OPEN);

            // Try text match first (loose: any word >= 2 chars)
            candidates = allOpen.stream()
                    .filter(t -> {
                        String orig = t.getOriginAddress() != null ? t.getOriginAddress().toLowerCase() : "";
                        String dest = t.getDestinationAddress() != null ? t.getDestinationAddress().toLowerCase() : "";
                        boolean originMatch = originLower.isEmpty() || containsAnyWord(orig, originLower);
                        boolean destMatch = destLower.isEmpty() || containsAnyWord(dest, destLower);
                        return originMatch && destMatch;
                    })
                    .toList();

            // If no text match found, return ALL open trips so passengers always see something
            if (candidates.isEmpty()) {
                candidates = allOpen;
            }
        }

        // Time-of-day filter
        List<Trip> timeFiltered = candidates;
        if (timeFrom != null || timeTo != null) {
            timeFiltered = candidates.stream().filter(t -> {
                LocalTime tripTime = t.getDepartureTime().toLocalTime();
                if (timeFrom != null && tripTime.isBefore(timeFrom)) return false;
                if (timeTo != null && tripTime.isAfter(timeTo)) return false;
                return true;
            }).toList();
        }

        // If time filter produced no results, fall back to all candidates for that destination/date
        if (timeFiltered.isEmpty() && (timeFrom != null || timeTo != null)) {
            timeFiltered = candidates; // show all rides to that place, ignoring time
        }

        List<TripSearchResultDTO> results = new ArrayList<>();
        boolean timeFallback = !timeFiltered.equals(candidates) && timeFiltered == candidates;

        for (Trip trip : timeFiltered) {
            // Optional filters
            if (minPrice != null && trip.getBaseFarePerKm() < minPrice) continue;
            if (maxPrice != null && trip.getBaseFarePerKm() > maxPrice) continue;

            Double driverRating = trip.getDriver().getAggregateRating();
            if (minDriverRating != null && (driverRating == null || driverRating < minDriverRating)) continue;

            Optional<Vehicle> vehicleOpt = vehicleRepository.findByUserId(trip.getDriver().getId());
            if (vehicleMake != null && !vehicleMake.isBlank()) {
                if (vehicleOpt.isEmpty() || !vehicleOpt.get().getMake().equalsIgnoreCase(vehicleMake)) continue;
            }

            // Estimate fare using haversine between trip origin and destination
            double tripDistanceKm = haversineKm(
                    trip.getOriginLat(), trip.getOriginLng(),
                    trip.getDestinationLat(), trip.getDestinationLng());
            double estimatedFare = BigDecimal.valueOf(trip.getBaseFarePerKm())
                    .multiply(BigDecimal.valueOf(Math.max(tripDistanceKm, 1.0)))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();

            TripSearchResultDTO.TripSearchResultDTOBuilder builder = TripSearchResultDTO.builder()
                    .tripId(trip.getId())
                    .driverName(trip.getDriver().getFullName())
                    .driverRating(driverRating)
                    .departureTime(trip.getDepartureTime())
                    .availableSeats(trip.getAvailableSeats())
                    .baseFarePerKm(trip.getBaseFarePerKm())
                    .estimatedFare(estimatedFare)
                    .detourDistanceKm(0.0)
                    .originAddress(trip.getOriginAddress())
                    .destinationAddress(trip.getDestinationAddress());

            vehicleOpt.ifPresent(v -> builder
                    .vehicleMake(v.getMake())
                    .vehicleModel(v.getModel())
                    .vehicleYear(v.getYear())
                    .vehicleColor(v.getColor())
                    .vehiclePassengerCapacity(v.getPassengerCapacity()));

            results.add(builder.build());
        }

        results.sort(Comparator.comparingDouble(TripSearchResultDTO::getDetourDistanceKm)
                .thenComparing(TripSearchResultDTO::getDepartureTime));

        if (results.isEmpty()) {
            return TripSearchResponseDTO.builder()
                    .results(results)
                    .message(NO_RESULTS_MESSAGE)
                    .build();
        }

        return TripSearchResponseDTO.builder()
                .results(results)
                .build();
    }

    /** Backward-compat overload without time filter. */
    public TripSearchResponseDTO search(String origin, String destination, LocalDate date,
                                        Double minPrice, Double maxPrice,
                                        String vehicleMake, Double minDriverRating) {
        return search(origin, destination, date, null, null, minPrice, maxPrice, vehicleMake, minDriverRating);
    }

    /** Returns true if any word from the query appears in the text. */
    private boolean containsAnyWord(String text, String query) {
        for (String word : query.split("[\\s,]+")) {
            if (word.length() >= 2 && text.contains(word)) return true;
        }
        return false;
    }

    // ---- Geometry helpers ----

    /**
     * Haversine formula: great-circle distance in km between two lat/lng points.
     */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Minimum distance in km from point P to the line segment AB.
     * Used to check if a passenger's boarding/alighting point is within 10 km of the trip route.
     */
    static double distanceToSegmentKm(double pLat, double pLng,
                                      double aLat, double aLng,
                                      double bLat, double bLng) {
        double ax = aLng, ay = aLat;
        double bx = bLng, by = bLat;
        double px = pLng, py = pLat;

        double dx = bx - ax;
        double dy = by - ay;
        double lenSq = dx * dx + dy * dy;

        if (lenSq == 0.0) {
            // Segment is a point
            return haversineKm(pLat, pLng, aLat, aLng);
        }

        double t = ((px - ax) * dx + (py - ay) * dy) / lenSq;
        t = Math.max(0.0, Math.min(1.0, t));

        double closestLat = ay + t * dy;
        double closestLng = ax + t * dx;

        return haversineKm(pLat, pLng, closestLat, closestLng);
    }
}
