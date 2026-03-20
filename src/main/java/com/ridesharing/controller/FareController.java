package com.ridesharing.controller;

import com.ridesharing.dto.FareResponseDTO;
import com.ridesharing.service.FareCalculatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Exposes the fare estimate endpoint (Requirement 6.4).
 *
 * GET /trips/{id}/fare?boardingWaypointId=...&alightingWaypointId=...
 *
 * For confirmed bookings with a locked fare, returns fare_locked.
 * Otherwise returns a live estimate using the current base_fare_per_km.
 */
@RestController
@RequestMapping("/trips")
public class FareController {

    private final FareCalculatorService fareCalculatorService;

    public FareController(FareCalculatorService fareCalculatorService) {
        this.fareCalculatorService = fareCalculatorService;
    }

    /**
     * GET /trips/{id}/fare
     *
     * @param id                  trip ID
     * @param boardingWaypointId  optional boarding waypoint (defaults to trip origin)
     * @param alightingWaypointId optional alighting waypoint (defaults to trip destination)
     * @param bookingId           optional booking ID; if provided and confirmed, returns locked fare
     */
    @GetMapping("/{id}/fare")
    public ResponseEntity<FareResponseDTO> getFare(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID boardingWaypointId,
            @RequestParam(required = false) UUID alightingWaypointId,
            @RequestParam(required = false) UUID bookingId) {

        FareResponseDTO response = fareCalculatorService.getFare(
                id, boardingWaypointId, alightingWaypointId, bookingId);
        return ResponseEntity.ok(response);
    }
}
