package com.ridesharing.controller;

import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.Dispute;
import com.ridesharing.dto.*;
import com.ridesharing.repository.DisputeRepository;
import com.ridesharing.service.AdminService;
import com.ridesharing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only endpoints (Requirements 11.1 – 11.11).
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DisputeRepository disputeRepository;
    private final PaymentService paymentService;
    private final AdminService adminService;

    // -------------------------------------------------------------------------
    // Metrics – Requirement 11.1
    // -------------------------------------------------------------------------

    /**
     * GET /admin/metrics — aggregate platform metrics, cached 5 minutes.
     */
    @GetMapping("/metrics")
    public ResponseEntity<AdminMetricsDTO> getMetrics() {
        return ResponseEntity.ok(adminService.getMetrics());
    }

    // -------------------------------------------------------------------------
    // User search – Requirement 11.2
    // -------------------------------------------------------------------------

    /**
     * GET /admin/users?q=... — search users by email or name.
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDTO>> searchUsers(
            @RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(adminService.searchUsers(query));
    }

    // -------------------------------------------------------------------------
    // Trip search – Requirement 11.3
    // -------------------------------------------------------------------------

    /**
     * GET /admin/trips?q=... — search trips by ID, origin, destination, or driver name.
     */
    @GetMapping("/trips")
    public ResponseEntity<List<AdminTripDTO>> searchTrips(
            @RequestParam(name = "q", defaultValue = "") String query) {
        return ResponseEntity.ok(adminService.searchTrips(query));
    }

    /**
     * POST /admin/users/{id}/reset-password — admin sets a new password for a user.
     */
    @PostMapping("/users/{id}/reset-password")
    public ResponseEntity<Void> resetUserPassword(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        adminService.resetUserPassword(id, body.get("newPassword"));
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Suspension – Requirement 11.4
    // -------------------------------------------------------------------------

    /**
     * POST /admin/users/{id}/suspend — suspend a user account.
     */
    @PostMapping("/users/{id}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID id) {
        adminService.suspendUser(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /admin/users/{id}/unsuspend — reinstate a suspended user account.
     */
    @PostMapping("/users/{id}/unsuspend")
    public ResponseEntity<Void> unsuspendUser(@PathVariable UUID id) {
        adminService.unsuspendUser(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Disputes – Requirements 11.5, 11.6
    // -------------------------------------------------------------------------

    /**
     * GET /admin/disputes — list all disputes.
     */
    @GetMapping("/disputes")
    public ResponseEntity<List<DisputeDTO>> listDisputes() {
        return ResponseEntity.ok(adminService.listDisputes());
    }

    /**
     * POST /admin/disputes/{id}/resolve — resolve a dispute with optional refund.
     */
    @PostMapping("/disputes/{id}/resolve")
    public ResponseEntity<DisputeDTO> resolveDispute(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, Object> body) {
        double refundAmount = 0.0;
        if (body != null && body.containsKey("refundAmount")) {
            refundAmount = ((Number) body.get("refundAmount")).doubleValue();
        }
        return ResponseEntity.ok(adminService.resolveDispute(id, refundAmount));
    }

    /**
     * POST /admin/disputes/{id}/refund — admin-initiated exact refund (Requirement 7.10).
     */
    @PostMapping("/disputes/{id}/refund")
    public ResponseEntity<Void> refundDispute(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequestDTO dto) {

        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DISPUTE_NOT_FOUND",
                        "Dispute not found: " + id));

        if (dispute.getTrip() == null || dispute.getTrip().getBookings() == null
                || dispute.getTrip().getBookings().isEmpty()) {
            throw new UnprocessableEntityException("NO_BOOKING_FOR_DISPUTE",
                    "No booking found for this dispute.");
        }

        var booking = dispute.getTrip().getBookings().stream()
                .filter(b -> b.getPassenger().getId().equals(dispute.getReporter().getId()))
                .findFirst()
                .orElseThrow(() -> new UnprocessableEntityException("NO_BOOKING_FOR_DISPUTE",
                        "No booking by the reporter found on the dispute trip."));

        String paymentIntentId = booking.getPaymentIntentId();
        if (paymentIntentId == null) {
            throw new UnprocessableEntityException("NO_PAYMENT_INTENT",
                    "No payment intent found for the booking associated with this dispute.");
        }

        paymentService.adminRefund(paymentIntentId, dispute.getReporter(),
                booking, dto.getAmount().doubleValue());

        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Platform config – Requirements 11.7, 11.8, 11.9
    // -------------------------------------------------------------------------

    /**
     * GET /admin/config — read all platform configuration entries.
     */
    @GetMapping("/config")
    public ResponseEntity<List<PlatformConfigDTO>> getConfig() {
        return ResponseEntity.ok(adminService.getAllConfig());
    }

    /**
     * PUT /admin/config — upsert a platform configuration entry.
     * Fee changes only apply to future trips (snapshotted at trip creation).
     */
    @PutMapping("/config")
    public ResponseEntity<PlatformConfigDTO> updateConfig(
            @Valid @RequestBody PlatformConfigDTO dto) {
        return ResponseEntity.ok(adminService.upsertConfig(dto.getKey(), dto.getValue()));
    }
}
