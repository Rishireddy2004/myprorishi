package com.ridesharing.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for fare estimate and locked fare (Requirement 6.4, 6.5).
 */
public class FareResponseDTO {

    private UUID tripId;
    private UUID boardingWaypointId;
    private UUID alightingWaypointId;
    private double distanceKm;
    private double baseFarePerKm;
    private BigDecimal fare;
    /** True when the fare is locked from a confirmed booking; false for an estimate. */
    private boolean locked;

    public FareResponseDTO() {}

    private FareResponseDTO(Builder builder) {
        this.tripId = builder.tripId;
        this.boardingWaypointId = builder.boardingWaypointId;
        this.alightingWaypointId = builder.alightingWaypointId;
        this.distanceKm = builder.distanceKm;
        this.baseFarePerKm = builder.baseFarePerKm;
        this.fare = builder.fare;
        this.locked = builder.locked;
    }

    public static Builder builder() { return new Builder(); }

    public UUID getTripId() { return tripId; }
    public UUID getBoardingWaypointId() { return boardingWaypointId; }
    public UUID getAlightingWaypointId() { return alightingWaypointId; }
    public double getDistanceKm() { return distanceKm; }
    public double getBaseFarePerKm() { return baseFarePerKm; }
    public BigDecimal getFare() { return fare; }
    public boolean isLocked() { return locked; }

    public static class Builder {
        private UUID tripId;
        private UUID boardingWaypointId;
        private UUID alightingWaypointId;
        private double distanceKm;
        private double baseFarePerKm;
        private BigDecimal fare;
        private boolean locked;

        public Builder tripId(UUID tripId) { this.tripId = tripId; return this; }
        public Builder boardingWaypointId(UUID id) { this.boardingWaypointId = id; return this; }
        public Builder alightingWaypointId(UUID id) { this.alightingWaypointId = id; return this; }
        public Builder distanceKm(double distanceKm) { this.distanceKm = distanceKm; return this; }
        public Builder baseFarePerKm(double baseFarePerKm) { this.baseFarePerKm = baseFarePerKm; return this; }
        public Builder fare(BigDecimal fare) { this.fare = fare; return this; }
        public Builder locked(boolean locked) { this.locked = locked; return this; }
        public FareResponseDTO build() { return new FareResponseDTO(this); }
    }
}
