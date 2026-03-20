package com.ridesharing.controller;

import com.ridesharing.dto.LocationUpdateDTO;
import com.ridesharing.service.TrackingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class LocationController {

    private final TrackingService trackingService;

    public LocationController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    /**
     * POST /trips/{id}/location — driver sends live location update.
     */
    @PostMapping("/{id}/location")
    public ResponseEntity<Void> updateLocation(
            @PathVariable UUID id,
            @Valid @RequestBody LocationUpdateDTO dto) {
        trackingService.updateLocation(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /trips/{id}/location — passenger polls last known driver location.
     */
    @GetMapping("/{id}/location")
    public ResponseEntity<?> getLocation(@PathVariable UUID id) {
        LocationUpdateDTO loc = trackingService.getLastLocation(id);
        if (loc == null) {
            return ResponseEntity.ok(Map.of("active", false));
        }
        return ResponseEntity.ok(Map.of(
                "active", true,
                "latitude", loc.getLat(),
                "longitude", loc.getLng()
        ));
    }
}
