package com.ridesharing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponseDTO {

    private UUID id;
    private UUID tripId;
    private UUID reviewerId;
    private String reviewerName;
    private UUID revieweeId;
    private String revieweeName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
