package com.ridesharing.service;

import com.ridesharing.client.GoogleMapsDistanceClient;
import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.domain.Booking;
import com.ridesharing.domain.BookingStatus;
import com.ridesharing.domain.Trip;
import com.ridesharing.domain.Waypoint;
import com.ridesharing.dto.FareResponseDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.WaypointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Computes fares using the Google Maps Distance Matrix API and locks fares
 * into confirmed bookings (Requirements 6.1–6.5).
 */
@Service
public class FareCalculatorService {

    private final GoogleMapsDistanceClient distanceClient;
    private final TripRepository tripRepository;
    private final WaypointRepository waypointRepository;
    private final BookingRepository bookingRepository;

    public FareCalculatorService(GoogleMapsDistanceClient distanceClient,
                                 TripRepository tripRepository,
                                 WaypointRepository waypointRepository,
                                 BookingRepository bookingRepository) {
        this.distanceClient = distanceClient;
        this.tripRepository = tripRepository;
        this.waypointRepository = waypointRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Estimates the fare for a trip segment between two waypoints.
     * Uses the trip's current base_fare_per_km (Requirement 6.4).
     *
     * @param tripId               the trip
     * @param boardingWaypointId   boarding waypoint (null → trip origin)
     * @param alightingWaypointId  alighting waypoint (null → trip destination)
     * @return fare estimate DTO
     */
    public FareResponseDTO estimateFare(UUID tripId,
                                        UUID boardingWaypointId,
                                        UUID alightingWaypointId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND",
                        "Trip not found: " + tripId));

        double[] boarding = resolveCoordinates(boardingWaypointId,
                trip.getOriginLat(), trip.getOriginLng());
        double[] alighting = resolveCoordinates(alightingWaypointId,
                trip.getDestinationLat(), trip.getDestinationLng());

        double distanceKm = distanceClient.getRoadDistanceKm(
                boarding[0], boarding[1], alighting[0], alighting[1]);

        BigDecimal fare = computeFare(trip.getBaseFarePerKm(), distanceKm);

        return FareResponseDTO.builder()
                .tripId(tripId)
                .boardingWaypointId(boardingWaypointId)
                .alightingWaypointId(alightingWaypointId)
                .distanceKm(distanceKm)
                .baseFarePerKm(trip.getBaseFarePerKm())
                .fare(fare)
                .locked(false)
                .build();
    }

    /**
     * Locks the fare into the booking at confirmation time (Requirement 6.7).
     * Sets Booking.fareLocked and Booking.distanceKm, then saves.
     *
     * @param bookingId the booking to lock
     */
    @Transactional
    public void lockFare(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND",
                        "Booking not found: " + bookingId));

        Trip trip = booking.getTrip();

        double[] boarding = resolveCoordinates(
                booking.getBoardingWaypoint() != null ? booking.getBoardingWaypoint().getId() : null,
                trip.getOriginLat(), trip.getOriginLng());
        double[] alighting = resolveCoordinates(
                booking.getAlightingWaypoint() != null ? booking.getAlightingWaypoint().getId() : null,
                trip.getDestinationLat(), trip.getDestinationLng());

        double distanceKm = distanceClient.getRoadDistanceKm(
                boarding[0], boarding[1], alighting[0], alighting[1]);

        BigDecimal fare = computeFare(trip.getBaseFarePerKm(), distanceKm);

        booking.setDistanceKm((float) distanceKm);
        booking.setFareLocked(fare.floatValue());
        bookingRepository.save(booking);
    }

    /**
     * Returns the fare for a confirmed booking (locked) or an estimate for others.
     * Used by FareController for GET /trips/{id}/fare (Requirement 6.4, 6.5).
     */
    public FareResponseDTO getFare(UUID tripId,
                                   UUID boardingWaypointId,
                                   UUID alightingWaypointId,
                                   UUID bookingId) {
        if (bookingId != null) {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null && booking.getStatus() == BookingStatus.CONFIRMED
                    && booking.getFareLocked() > 0) {
                Trip trip = booking.getTrip();
                return FareResponseDTO.builder()
                        .tripId(tripId)
                        .boardingWaypointId(boardingWaypointId)
                        .alightingWaypointId(alightingWaypointId)
                        .distanceKm(booking.getDistanceKm())
                        .baseFarePerKm(trip.getBaseFarePerKm())
                        .fare(BigDecimal.valueOf(booking.getFareLocked())
                                .setScale(2, RoundingMode.HALF_UP))
                        .locked(true)
                        .build();
            }
        }
        return estimateFare(tripId, boardingWaypointId, alightingWaypointId);
    }

    // ---- Private helpers ----

    /**
     * Computes fare = base_fare_per_km × road_distance_km, rounded to 2 decimal places.
     */
    private BigDecimal computeFare(double baseFarePerKm, double distanceKm) {
        return BigDecimal.valueOf(baseFarePerKm)
                .multiply(BigDecimal.valueOf(distanceKm))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Resolves a waypoint ID to [lat, lng], falling back to the provided defaults
     * when the ID is null (i.e., use trip origin/destination).
     */
    private double[] resolveCoordinates(UUID waypointId, double defaultLat, double defaultLng) {
        if (waypointId == null) {
            return new double[]{defaultLat, defaultLng};
        }
        Waypoint wp = waypointRepository.findById(waypointId)
                .orElseThrow(() -> new ResourceNotFoundException("WAYPOINT_NOT_FOUND",
                        "Waypoint not found: " + waypointId));
        return new double[]{wp.getLat(), wp.getLng()};
    }
}
