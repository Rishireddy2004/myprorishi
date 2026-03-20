package com.ridesharing.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripUpdateDTO {

    @Min(value = 0, message = "Available seats cannot be negative")
    private Integer availableSeats;

    @Min(value = 0, message = "Base fare per km cannot be negative")
    private Double baseFarePerKm;
}
