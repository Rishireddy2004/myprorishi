package com.ridesharing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationUpdateDTO {

    @NotNull
    private Double lat;

    @NotNull
    private Double lng;
}
