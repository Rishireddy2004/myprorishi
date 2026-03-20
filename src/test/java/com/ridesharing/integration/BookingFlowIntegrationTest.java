package com.ridesharing.integration;

import com.ridesharing.domain.BookingStatus;
import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.BookingResponseDTO;
import com.ridesharing.dto.TripRequestDTO;
import com.ridesharing.dto.TripResponseDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: full booking flow.
 * Validates Requirements 12.2, 12.4
 */
class BookingFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void fullBookingFlow_registerUserPostTripBookSeat_verifyConfirmedAndSeatsDecremented() {
        // 1. Register driver and passenger
        String driverToken = registerAndLogin("driver@test.com", "password123", "Driver User", "+1111111111");
        String passengerToken = registerAndLogin("passenger@test.com", "password123", "Passenger User", "+2222222222");

        // 2. Driver needs a vehicle to post a trip
        addVehicleForUser("driver@test.com");

        // 3. Driver posts a trip with 3 seats
        TripRequestDTO tripRequest = TripRequestDTO.builder()
                .originAddress("Bangalore")
                .destinationAddress("Mysore")
                .departureTime(LocalDateTime.now().plusDays(1))
                .totalSeats(3)
                .baseFarePerKm(5.0)
                .build();

        ResponseEntity<TripResponseDTO> tripResp = post("/trips", tripRequest, driverToken, TripResponseDTO.class);
        assertThat(tripResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TripResponseDTO trip = tripResp.getBody();
        assertThat(trip).isNotNull();
        assertThat(trip.getAvailableSeats()).isEqualTo(3);

        // 4. Passenger books a seat
        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setPassengerName("Passenger User");
        bookingRequest.setContactNumber("+2222222222");

        ResponseEntity<BookingResponseDTO> bookingResp = post(
                "/trips/" + trip.getId() + "/bookings",
                bookingRequest,
                passengerToken,
                BookingResponseDTO.class
        );

        assertThat(bookingResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        BookingResponseDTO booking = bookingResp.getBody();
        assertThat(booking).isNotNull();

        // 5. Verify booking status is CONFIRMED
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // 6. Verify available seats decremented in DB
        var updatedTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(updatedTrip.getAvailableSeats()).isEqualTo(2);
    }
}
