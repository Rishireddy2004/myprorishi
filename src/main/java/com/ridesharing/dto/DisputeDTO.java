package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for dispute data returned to admin (Requirements 11.5, 11.6).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeDTO {

    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private UUID reportedId;
    private String reportedName;
    private UUID tripId;
    private String description;
    private String status;
    private float refundAmount;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
