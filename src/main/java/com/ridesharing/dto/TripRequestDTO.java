package com.ridesharing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRequestDTO {

    @NotBlank(message = "Origin address is required")
    private String originAddress;

    @NotBlank(message = "Destination address is required")
    private String destinationAddress;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    private Integer totalSeats;

    @NotNull(message = "Base fare per km is required")
    @Min(value = 0, message = "Base fare per km cannot be negative")
    private Double baseFarePerKm;

    /** Optional intermediate waypoints (between origin and destination) */
    @Valid
    private List<WaypointDTO> intermediateWaypoints;
}
