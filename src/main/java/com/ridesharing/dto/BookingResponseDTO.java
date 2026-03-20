package com.ridesharing.dto;

import com.ridesharing.domain.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingResponseDTO {

    private UUID id;
    private UUID tripId;
    private String tripOrigin;
    private String tripDestination;
    private LocalDateTime departureTime;
    private UUID passengerId;
    private String passengerName;
    private String passengerPhone;

    // Driver contact info (visible to passenger after booking)
    private String driverName;
    private String driverPhone;
    private String driverEmail;

    private UUID boardingWaypointId;
    private UUID alightingWaypointId;
    private float fareLocked;
    private float distanceKm;
    private int seats;
    private float totalFare;
    private BookingStatus status;
    private LocalDateTime createdAt;

    // Loyalty points info
    private int pointsRedeemed;
    private float discountApplied;
    private int remainingPoints;

    // Tip
    private float tipAmount;
}
