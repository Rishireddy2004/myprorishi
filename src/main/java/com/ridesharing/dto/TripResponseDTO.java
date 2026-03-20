package com.ridesharing.dto;

import com.ridesharing.domain.TripStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponseDTO {

    private UUID id;
    private UUID driverId;
    private String driverName;

    private String originAddress;
    private double originLat;
    private double originLng;

    private String destinationAddress;
    private double destinationLat;
    private double destinationLng;

    private LocalDateTime departureTime;
    private int totalSeats;
    private int availableSeats;
    private double baseFarePerKm;
    private double pricePerSeat; // alias for baseFarePerKm, used by frontend
    private TripStatus status;
    private double serviceFeeRate;

    private List<WaypointDTO> waypoints;

    private LocalDateTime createdAt;

    private int driverTrustScore;
    private Double driverRating;
    private long driverCompletedTrips;
}
