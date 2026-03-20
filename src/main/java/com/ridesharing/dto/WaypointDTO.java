package com.ridesharing.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaypointDTO {

    private UUID id;

    private int sequenceOrder;

    @NotBlank(message = "Waypoint address is required")
    private String address;

    private double lat;

    private double lng;
}
