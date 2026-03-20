package com.ridesharing.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridesharing.common.exception.ConflictException;
import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.Booking;
import com.ridesharing.domain.BookingStatus;
import com.ridesharing.domain.Trip;
import com.ridesharing.domain.TripStatus;
import com.ridesharing.domain.User;
import com.ridesharing.domain.Waypoint;
import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.BookingResponseDTO;
import com.ridesharing.event.BookingCancelledEvent;
import com.ridesharing.event.BookingConfirmedEvent;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.repository.WaypointRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int POINTS_TO_RUPEE = 1;    // 1 point = ₹1 discount
    // Points earned per ride: 5 base + 1 extra per ₹50 of fare, capped at 10
    private static int calcPointsEarned(float totalFare) {
        int extra = (int)(totalFare / 50);           // +1 point per ₹50 spent
        return Math.min(10, Math.max(5, 5 + extra)); // clamp between 5 and 10
    }

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WaypointRepository waypointRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * POST /trips/{id}/bookings — book seats on a trip.
     * @param redeemPoints how many loyalty points the passenger wants to redeem (0 = none)
     */
    @Transactional
    public BookingResponseDTO createBooking(UUID tripId, BookingRequestDTO dto, String passengerEmail, int redeemPoints) {
        User passenger = findUserByEmail(passengerEmail);

        if (passenger.isSuspended()) {
            throw new UnprocessableEntityException("USER_SUSPENDED",
                    "Your account has been suspended. You cannot create bookings.");
        }

        int seatsRequested = (dto.getSeats() != null && dto.getSeats() > 0) ? dto.getSeats() : 1;

        Trip trip = tripRepository.findByIdWithLock(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND",
                        "Trip not found with id: " + tripId));

        if (trip.getStatus() != TripStatus.OPEN) {
            throw new UnprocessableEntityException("TRIP_NOT_OPEN",
                    "Bookings can only be made on open trips.");
        }

        if (trip.getAvailableSeats() < seatsRequested) {
            throw new ConflictException("NO_SEATS_AVAILABLE",
                    "Not enough seats available. Requested: " + seatsRequested + ", Available: " + trip.getAvailableSeats());
        }

        Waypoint boardingWaypoint = null;
        if (dto.getBoardingWaypointId() != null) {
            boardingWaypoint = waypointRepository.findById(dto.getBoardingWaypointId())
                    .orElseThrow(() -> new ResourceNotFoundException("WAYPOINT_NOT_FOUND",
                            "Boarding waypoint not found: " + dto.getBoardingWaypointId()));
        }

        Waypoint alightingWaypoint = null;
        if (dto.getAlightingWaypointId() != null) {
            alightingWaypoint = waypointRepository.findById(dto.getAlightingWaypointId())
                    .orElseThrow(() -> new ResourceNotFoundException("WAYPOINT_NOT_FOUND",
                            "Alighting waypoint not found: " + dto.getAlightingWaypointId()));
        }

        // Apply loyalty points discount
        int pointsToRedeem = Math.min(redeemPoints, passenger.getLoyaltyPoints());
        float baseFare = (float)(trip.getBaseFarePerKm() * seatsRequested);
        float maxDiscount = baseFare * 0.5f; // max 50% discount
        float discount = Math.min(pointsToRedeem * POINTS_TO_RUPEE, maxDiscount);
        float fareLocked = Math.max(0, baseFare - discount);

        if (pointsToRedeem > 0) {
            passenger.setLoyaltyPoints(passenger.getLoyaltyPoints() - pointsToRedeem);
            userRepository.save(passenger);
            // Reward driver: each point redeemed = +1 trust score
            User driver = trip.getDriver();
            driver.setTrustScore(driver.getTrustScore() + pointsToRedeem);
            userRepository.save(driver);
        }

        trip.setAvailableSeats(trip.getAvailableSeats() - seatsRequested);
        tripRepository.save(trip);

        Booking booking = Booking.builder()
                .trip(trip)
                .passenger(passenger)
                .boardingWaypoint(boardingWaypoint)
                .alightingWaypoint(alightingWaypoint)
                .fareLocked(fareLocked)
                .distanceKm(0f)
                .seatsBooked(seatsRequested)
                .status(BookingStatus.PENDING)
                .tipAmount(Math.max(0f, dto.getTipAmount()))
                .build();

        booking = bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingConfirmedEvent(this, booking));
        eventPublisher.publishEvent(new com.ridesharing.event.PassengerBookedEvent(this, booking));

        BookingResponseDTO resp = toResponseDTO(booking);
        resp.setPointsRedeemed(pointsToRedeem);
        resp.setDiscountApplied(discount);
        resp.setRemainingPoints(passenger.getLoyaltyPoints());
        return resp;
    }

    /** Backward-compat overload with no points redemption */
    @Transactional
    public BookingResponseDTO createBooking(UUID tripId, BookingRequestDTO dto, String passengerEmail) {
        return createBooking(tripId, dto, passengerEmail, 0);
    }

    /**
     * Award loyalty points to all confirmed passengers when a trip is completed.
     * Called by TripService after status → COMPLETED.
     */
    @Transactional
    public void awardPointsForCompletedTrip(UUID tripId) {
        List<Booking> confirmed = bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED);
        for (Booking b : confirmed) {
            b.setStatus(BookingStatus.COMPLETED);
            bookingRepository.save(b);
            User passenger = b.getPassenger();
            // Points = 5–10 based on fare paid (5 base + 1 per ₹50, max 10)
            float farePaid = b.getFareLocked() * b.getSeatsBooked();
            int earned = calcPointsEarned(farePaid);
            passenger.setLoyaltyPoints(passenger.getLoyaltyPoints() + earned);
            userRepository.save(passenger);
        }
    }

    /**
     * GET /bookings/my — get all bookings for the authenticated passenger.
     */
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getMyBookings(String passengerEmail) {
        User passenger = findUserByEmail(passengerEmail);
        return bookingRepository.findByPassengerId(passenger.getId())
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * GET /bookings/trip/{tripId} — get all bookings for a trip (driver only).
     */
    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getTripBookings(UUID tripId, String driverEmail) {
        User driver = findUserByEmail(driverEmail);
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND",
                        "Trip not found with id: " + tripId));
        if (!trip.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("You are not the driver of this trip.");
        }
        return bookingRepository.findByTripId(tripId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    /**
     * PATCH /bookings/{id}/confirm — driver confirms a pending booking.
     */
    @Transactional
    public BookingResponseDTO confirmBooking(UUID bookingId, String driverEmail) {
        User driver = findUserByEmail(driverEmail);
        Booking booking = findBookingById(bookingId);

        if (!booking.getTrip().getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("You are not the driver of this trip.");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new UnprocessableEntityException("BOOKING_NOT_PENDING",
                    "Only pending bookings can be confirmed.");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponseDTO(bookingRepository.save(booking));
    }

    /**
     * PATCH /bookings/{id}/reject — driver rejects/cancels a booking.
     */
    @Transactional
    public BookingResponseDTO rejectBooking(UUID bookingId, String driverEmail) {
        User driver = findUserByEmail(driverEmail);
        Booking booking = findBookingById(bookingId);

        if (!booking.getTrip().getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("You are not the driver of this trip.");
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new UnprocessableEntityException("BOOKING_CANNOT_REJECT",
                    "This booking cannot be rejected.");
        }
        // Restore seat
        Trip trip = booking.getTrip();
        trip.setAvailableSeats(trip.getAvailableSeats() + booking.getSeatsBooked());
        tripRepository.save(trip);

        booking.setStatus(BookingStatus.CANCELLED);
        return toResponseDTO(bookingRepository.save(booking));
    }

    /**
     * DELETE /bookings/{id} — passenger cancels a booking (> 2 hours before departure).
     */
    @Transactional
    public void cancelBooking(UUID bookingId, String passengerEmail) {
        User passenger = findUserByEmail(passengerEmail);
        Booking booking = findBookingById(bookingId);

        if (!booking.getPassenger().getId().equals(passenger.getId())) {
            throw new AccessDeniedException("You are not the passenger of this booking.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new UnprocessableEntityException("BOOKING_NOT_CANCELLABLE",
                    "Only pending or confirmed bookings can be cancelled.");
        }

        Trip trip = booking.getTrip();
        if (!LocalDateTime.now().plusHours(2).isBefore(trip.getDepartureTime())) {
            throw new UnprocessableEntityException("CANCELLATION_TOO_LATE",
                    "Bookings can only be cancelled more than 2 hours before departure.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        trip.setAvailableSeats(trip.getAvailableSeats() + booking.getSeatsBooked());
        tripRepository.save(trip);

        eventPublisher.publishEvent(new BookingCancelledEvent(this, booking));
    }

    /**
     * GET /trips/{tripId}/co-passengers — list fellow passengers (name + first letter of phone) for a trip.
     * Only visible to passengers who have a PENDING or CONFIRMED booking on the same trip.
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getCoPassengers(UUID tripId, String requesterEmail) {
        User requester = findUserByEmail(requesterEmail);
        // Verify requester has a booking on this trip
        List<Booking> myBookings = bookingRepository.findByTripId(tripId).stream()
                .filter(b -> b.getPassenger().getId().equals(requester.getId()))
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.toList());
        if (myBookings.isEmpty()) {
            throw new AccessDeniedException("You do not have an active booking on this trip.");
        }
        return bookingRepository.findByTripId(tripId).stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> !b.getPassenger().getId().equals(requester.getId()))
                .map(b -> {
                    User p = b.getPassenger();
                    String phone = p.getPhone();
                    String maskedPhone = (phone != null && phone.length() >= 4)
                            ? phone.substring(0, phone.length() - 4) + "****" : "****";
                    Map<String, String> m = new HashMap<>();
                    m.put("name", p.getFullName());
                    m.put("phone", maskedPhone);
                    m.put("seats", String.valueOf(b.getSeatsBooked()));
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ---- Private helpers ----

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found: " + email));
    }

    private Booking findBookingById(UUID id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND",
                        "Booking not found with id: " + id));
    }

    private BookingResponseDTO toResponseDTO(Booking booking) {
        Trip trip = booking.getTrip();
        User driver = trip.getDriver();
        User passenger = booking.getPassenger();
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .tripId(trip.getId())
                .tripOrigin(trip.getOriginAddress())
                .tripDestination(trip.getDestinationAddress())
                .departureTime(trip.getDepartureTime())
                .passengerId(passenger.getId())
                .passengerName(passenger.getFullName())
                .passengerPhone(passenger.getPhone())
                .driverName(driver.getFullName())
                .driverPhone(driver.getPhone())
                .driverEmail(driver.getEmail())
                .boardingWaypointId(booking.getBoardingWaypoint() != null
                        ? booking.getBoardingWaypoint().getId() : null)
                .alightingWaypointId(booking.getAlightingWaypoint() != null
                        ? booking.getAlightingWaypoint().getId() : null)
                .fareLocked(booking.getFareLocked())
                .distanceKm(booking.getDistanceKm())
                .seats(booking.getSeatsBooked())
                .totalFare(booking.getFareLocked() * booking.getSeatsBooked())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .tipAmount(booking.getTipAmount())
                .build();
    }
}
