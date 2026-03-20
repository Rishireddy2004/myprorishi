package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin view of a trip (Requirement 11.3).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTripDTO {

    private UUID id;
    private String driverName;
    private String driverEmail;
    private String originAddress;
    private String destinationAddress;
    private LocalDateTime departureTime;
    private String status;
    private int totalSeats;
    private int availableSeats;
    private double baseFarePerKm;
    private double serviceFeeRate;
    private LocalDateTime createdAt;
}
