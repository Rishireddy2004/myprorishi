package com.ridesharing.service;

import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.*;
import com.ridesharing.dto.*;
import com.ridesharing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin operations: metrics, user/trip search, suspension, disputes, platform config.
 * Requirements 11.1 – 11.11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final TransactionRepository transactionRepository;
    private final DisputeRepository disputeRepository;
    private final PlatformConfigRepository platformConfigRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // Metrics – Requirement 11.1
    // -------------------------------------------------------------------------

    /**
     * Returns aggregate platform metrics. Result is cached in Redis for 5 minutes.
     */
    @Cacheable("adminMetrics")
    public AdminMetricsDTO getMetrics() {
        long totalUsers = userRepository.count();
        long totalTrips = tripRepository.count();
        long completedTrips = tripRepository.countByStatus(TripStatus.COMPLETED);
        long totalCancellations = tripRepository.countByStatus(TripStatus.CANCELLED);

        double totalPaymentVolume = transactionRepository.sumAmountByType("CAPTURE");

        long activeUsers = userRepository.countByIsSuspendedFalse();

        long openDisputes = disputeRepository.countByStatus("OPEN");

        return AdminMetricsDTO.builder()
                .totalUsers(totalUsers)
                .totalTrips(totalTrips)
                .completedTrips(completedTrips)
                .totalPaymentVolume(totalPaymentVolume)
                .activeUsers(activeUsers)
                .totalCancellations(totalCancellations)
                .openDisputes(openDisputes)
                .build();
    }

    // -------------------------------------------------------------------------
    // User search – Requirement 11.2
    // -------------------------------------------------------------------------

    public List<AdminUserDTO> searchUsers(String query) {
        List<User> users = userRepository
                .findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(query, query);
        return users.stream().map(this::toAdminUserDTO).collect(Collectors.toList());
    }

    private AdminUserDTO toAdminUserDTO(User user) {
        List<Trip> trips = tripRepository.findByDriverId(user.getId());
        List<TripResponseDTO> tripHistory = trips.stream()
                .map(this::toTripResponseDTO)
                .collect(Collectors.toList());

        List<ReviewResponseDTO> reviews = reviewRepository
                .findByRevieweeIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toReviewResponseDTO)
                .collect(Collectors.toList());

        return AdminUserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isSuspended(user.isSuspended())
                .isVerified(user.isVerified())
                .aggregateRating(user.getAggregateRating())
                .reviewCount(user.getReviewCount())
                .createdAt(user.getCreatedAt())
                .tripHistory(tripHistory)
                .reviews(reviews)
                .build();
    }

    // -------------------------------------------------------------------------
    // Trip search – Requirement 11.3
    // -------------------------------------------------------------------------

    public List<AdminTripDTO> searchTrips(String query) {
        List<Trip> trips = tripRepository.searchByAdmin(query);
        return trips.stream().map(this::toAdminTripDTO).collect(Collectors.toList());
    }

    private AdminTripDTO toAdminTripDTO(Trip trip) {
        return AdminTripDTO.builder()
                .id(trip.getId())
                .driverName(trip.getDriver().getFullName())
                .driverEmail(trip.getDriver().getEmail())
                .originAddress(trip.getOriginAddress())
                .destinationAddress(trip.getDestinationAddress())
                .departureTime(trip.getDepartureTime())
                .status(trip.getStatus().name())
                .totalSeats(trip.getTotalSeats())
                .availableSeats(trip.getAvailableSeats())
                .baseFarePerKm(trip.getBaseFarePerKm())
                .serviceFeeRate(trip.getServiceFeeRate())
                .createdAt(trip.getCreatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Password reset by admin – no email/SMS needed
    // -------------------------------------------------------------------------

    @Transactional
    public void resetUserPassword(UUID userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new UnprocessableEntityException("WEAK_PASSWORD",
                    "Password must be at least 8 characters.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found: " + userId));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Admin reset password for user {}", userId);
    }

    // -------------------------------------------------------------------------
    // Suspension – Requirement 11.4
    // -------------------------------------------------------------------------

    @Transactional
    @CacheEvict(value = "adminMetrics", allEntries = true)
    public void suspendUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found: " + userId));

        if (user.isSuspended()) {
            throw new UnprocessableEntityException("USER_ALREADY_SUSPENDED",
                    "User is already suspended.");
        }

        user.setSuspended(true);
        userRepository.save(user);

        notificationService.sendSuspensionNotification(user, true);
        log.info("Admin suspended user {}", userId);
    }

    @Transactional
    @CacheEvict(value = "adminMetrics", allEntries = true)
    public void unsuspendUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found: " + userId));

        if (!user.isSuspended()) {
            throw new UnprocessableEntityException("USER_NOT_SUSPENDED",
                    "User is not suspended.");
        }

        user.setSuspended(false);
        userRepository.save(user);

        notificationService.sendSuspensionNotification(user, false);
        log.info("Admin unsuspended user {}", userId);
    }

    // -------------------------------------------------------------------------
    // Disputes – Requirements 11.5, 11.6
    // -------------------------------------------------------------------------

    public List<DisputeDTO> listDisputes() {
        return disputeRepository.findAll().stream()
                .map(this::toDisputeDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DisputeDTO resolveDispute(UUID disputeId, double refundAmount) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("DISPUTE_NOT_FOUND",
                        "Dispute not found: " + disputeId));

        if (!"OPEN".equals(dispute.getStatus())) {
            throw new UnprocessableEntityException("DISPUTE_ALREADY_RESOLVED",
                    "Dispute is already resolved.");
        }

        // Issue refund if amount > 0
        if (refundAmount > 0 && dispute.getTrip() != null) {
            var bookings = dispute.getTrip().getBookings();
            if (bookings != null && !bookings.isEmpty()) {
                var booking = bookings.stream()
                        .filter(b -> b.getPassenger().getId().equals(dispute.getReporter().getId()))
                        .findFirst()
                        .orElse(null);

                if (booking != null && booking.getPaymentIntentId() != null) {
                    // PaymentService.adminRefund is called from AdminController for the refund endpoint
                    // Here we just record the resolution
                }
            }
        }

        dispute.setStatus("RESOLVED");
        dispute.setRefundAmount((float) refundAmount);
        dispute.setResolvedAt(LocalDateTime.now());
        disputeRepository.save(dispute);

        log.info("Admin resolved dispute {} with refund {}", disputeId, refundAmount);
        return toDisputeDTO(dispute);
    }

    private DisputeDTO toDisputeDTO(Dispute dispute) {
        return DisputeDTO.builder()
                .id(dispute.getId())
                .reporterId(dispute.getReporter().getId())
                .reporterName(dispute.getReporter().getFullName())
                .reportedId(dispute.getReported().getId())
                .reportedName(dispute.getReported().getFullName())
                .tripId(dispute.getTrip() != null ? dispute.getTrip().getId() : null)
                .description(dispute.getDescription())
                .status(dispute.getStatus())
                .refundAmount(dispute.getRefundAmount())
                .createdAt(dispute.getCreatedAt())
                .resolvedAt(dispute.getResolvedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Platform config – Requirements 11.7, 11.8, 11.9
    // -------------------------------------------------------------------------

    public List<PlatformConfigDTO> getAllConfig() {
        return platformConfigRepository.findAll().stream()
                .map(this::toConfigDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "adminMetrics", allEntries = true)
    public PlatformConfigDTO upsertConfig(String key, String value) {
        PlatformConfig config = platformConfigRepository.findById(key)
                .orElse(PlatformConfig.builder().key(key).build());

        config.setValue(value);
        config.setUpdatedAt(LocalDateTime.now());
        platformConfigRepository.save(config);

        log.info("Admin updated platform config: {}={}", key, value);
        return toConfigDTO(config);
    }

    private PlatformConfigDTO toConfigDTO(PlatformConfig config) {
        return PlatformConfigDTO.builder()
                .key(config.getKey())
                .value(config.getValue())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private TripResponseDTO toTripResponseDTO(Trip trip) {
        return TripResponseDTO.builder()
                .id(trip.getId())
                .driverId(trip.getDriver().getId())
                .driverName(trip.getDriver().getFullName())
                .originAddress(trip.getOriginAddress())
                .destinationAddress(trip.getDestinationAddress())
                .departureTime(trip.getDepartureTime())
                .totalSeats(trip.getTotalSeats())
                .availableSeats(trip.getAvailableSeats())
                .baseFarePerKm(trip.getBaseFarePerKm())
                .status(trip.getStatus())
                .serviceFeeRate(trip.getServiceFeeRate())
                .createdAt(trip.getCreatedAt())
                .build();
    }

    private ReviewResponseDTO toReviewResponseDTO(Review review) {
        return ReviewResponseDTO.builder()
                .id(review.getId())
                .tripId(review.getTrip().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .revieweeId(review.getReviewee().getId())
                .revieweeName(review.getReviewee().getFullName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
