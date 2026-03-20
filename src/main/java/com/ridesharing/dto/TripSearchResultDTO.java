package com.ridesharing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single trip result returned by the search endpoint (Requirements 4.4, 4.5).
 */
@Data
@Builder
public class TripSearchResultDTO {

    private UUID tripId;

    // Driver info
    private String driverName;
    private Double driverRating;

    // Trip info
    private LocalDateTime departureTime;
    private int availableSeats;
    private double baseFarePerKm;

    /** Estimated fare = baseFarePerKm × haversine(passengerOrigin, passengerDestination) */
    private double estimatedFare;

    /** Detour distance in km (lower = better match) */
    private double detourDistanceKm;

    // Vehicle details
    private String vehicleMake;
    private String vehicleModel;
    private int vehicleYear;
    private String vehicleColor;
    private int vehiclePassengerCapacity;

    // Trip route info
    private String originAddress;
    private String destinationAddress;
}
