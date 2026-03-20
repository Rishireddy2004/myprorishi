package com.ridesharing.dto;

import java.util.UUID;

public class UserProfileDTO {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private String photoUrl;
    private Double aggregateRating;
    private int reviewCount;
    private VehicleDTO vehicle;
    private boolean isVerified;

    public UserProfileDTO() {}

    public UserProfileDTO(UUID id, String fullName, String photoUrl, Double aggregateRating,
                          int reviewCount, VehicleDTO vehicle, boolean isVerified) {
        this.id = id;
        this.fullName = fullName;
        this.photoUrl = photoUrl;
        this.aggregateRating = aggregateRating;
        this.reviewCount = reviewCount;
        this.vehicle = vehicle;
        this.isVerified = isVerified;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Double getAggregateRating() { return aggregateRating; }
    public void setAggregateRating(Double aggregateRating) { this.aggregateRating = aggregateRating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public VehicleDTO getVehicle() { return vehicle; }
    public void setVehicle(VehicleDTO vehicle) { this.vehicle = vehicle; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { this.isVerified = verified; }

    private int loyaltyPoints;
    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }

    private int trustScore;
    public int getTrustScore() { return trustScore; }
    public void setTrustScore(int trustScore) { this.trustScore = trustScore; }
}
