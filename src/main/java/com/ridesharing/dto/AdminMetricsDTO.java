package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Aggregate platform metrics returned by GET /admin/metrics (Requirement 11.1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMetricsDTO implements Serializable {

    private long totalUsers;
    private long totalTrips;
    private long completedTrips;
    private double totalPaymentVolume;
    private long activeUsers;
    private long totalCancellations;
    private long openDisputes;
}
