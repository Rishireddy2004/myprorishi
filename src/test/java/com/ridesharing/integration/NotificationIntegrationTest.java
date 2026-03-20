package com.ridesharing.integration;

import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.BookingResponseDTO;
import com.ridesharing.dto.TripRequestDTO;
import com.ridesharing.dto.TripResponseDTO;
import com.ridesharing.repository.NotificationRepository;
import com.ridesharing.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: notification delivery pipeline.
 * Validates Requirements 12.2, 12.4
 */
class NotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void bookingConfirmed_notificationRecordCreatedInDB() {
        // Register driver and passenger
        String driverToken = registerAndLogin("driver.notif@test.com", "password123", "Driver Notif", "+7777777771");
        String passengerToken = registerAndLogin("passenger.notif@test.com", "password123", "Passenger Notif", "+7777777772");
        addVehicleForUser("driver.notif@test.com");

        // Driver posts a trip
        TripRequestDTO tripRequest = TripRequestDTO.builder()
                .originAddress("Bangalore")
                .destinationAddress("Mysore")
                .departureTime(LocalDateTime.now().plusDays(1))
                .totalSeats(2)
                .baseFarePerKm(5.0)
                .build();

        ResponseEntity<TripResponseDTO> tripResp = post("/trips", tripRequest, driverToken, TripResponseDTO.class);
        TripResponseDTO trip = tripResp.getBody();

        // Passenger books a seat
        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setPassengerName("Passenger Notif");
        bookingRequest.setContactNumber("+7777777772");

        ResponseEntity<BookingResponseDTO> bookingResp = post(
                "/trips/" + trip.getId() + "/bookings",
                bookingRequest,
                passengerToken,
                BookingResponseDTO.class
        );
        assertThat(bookingResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Verify a BOOKING_CONFIRMED notification was saved for the passenger
        var passenger = userRepository.findByEmail("passenger.notif@test.com").orElseThrow();
        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(passenger.getId());

        assertThat(notifications).isNotEmpty();
        assertThat(notifications)
                .anyMatch(n -> "BOOKING_CONFIRMED".equals(n.getType()));
    }
}
