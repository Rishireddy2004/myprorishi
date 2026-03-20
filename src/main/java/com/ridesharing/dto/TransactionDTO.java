package com.ridesharing.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionDTO {

    private UUID id;
    private UUID userId;
    private UUID bookingId;
    private String type;
    private float amount;
    private String currency;
    private String status;
    private String stripeReference;
    private LocalDateTime createdAt;
}
