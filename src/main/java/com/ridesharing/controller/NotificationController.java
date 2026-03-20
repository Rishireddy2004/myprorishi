package com.ridesharing.controller;

import com.ridesharing.domain.Notification;
import com.ridesharing.dto.NotificationPreferencesDTO;
import com.ridesharing.repository.NotificationRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Exposes notification-related endpoints.
 * Requirements 10.1 – 10.6
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public NotificationController(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * GET /notifications – returns the authenticated user's notifications, newest first.
     * Requirement 10.1
     */
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        UUID userId = getAuthenticatedUserId();
        List<Notification> notifications =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * PATCH /notifications/{id}/read – marks a notification as read.
     * Requirement 10.1
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /users/me/notification-preferences – toggle email notifications.
     * Requirement 10.5
     * Note: also handled in UserController; this endpoint is an alias for convenience.
     */
    @PatchMapping("/users/me/notification-preferences")
    public ResponseEntity<Void> updateNotificationPreferences(
            @Valid @RequestBody NotificationPreferencesDTO dto) {
        String email = getAuthenticatedEmail();
        userService.updateNotificationPreferences(email, dto.isEmailNotificationsEnabled());
        return ResponseEntity.ok().build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String getAuthenticatedEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private UUID getAuthenticatedUserId() {
        String email = getAuthenticatedEmail();
        return userRepository.findByEmail(email)
                .map(u -> u.getId())
                .orElseThrow(() -> new com.ridesharing.common.exception.ResourceNotFoundException(
                        "USER_NOT_FOUND", "User not found"));
    }
}
