package com.ridesharing.unit;

import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.*;
import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.BookingResponseDTO;
import com.ridesharing.event.BookingCancelledEvent;
import com.ridesharing.event.BookingConfirmedEvent;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.repository.WaypointRepository;
import com.ridesharing.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingService.
 * Validates: Requirements 5.1–5.6
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private TripRepository tripRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private WaypointRepository waypointRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingService bookingService;

    // ---- createBooking: seat decrement ----

    @Test
    void createBooking_decrementsAvailableSeats() {
        UUID tripId = UUID.randomUUID();
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildOpenTrip(tripId, 3, LocalDateTime.now().plusHours(5));

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdWithLock(tripId)).thenReturn(Optional.of(trip));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingRequestDTO dto = new BookingRequestDTO();
        bookingService.createBooking(tripId, dto, "passenger@example.com");

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        assertThat(tripCaptor.getValue().getAvailableSeats()).isEqualTo(2);
    }

    @Test
    void createBooking_publishesBookingConfirmedEvent() {
        UUID tripId = UUID.randomUUID();
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildOpenTrip(tripId, 2, LocalDateTime.now().plusHours(5));

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(tripRepository.findByIdWithLock(tripId)).thenReturn(Optional.of(trip));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.createBooking(tripId, new BookingRequestDTO(), "passenger@example.com");

        verify(eventPublisher).publishEvent(any(BookingConfirmedEvent.class));
    }

    // ---- cancelBooking: seat restore ----

    @Test
    void cancelBooking_restoresAvailableSeats() {
        UUID bookingId = UUID.randomUUID();
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildOpenTrip(UUID.randomUUID(), 1, LocalDateTime.now().plusHours(5));
        Booking booking = buildConfirmedBooking(bookingId, trip, passenger);

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(bookingId, "passenger@example.com");

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        assertThat(tripCaptor.getValue().getAvailableSeats()).isEqualTo(2);
    }

    @Test
    void cancelBooking_setsBookingStatusToCancelled() {
        UUID bookingId = UUID.randomUUID();
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildOpenTrip(UUID.randomUUID(), 1, LocalDateTime.now().plusHours(5));
        Booking booking = buildConfirmedBooking(bookingId, trip, passenger);

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(bookingId, "passenger@example.com");

        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void cancelBooking_publishesBookingCancelledEvent() {
        UUID bookingId = UUID.randomUUID();
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildOpenTrip(UUID.randomUUID(), 1, LocalDateTime.now().plusHours(5));
        Booking booking = buildConfirmedBooking(bookingId, trip, passenger);

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingService.cancelBooking(bookingId, "passenger@example.com");

        verify(eventPublisher).publishEvent(any(BookingCancelledEvent.class));
    }

    // ---- cancelBooking: 2-hour window boundary ----

    @Test
    void cancelBooking_exactlyTwoHoursBeforeDeparture_throwsUnprocessable() {
        UUID bookingId = UUID.randomUUID();
        User passenger = buildUser("passenger@example.com");
        // Departure exactly 2 hours from now — should fail (must be MORE than 2h)
        Trip trip = buildOpenTrip(UUID.randomUUID(), 1, LocalDateTime.now().plusHours(2));
        Booking booking = buildConfirmedBooking(bookingId, trip, passenger);

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, "passenger@example.com"))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("2 hours");
    }

    @Test
    void cancelBooking_moreThanTwoHoursBeforeDeparture_succeeds() {
        UUID bookingId = UUID.randomUUID();
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildOpenTrip(UUID.randomUUID(), 1, LocalDateTime.now().plusHours(3));
        Booking booking = buildConfirmedBooking(bookingId, trip, passenger);

        when(userRepository.findByEmail("passenger@example.com")).thenReturn(Optional.of(passenger));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw
        bookingService.cancelBooking(bookingId, "passenger@example.com");
    }

    // ---- helpers ----

    private User buildUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .fullName("Test User")
                .build();
    }

    private Trip buildOpenTrip(UUID tripId, int availableSeats, LocalDateTime departureTime) {
        return Trip.builder()
                .id(tripId)
                .driver(buildUser("driver@example.com"))
                .availableSeats(availableSeats)
                .totalSeats(availableSeats + 1)
                .departureTime(departureTime)
                .status(TripStatus.OPEN)
                .baseFarePerKm(5.0)
                .serviceFeeRate(0.1)
                .originAddress("Origin")
                .destinationAddress("Destination")
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .build();
    }

    private Booking buildConfirmedBooking(UUID bookingId, Trip trip, User passenger) {
        return Booking.builder()
                .id(bookingId)
                .trip(trip)
                .passenger(passenger)
                .status(BookingStatus.CONFIRMED)
                .fareLocked(50.0f)
                .distanceKm(10.0f)
                .seatsBooked(1)
                .build();
    }
}
