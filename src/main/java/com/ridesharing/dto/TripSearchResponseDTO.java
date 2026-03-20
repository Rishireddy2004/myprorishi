package com.ridesharing.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Wraps the list of trip search results with an optional message (Requirement 4.7).
 */
@Data
@Builder
public class TripSearchResponseDTO {

    private List<TripSearchResultDTO> results;

    /** Present when the results list is empty. */
    private String message;
}
