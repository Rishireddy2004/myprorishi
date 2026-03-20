package com.ridesharing.controller;

import com.ridesharing.dto.UpdateProfileRequestDTO;
import com.ridesharing.dto.UserProfileDTO;
import com.ridesharing.dto.VehicleDTO;
import com.ridesharing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /users/me — get own full profile (requires auth)
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getMyProfile() {
        String email = getAuthenticatedEmail();
        UserProfileDTO profile = userService.getMyProfile(email);
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /users/{id} — public profile (no auth required)
     * Requirement 2.1, 2.2, 2.3, 2.6, 2.7
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable UUID id) {
        UserProfileDTO profile = userService.getUserProfile(id);
        return ResponseEntity.ok(profile);
    }

    /**
     * PATCH /users/me — update own profile (requires auth)
     * Requirement 2.1
     */
    @PatchMapping("/me")
    public ResponseEntity<UserProfileDTO> updateProfile(@Valid @RequestBody UpdateProfileRequestDTO dto) {
        String email = getAuthenticatedEmail();
        UserProfileDTO updated = userService.updateProfile(email, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * GET /users/me/trust-stats — get driver trust score stats
     */
    @GetMapping("/me/trust-stats")
    public ResponseEntity<Map<String, Object>> getTrustStats() {
        String email = getAuthenticatedEmail();
        return ResponseEntity.ok(userService.getTrustStats(email));
    }

    /**
     * GET /users/me/points-history — get loyalty points history for authenticated user
     */
    @GetMapping("/me/points-history")
    public ResponseEntity<List<Map<String, Object>>> getPointsHistory() {
        String email = getAuthenticatedEmail();
        return ResponseEntity.ok(userService.getPointsHistory(email));
    }

    /**
     * GET /users/me/vehicle — get own vehicle (requires auth)
     */
    @GetMapping("/me/vehicle")
    public ResponseEntity<VehicleDTO> getMyVehicle() {
        String email = getAuthenticatedEmail();
        VehicleDTO vehicle = userService.getVehicle(email);
        return ResponseEntity.ok(vehicle);
    }

    /**
     * PUT /users/me/vehicle — add/update vehicle (requires auth)
     * Requirement 2.4, 2.7
     */
    @PutMapping("/me/vehicle")
    public ResponseEntity<VehicleDTO> updateVehicle(@Valid @RequestBody VehicleDTO dto) {
        String email = getAuthenticatedEmail();
        VehicleDTO updated = userService.updateVehicle(email, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /users/me/notification-preferences — toggle email notifications (requires auth)
     * Requirement 10.5
     */
    @PatchMapping("/me/notification-preferences")
    public ResponseEntity<Void> updateNotificationPreferences(
            @RequestBody Map<String, Boolean> body) {
        String email = getAuthenticatedEmail();
        Boolean emailNotificationsEnabled = body.get("emailNotificationsEnabled");
        if (emailNotificationsEnabled != null) {
            userService.updateNotificationPreferences(email, emailNotificationsEnabled);
        }
        return ResponseEntity.ok().build();
    }

    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
