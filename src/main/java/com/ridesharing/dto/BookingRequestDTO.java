package com.ridesharing.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class BookingRequestDTO {

    /** Number of seats to book (defaults to 1) */
    private Integer seats;

    /** Optional passenger name (kept for backward compatibility) */
    private String passengerName;

    /** Optional contact number (kept for backward compatibility) */
    private String contactNumber;

    /** Optional: waypoint id for boarding stop */
    private UUID boardingWaypointId;

    /** Optional: waypoint id for alighting stop */
    private UUID alightingWaypointId;

    /** Optional: loyalty points to redeem for a discount (0 = none) */
    private int redeemPoints = 0;

    /** Optional: tip amount for the driver in ₹ (0 = none) */
    private float tipAmount = 0f;
}
