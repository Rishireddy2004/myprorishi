package com.ridesharing.dto;

import com.ridesharing.domain.TripStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStatusUpdateDTO {

    @NotNull(message = "Status is required")
    private TripStatus status;

    /** Required when transitioning to COMPLETED — driver's current latitude */
    private Double currentLat;

    /** Required when transitioning to COMPLETED — driver's current longitude */
    private Double currentLng;
}
