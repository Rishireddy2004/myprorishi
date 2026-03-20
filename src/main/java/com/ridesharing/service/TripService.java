package com.ridesharing.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.client.GoogleMapsGeocodingClient;
import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.BookingStatus;
import com.ridesharing.domain.Trip;
import com.ridesharing.domain.TripStatus;
import com.ridesharing.domain.User;
import com.ridesharing.domain.Waypoint;
import com.ridesharing.dto.TripRequestDTO;
import com.ridesharing.dto.TripResponseDTO;
import com.ridesharing.dto.TripStatusUpdateDTO;
import com.ridesharing.dto.TripUpdateDTO;
import com.ridesharing.dto.WaypointDTO;
import com.ridesharing.event.TripCancelledEvent;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.PlatformConfigRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.repository.VehicleRepository;
import com.ridesharing.repository.WaypointRepository;

import lombok.RequiredArgsConstructor;

@Service
public class TripService {

    private static final String SERVICE_FEE_RATE_KEY = "service_fee_rate";
    private static final double DEFAULT_SERVICE_FEE_RATE = 0.10;

    private final TripRepository tripRepository;
    private final WaypointRepository waypointRepository;
    private final BookingRepository bookingRepository;
    private final PlatformConfigRepository platformConfigRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final GoogleMapsGeocodingClient geocodingClient;
    private final ApplicationEventPublisher eventPublisher;
    private final BookingService bookingService;

    public TripService(TripRepository tripRepository, WaypointRepository waypointRepository,
                       BookingRepository bookingRepository, PlatformConfigRepository platformConfigRepository,
                       VehicleRepository vehicleRepository, UserRepository userRepository,
                       GoogleMapsGeocodingClient geocodingClient, ApplicationEventPublisher eventPublisher,
                       @Lazy BookingService bookingService) {
        this.tripRepository = tripRepository;
        this.waypointRepository = waypointRepository;
        this.bookingRepository = bookingRepository;
        this.platformConfigRepository = platformConfigRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.geocodingClient = geocodingClient;
        this.eventPublisher = eventPublisher;
        this.bookingService = bookingService;
    }

    /**
     * POST /trips — create a new trip.
     * Validates vehicle exists, geocodes addresses, persists Trip + Waypoints,
     * snapshots service_fee_rate from PlatformConfig.
     */
    @Transactional
    @CacheEvict(value = "tripSearch", allEntries = true)
    public TripResponseDTO createTrip(TripRequestDTO dto, String driverEmail) {
        User driver = findUserByEmail(driverEmail);

        // Reject suspended users (Requirement 11.4)
        if (driver.isSuspended()) {
            throw new UnprocessableEntityException("USER_SUSPENDED",
                    "Your account has been suspended. You cannot post trips.");
        }

        // Validate vehicle details exist (Requirement 3.1)
        vehicleRepository.findByUserId(driver.getId())
                .orElseThrow(() -> new UnprocessableEntityException("VEHICLE_REQUIRED",
                        "You must add vehicle details before posting a trip. Please add your vehicle information first."));

        // Validate max 5 intermediate waypoints (Requirement 3.2)
        if (dto.getIntermediateWaypoints() != null && dto.getIntermediateWaypoints().size() > 5) {
            throw new UnprocessableEntityException("TOO_MANY_WAYPOINTS",
                    "A trip can have at most 5 intermediate waypoints.");
        }

        // Geocode origin and destination (Requirement 3.2)
        double[] originCoords = geocodingClient.geocode(dto.getOriginAddress());
        double[] destCoords = geocodingClient.geocode(dto.getDestinationAddress());

        // Snapshot service_fee_rate from PlatformConfig (Requirement 3.3)
        double serviceFeeRate = platformConfigRepository.findById(SERVICE_FEE_RATE_KEY)
                .map(config -> Double.parseDouble(config.getValue()))
                .orElse(DEFAULT_SERVICE_FEE_RATE);

        Trip trip = Trip.builder()
                .driver(driver)
                .originAddress(dto.getOriginAddress())
                .originLat(originCoords[0])
                .originLng(originCoords[1])
                .destinationAddress(dto.getDestinationAddress())
                .destinationLat(destCoords[0])
                .destinationLng(destCoords[1])
                .departureTime(dto.getDepartureTime())
                .totalSeats(dto.getTotalSeats())
                .availableSeats(dto.getTotalSeats())
                .baseFarePerKm(dto.getBaseFarePerKm())
                .status(TripStatus.OPEN)
                .serviceFeeRate(serviceFeeRate)
                .build();

        trip = tripRepository.save(trip);

        // Build waypoints: origin (seq=0), intermediates, destination (seq=last)
        List<Waypoint> waypoints = new ArrayList<>();
        waypoints.add(buildWaypoint(trip, 0, dto.getOriginAddress(), originCoords[0], originCoords[1]));

        int seq = 1;
        if (dto.getIntermediateWaypoints() != null) {
            for (WaypointDTO wp : dto.getIntermediateWaypoints()) {
                double[] coords = geocodingClient.geocode(wp.getAddress());
                waypoints.add(buildWaypoint(trip, seq++, wp.getAddress(), coords[0], coords[1]));
            }
        }

        waypoints.add(buildWaypoint(trip, seq, dto.getDestinationAddress(), destCoords[0], destCoords[1]));
        waypointRepository.saveAll(waypoints);

        return toResponseDTO(trip, waypoints);
    }

    /**
     * GET /trips/my — get all trips posted by the authenticated driver.
     */
    @Transactional(readOnly = true)
    public List<TripResponseDTO> getOpenTrips() {
        return tripRepository.findByStatus(TripStatus.OPEN).stream()
                .sorted((a, b) -> Integer.compare(b.getDriver().getTrustScore(), a.getDriver().getTrustScore()))
                .map(trip -> {
                    List<Waypoint> waypoints = waypointRepository.findByTripIdOrderBySequenceOrder(trip.getId());
                    return toResponseDTO(trip, waypoints);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TripResponseDTO> getMyTrips(String driverEmail) {
        User driver = findUserByEmail(driverEmail);
        return tripRepository.findByDriverId(driver.getId()).stream()
                .map(trip -> {
                    List<Waypoint> waypoints = waypointRepository.findByTripIdOrderBySequenceOrder(trip.getId());
                    return toResponseDTO(trip, waypoints);
                })
                .collect(Collectors.toList());
    }

    /**
     * GET /trips/:id — return full trip detail including waypoints.
     */
    @Transactional(readOnly = true)
    public TripResponseDTO getTrip(UUID tripId) {
        Trip trip = findTripById(tripId);
        List<Waypoint> waypoints = waypointRepository.findByTripIdOrderBySequenceOrder(tripId);
        return toResponseDTO(trip, waypoints);
    }

    /**
     * PATCH /trips/:id — update availableSeats and/or baseFarePerKm on open trips.
     * Rejects if new availableSeats < confirmed booking count.
     */
    @Transactional
    @CacheEvict(value = "tripSearch", allEntries = true)
    public TripResponseDTO updateTrip(UUID tripId, TripUpdateDTO dto, String driverEmail) {
        Trip trip = findTripById(tripId);
        validateDriverOwnership(trip, driverEmail);

        if (trip.getStatus() != TripStatus.OPEN) {
            throw new UnprocessableEntityException("TRIP_NOT_OPEN",
                    "Only open trips can be updated.");
        }

        if (dto.getAvailableSeats() != null) {
            long confirmedCount = bookingRepository.countByTripIdAndStatus(tripId, BookingStatus.CONFIRMED);
            if (dto.getAvailableSeats() < confirmedCount) {
                throw new UnprocessableEntityException("SEATS_BELOW_CONFIRMED",
                        "Available seats cannot be less than the number of confirmed bookings (" + confirmedCount + ").");
            }
            trip.setAvailableSeats(dto.getAvailableSeats());
        }

        if (dto.getBaseFarePerKm() != null) {
            trip.setBaseFarePerKm(dto.getBaseFarePerKm());
        }

        trip = tripRepository.save(trip);
        List<Waypoint> waypoints = waypointRepository.findByTripIdOrderBySequenceOrder(tripId);
        return toResponseDTO(trip, waypoints);
    }

    /**
     * DELETE /trips/:id — cancel open trip if departure is > 2 hours away.
     * Dispatches TripCancelledEvent for confirmed passengers.
     */
    @Transactional
    @CacheEvict(value = "tripSearch", allEntries = true)
    public void cancelTrip(UUID tripId, String driverEmail) {
        Trip trip = findTripById(tripId);
        validateDriverOwnership(trip, driverEmail);

        if (trip.getStatus() != TripStatus.OPEN) {
            throw new UnprocessableEntityException("TRIP_NOT_OPEN",
                    "Only open trips can be cancelled.");
        }

        if (!LocalDateTime.now().plusHours(2).isBefore(trip.getDepartureTime())) {
            throw new UnprocessableEntityException("CANCELLATION_TOO_LATE",
                    "Trips can only be cancelled more than 2 hours before departure.");
        }

        trip.setStatus(TripStatus.CANCELLED);
        tripRepository.save(trip);

        // Publish event so NotificationService (task 14) can notify confirmed passengers
        eventPublisher.publishEvent(new TripCancelledEvent(this, trip));
    }

    /**
     * POST /trips/:id/reopen — undo a cancellation, restoring the trip to OPEN.
     * Only allowed if departure is still in the future.
     */
    @Transactional
    @CacheEvict(value = "tripSearch", allEntries = true)
    public TripResponseDTO reopenTrip(UUID tripId, String driverEmail) {
        Trip trip = findTripById(tripId);
        validateDriverOwnership(trip, driverEmail);

        if (trip.getStatus() != TripStatus.CANCELLED) {
            throw new UnprocessableEntityException("TRIP_NOT_CANCELLED",
                    "Only cancelled trips can be reopened.");
        }
        if (!LocalDateTime.now().isBefore(trip.getDepartureTime())) {
            throw new UnprocessableEntityException("TRIP_DEPARTED",
                    "Cannot reopen a trip whose departure time has already passed.");
        }

        trip.setStatus(TripStatus.OPEN);
        trip = tripRepository.save(trip);
        List<Waypoint> waypoints = waypointRepository.findByTripIdOrderBySequenceOrder(tripId);
        return toResponseDTO(trip, waypoints);
    }

    /**
     * PATCH /trips/:id/status — transition trip status.
     * open to in_progress: driver starts the trip.
     * in_progress to completed: driver marks trip done, awards loyalty points.
     */
    @Transactional
    public TripResponseDTO updateTripStatus(UUID tripId, TripStatusUpdateDTO dto, String driverEmail) {
        Trip trip = findTripById(tripId);
        validateDriverOwnership(trip, driverEmail);

        TripStatus current = trip.getStatus();
        TripStatus target = dto.getStatus();

        if (current == TripStatus.OPEN && target == TripStatus.IN_PROGRESS) {
            // Allow starting anytime (driver decides when to go)
            trip.setStatus(TripStatus.IN_PROGRESS);

        } else if (current == TripStatus.IN_PROGRESS && target == TripStatus.COMPLETED) {
            trip.setStatus(TripStatus.COMPLETED);
            trip = tripRepository.save(trip);
            // Award loyalty points to all confirmed passengers
            bookingService.awardPointsForCompletedTrip(tripId);
            List<Waypoint> waypoints = waypointRepository.findByTripIdOrderBySequenceOrder(tripId);
            return toResponseDTO(trip, waypoints);

        } else {
            throw new UnprocessableEntityException("INVALID_STATUS_TRANSITION",
                    "Invalid status transition from " + current + " to " + target + ".");
        }

        trip = tripRepository.save(trip);
        List<Waypoint> waypoints = waypointRepository.findByTripIdOrderBySequenceOrder(tripId);
        return toResponseDTO(trip, waypoints);
    }

    // ---- Private helpers ----

    private Trip findTripById(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND",
                        "Trip not found with id: " + tripId));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found: " + email));
    }

    private void validateDriverOwnership(Trip trip, String driverEmail) {
        if (!trip.getDriver().getEmail().equals(driverEmail)) {
            throw new AccessDeniedException("You are not the driver of this trip.");
        }
    }

    private Waypoint buildWaypoint(Trip trip, int seq, String address, double lat, double lng) {
        return Waypoint.builder()
                .trip(trip)
                .sequenceOrder(seq)
                .address(address)
                .lat(lat)
                .lng(lng)
                .build();
    }

    private TripResponseDTO toResponseDTO(Trip trip, List<Waypoint> waypoints) {
        List<WaypointDTO> waypointDTOs = waypoints.stream()
                .map(w -> WaypointDTO.builder()
                        .id(w.getId())
                        .sequenceOrder(w.getSequenceOrder())
                        .address(w.getAddress())
                        .lat(w.getLat())
                        .lng(w.getLng())
                        .build())
                .collect(Collectors.toList());

        return TripResponseDTO.builder()
                .id(trip.getId())
                .driverId(trip.getDriver().getId())
                .driverName(trip.getDriver().getFullName())
                .originAddress(trip.getOriginAddress())
                .originLat(trip.getOriginLat())
                .originLng(trip.getOriginLng())
                .destinationAddress(trip.getDestinationAddress())
                .destinationLat(trip.getDestinationLat())
                .destinationLng(trip.getDestinationLng())
                .departureTime(trip.getDepartureTime())
                .totalSeats(trip.getTotalSeats())
                .availableSeats(trip.getAvailableSeats())
                .baseFarePerKm(trip.getBaseFarePerKm())
                .pricePerSeat(trip.getBaseFarePerKm())
                .status(trip.getStatus())
                .serviceFeeRate(trip.getServiceFeeRate())
                .waypoints(waypointDTOs)
                .createdAt(trip.getCreatedAt())
                .driverTrustScore(trip.getDriver().getTrustScore())
                .driverRating(trip.getDriver().getAggregateRating())
                .driverCompletedTrips(bookingRepository.countCompletedTripsByDriverId(trip.getDriver().getId()))
                .build();
    }

    /**
     * Haversine formula to compute distance in km between two lat/lng points.
     */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
