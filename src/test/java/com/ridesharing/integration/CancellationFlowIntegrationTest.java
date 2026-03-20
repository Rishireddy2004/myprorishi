package com.ridesharing.integration;

import com.ridesharing.domain.BookingStatus;
import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.BookingResponseDTO;
import com.ridesharing.dto.TripRequestDTO;
import com.ridesharing.dto.TripResponseDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Integration test: cancellation flows.
 * Validates Requirements 12.2, 12.4
 */
class CancellationFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Cancel > 24h before departure: booking cancelled, seat restored.
     */
    @Test
    void cancellationMoreThan24h_bookingCancelledAndSeatRestored() {
        // Setup: driver + passenger + trip + booking
        String driverToken = registerAndLogin("driver2@test.com", "password123", "Driver Two", "+3333333333");
        String passengerToken = registerAndLogin("passenger2@test.com", "password123", "Passenger Two", "+4444444444");
        addVehicleForUser("driver2@test.com");

        TripRequestDTO tripRequest = TripRequestDTO.builder()
                .originAddress("Bangalore")
                .destinationAddress("Mysore")
                .departureTime(LocalDateTime.now().plusDays(3)) // > 24h away
                .totalSeats(2)
                .baseFarePerKm(5.0)
                .build();

        ResponseEntity<TripResponseDTO> tripResp = post("/trips", tripRequest, driverToken, TripResponseDTO.class);
        TripResponseDTO trip = tripResp.getBody();

        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setPassengerName("Passenger Two");
        bookingRequest.setContactNumber("+4444444444");

        ResponseEntity<BookingResponseDTO> bookingResp = post(
                "/trips/" + trip.getId() + "/bookings",
                bookingRequest,
                passengerToken,
                BookingResponseDTO.class
        );
        BookingResponseDTO booking = bookingResp.getBody();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Verify seat was decremented
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getAvailableSeats()).isEqualTo(1);

        // Cancel the booking
        ResponseEntity<Void> cancelResp = delete("/bookings/" + booking.getId(), passengerToken, Void.class);
        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify booking is CANCELLED
        var cancelledBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // Verify seat was restored
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getAvailableSeats()).isEqualTo(2);
    }

    /**
     * Cancel 2-24h before departure: booking cancelled, seat restored.
     * The 50% refund logic is handled by PaymentService (mocked here since no real payment intent).
     */
    @Test
    void cancellationBetween2And24h_bookingCancelledAndSeatRestored() {
        // Setup: driver + passenger + trip departing in ~12h
        String driverToken = registerAndLogin("driver3@test.com", "password123", "Driver Three", "+5555555555");
        String passengerToken = registerAndLogin("passenger3@test.com", "password123", "Passenger Three", "+6666666666");
        addVehicleForUser("driver3@test.com");

        TripRequestDTO tripRequest = TripRequestDTO.builder()
                .originAddress("Bangalore")
                .destinationAddress("Mysore")
                .departureTime(LocalDateTime.now().plusHours(12)) // 2-24h window
                .totalSeats(2)
                .baseFarePerKm(5.0)
                .build();

        ResponseEntity<TripResponseDTO> tripResp = post("/trips", tripRequest, driverToken, TripResponseDTO.class);
        TripResponseDTO trip = tripResp.getBody();

        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setPassengerName("Passenger Three");
        bookingRequest.setContactNumber("+6666666666");

        ResponseEntity<BookingResponseDTO> bookingResp = post(
                "/trips/" + trip.getId() + "/bookings",
                bookingRequest,
                passengerToken,
                BookingResponseDTO.class
        );
        BookingResponseDTO booking = bookingResp.getBody();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // Cancel the booking (within 2-24h window)
        ResponseEntity<Void> cancelResp = delete("/bookings/" + booking.getId(), passengerToken, Void.class);
        assertThat(cancelResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify booking is CANCELLED
        var cancelledBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        // Verify seat was restored
        assertThat(tripRepository.findById(trip.getId()).orElseThrow().getAvailableSeats()).isEqualTo(2);
    }
}
