package com.ridesharing.unit;

import com.ridesharing.client.GoogleMapsDistanceClient;
import com.ridesharing.domain.Trip;
import com.ridesharing.dto.FareResponseDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.WaypointRepository;
import com.ridesharing.service.FareCalculatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FareCalculatorService.
 * Validates: Requirements 6.1–6.5
 */
@ExtendWith(MockitoExtension.class)
class FareCalculatorServiceTest {

    @Mock private GoogleMapsDistanceClient distanceClient;
    @Mock private TripRepository tripRepository;
    @Mock private WaypointRepository waypointRepository;
    @Mock private BookingRepository bookingRepository;

    @InjectMocks
    private FareCalculatorService fareCalculatorService;

    @Test
    void estimateFare_knownInputs_returnsExpectedFare() {
        UUID tripId = UUID.randomUUID();
        Trip trip = Trip.builder()
                .id(tripId)
                .baseFarePerKm(5.0)
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(distanceClient.getRoadDistanceKm(0.0, 0.0, 1.0, 1.0)).thenReturn(10.0);

        FareResponseDTO result = fareCalculatorService.estimateFare(tripId, null, null);

        assertThat(result.getFare()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void estimateFare_zeroDistance_returnsFareOfZero() {
        UUID tripId = UUID.randomUUID();
        Trip trip = Trip.builder()
                .id(tripId)
                .baseFarePerKm(5.0)
                .originLat(0.0).originLng(0.0)
                .destinationLat(0.0).destinationLng(0.0)
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(distanceClient.getRoadDistanceKm(0.0, 0.0, 0.0, 0.0)).thenReturn(0.0);

        FareResponseDTO result = fareCalculatorService.estimateFare(tripId, null, null);

        assertThat(result.getFare()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void estimateFare_fractionalDistance_roundsToTwoDecimalPlaces() {
        UUID tripId = UUID.randomUUID();
        Trip trip = Trip.builder()
                .id(tripId)
                .baseFarePerKm(5.0)
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        // 5.0 × 3.333 = 16.665 → rounds to 16.67
        when(distanceClient.getRoadDistanceKm(0.0, 0.0, 1.0, 1.0)).thenReturn(3.333);

        FareResponseDTO result = fareCalculatorService.estimateFare(tripId, null, null);

        assertThat(result.getFare()).isEqualByComparingTo(new BigDecimal("16.67"));
    }

    @Test
    void estimateFare_returnsDistanceKmInResponse() {
        UUID tripId = UUID.randomUUID();
        Trip trip = Trip.builder()
                .id(tripId)
                .baseFarePerKm(5.0)
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .build();
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(distanceClient.getRoadDistanceKm(0.0, 0.0, 1.0, 1.0)).thenReturn(10.0);

        FareResponseDTO result = fareCalculatorService.estimateFare(tripId, null, null);

        assertThat(result.getDistanceKm()).isEqualTo(10.0);
    }
}
