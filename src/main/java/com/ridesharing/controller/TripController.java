package com.ridesharing.controller;

import com.ridesharing.dto.*;
import com.ridesharing.service.TripService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /** POST /trips — create a new trip */
    @PostMapping
    public ResponseEntity<TripResponseDTO> createTrip(
            @Valid @RequestBody TripRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponseDTO response = tripService.createTrip(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** GET /trips/open — get all open trips (public, no auth required) */
    @GetMapping("/open")
    public ResponseEntity<Map<String, List<TripResponseDTO>>> getOpenTrips() {
        List<TripResponseDTO> trips = tripService.getOpenTrips();
        return ResponseEntity.ok(Map.of("trips", trips));
    }

    /** GET /trips/my — get all trips posted by the authenticated driver */
    @GetMapping("/my")
    public ResponseEntity<Map<String, List<TripResponseDTO>>> getMyTrips(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<TripResponseDTO> trips = tripService.getMyTrips(userDetails.getUsername());
        return ResponseEntity.ok(java.util.Map.of("trips", trips));
    }

    /** GET /trips/:id — get trip detail with waypoints */
    @GetMapping("/{id}")
    public ResponseEntity<TripResponseDTO> getTrip(@PathVariable UUID id) {
        return ResponseEntity.ok(tripService.getTrip(id));
    }

    /** PATCH /trips/:id — update availableSeats and/or baseFarePerKm */
    @PatchMapping("/{id}")
    public ResponseEntity<TripResponseDTO> updateTrip(
            @PathVariable UUID id,
            @Valid @RequestBody TripUpdateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponseDTO response = tripService.updateTrip(id, dto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /** DELETE /trips/:id — cancel open trip (> 2 hours before departure) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        tripService.cancelTrip(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /** POST /trips/:id/reopen — undo cancellation, restore trip to OPEN */
    @PostMapping("/{id}/reopen")
    public ResponseEntity<TripResponseDTO> reopenTrip(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponseDTO response = tripService.reopenTrip(id, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    /** PATCH /trips/:id/status — transition trip status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TripResponseDTO> updateTripStatus(
            @PathVariable UUID id,
            @Valid @RequestBody TripStatusUpdateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        TripResponseDTO response = tripService.updateTripStatus(id, dto, userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
