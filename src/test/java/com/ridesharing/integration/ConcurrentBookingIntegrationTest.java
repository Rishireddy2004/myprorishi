package com.ridesharing.integration;

import com.ridesharing.dto.BookingRequestDTO;
import com.ridesharing.dto.TripRequestDTO;
import com.ridesharing.dto.TripResponseDTO;
import com.ridesharing.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: concurrent booking overbooking prevention.
 * Validates Requirements 12.2, 12.4
 */
class ConcurrentBookingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TripRepository tripRepository;

    @Test
    void concurrentBooking_onlyOneSeatAvailable_exactlyOneSucceedsAndOneGets409() throws Exception {
        // Register driver and two passengers
        String driverToken = registerAndLogin("driver.conc@test.com", "password123", "Driver Conc", "+1010101011");
        String passenger1Token = registerAndLogin("passenger.conc1@test.com", "password123", "Passenger Conc1", "+1010101012");
        String passenger2Token = registerAndLogin("passenger.conc2@test.com", "password123", "Passenger Conc2", "+1010101013");
        addVehicleForUser("driver.conc@test.com");

        // Driver posts a trip with exactly 1 seat
        TripRequestDTO tripRequest = TripRequestDTO.builder()
                .originAddress("Bangalore")
                .destinationAddress("Mysore")
                .departureTime(LocalDateTime.now().plusDays(1))
                .totalSeats(1)
                .baseFarePerKm(5.0)
                .build();

        ResponseEntity<TripResponseDTO> tripResp = post("/trips", tripRequest, driverToken, TripResponseDTO.class);
        assertThat(tripResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TripResponseDTO trip = tripResp.getBody();
        assertThat(trip.getAvailableSeats()).isEqualTo(1);

        // Prepare two concurrent booking requests
        BookingRequestDTO req1 = new BookingRequestDTO();
        req1.setPassengerName("Passenger Conc1");
        req1.setContactNumber("+1010101012");

        BookingRequestDTO req2 = new BookingRequestDTO();
        req2.setPassengerName("Passenger Conc2");
        req2.setContactNumber("+1010101013");

        String tripId = trip.getId().toString();

        Callable<ResponseEntity<Map>> task1 = () ->
                post("/trips/" + tripId + "/bookings", req1, passenger1Token, Map.class);
        Callable<ResponseEntity<Map>> task2 = () ->
                post("/trips/" + tripId + "/bookings", req2, passenger2Token, Map.class);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<Future<ResponseEntity<Map>>> futures = executor.invokeAll(List.of(task1, task2));
        executor.shutdown();

        List<Integer> statusCodes = new ArrayList<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            statusCodes.add(future.get().getStatusCode().value());
        }

        // Exactly one should succeed (201) and one should fail (409)
        long successCount = statusCodes.stream().filter(s -> s == 201).count();
        long conflictCount = statusCodes.stream().filter(s -> s == 409).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(conflictCount).isEqualTo(1);

        // Verify available seats is 0 in DB
        var updatedTrip = tripRepository.findById(trip.getId()).orElseThrow();
        assertThat(updatedTrip.getAvailableSeats()).isEqualTo(0);
    }
}
