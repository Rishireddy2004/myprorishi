package com.ridesharing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for reading/writing platform configuration (Requirements 11.7, 11.8, 11.9).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformConfigDTO {

    private String key;
    private String value;
    private LocalDateTime updatedAt;
}
