package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Admin view of a user including profile, trip history, and reviews (Requirement 11.2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private boolean isSuspended;
    private boolean isVerified;
    private Double aggregateRating;
    private int reviewCount;
    private LocalDateTime createdAt;

    private List<TripResponseDTO> tripHistory;
    private List<ReviewResponseDTO> reviews;
}
