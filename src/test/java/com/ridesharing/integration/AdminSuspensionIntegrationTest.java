package com.ridesharing.integration;

import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.TripRequestDTO;
import com.ridesharing.dto.TripResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: admin suspension blocks booking creation.
 * Validates Requirements 12.2, 12.4
 */
class AdminSuspensionIntegrationTest extends BaseIntegrationTest {

    @Test
    void suspendedUser_cannotCreateBooking_returns422() {
        // Register driver and a passenger who will be suspended
        String driverToken = registerAndLogin("driver.susp@test.com", "password123", "Driver Susp", "+8888888881");
        String passengerToken = registerAndLogin("passenger.susp@test.com", "password123", "Passenger Susp", "+8888888882");
        addVehicleForUser("driver.susp@test.com");

        // Driver posts a trip
        TripRequestDTO tripRequest = TripRequestDTO.builder()
                .originAddress("Bangalore")
                .destinationAddress("Mysore")
                .departureTime(LocalDateTime.now().plusDays(1))
                .totalSeats(2)
                .baseFarePerKm(5.0)
                .build();

        ResponseEntity<TripResponseDTO> tripResp = post("/trips", tripRequest, driverToken, TripResponseDTO.class);
        assertThat(tripResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TripResponseDTO trip = tripResp.getBody();

        // Suspend the passenger directly via DB
        suspendUser("passenger.susp@test.com");

        // Suspended passenger tries to book
        BookingRequestDTO bookingRequest = new BookingRequestDTO();
        bookingRequest.setPassengerName("Passenger Susp");
        bookingRequest.setContactNumber("+8888888882");

        ResponseEntity<Map> bookingResp = post(
                "/trips/" + trip.getId() + "/bookings",
                bookingRequest,
                passengerToken,
                Map.class
        );

        // Expect 422 Unprocessable Entity
        assertThat(bookingResp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void suspendedUser_cannotPostTrip_returns422() {
        // Register a driver who will be suspended
        String driverToken = registerAndLogin("driver.susp2@test.com", "password123", "Driver Susp2", "+9999999991");
        addVehicleForUser("driver.susp2@test.com");

        // Suspend the driver
        suspendUser("driver.susp2@test.com");

        // Suspended driver tries to post a trip
        TripRequestDTO tripRequest = TripRequestDTO.builder()
                .originAddress("Bangalore")
                .destinationAddress("Mysore")
                .departureTime(LocalDateTime.now().plusDays(1))
                .totalSeats(2)
                .baseFarePerKm(5.0)
                .build();

        ResponseEntity<Map> tripResp = post("/trips", tripRequest, driverToken, Map.class);

        // Expect 422 Unprocessable Entity
        assertThat(tripResp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
