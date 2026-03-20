package com.ridesharing.service;

import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.domain.Booking;
import com.ridesharing.domain.BookingStatus;
import com.ridesharing.domain.User;
import com.ridesharing.domain.Vehicle;
import com.ridesharing.dto.UpdateProfileRequestDTO;
import com.ridesharing.dto.UserProfileDTO;
import com.ridesharing.dto.VehicleDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.ReviewRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public UserService(UserRepository userRepository,
                       VehicleRepository vehicleRepository,
                       ReviewRepository reviewRepository,
                       BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * GET /users/me — returns own profile including email and phone.
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with email: " + email));
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByUserId(user.getId());
        VehicleDTO vehicleDTO = vehicleOpt.map(this::toVehicleDTO).orElse(null);
        UserProfileDTO dto = toUserProfileDTO(user, vehicleDTO);
        // Attach email + phone + loyalty points + trust score for own-profile view
        dto.setEmail(email);
        dto.setPhone(user.getPhone());
        dto.setLoyaltyPoints(user.getLoyaltyPoints());
        dto.setTrustScore(user.getTrustScore());
        return dto;
    }

    /**
     * Returns the public profile for a given user ID.
     * Requirement 2.1, 2.2, 2.3, 2.6, 2.7
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with id: " + userId));

        Optional<Vehicle> vehicleOpt = vehicleRepository.findByUserId(userId);
        VehicleDTO vehicleDTO = vehicleOpt.map(this::toVehicleDTO).orElse(null);

        return toUserProfileDTO(user, vehicleDTO);
    }

    /**
     * Updates the authenticated user's display name, phone, and/or photo URL.
     * Requirement 2.1
     */
    @Transactional
    public UserProfileDTO updateProfile(String email, UpdateProfileRequestDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with email: " + email));

        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getPhotoUrl() != null) {
            user.setPhotoUrl(dto.getPhotoUrl());
        }

        userRepository.save(user);

        Optional<Vehicle> vehicleOpt = vehicleRepository.findByUserId(user.getId());
        VehicleDTO vehicleDTO = vehicleOpt.map(this::toVehicleDTO).orElse(null);

        return toUserProfileDTO(user, vehicleDTO);
    }

    /**
     * Adds or updates the vehicle linked to the authenticated user.
     * Requirement 2.4, 2.7
     */
    @Transactional
    public VehicleDTO updateVehicle(String email, VehicleDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with email: " + email));

        Vehicle vehicle = vehicleRepository.findByUserId(user.getId())
                .orElseGet(() -> Vehicle.builder().user(user).build());

        vehicle.setMake(dto.getMake());
        vehicle.setModel(dto.getModel());
        vehicle.setYear(dto.getYear());
        vehicle.setColor(dto.getColor());
        vehicle.setLicensePlate(dto.getLicensePlate());
        vehicle.setPassengerCapacity(dto.getPassengerCapacity());

        vehicleRepository.save(vehicle);

        return toVehicleDTO(vehicle);
    }

    /**
     * Gets the vehicle linked to the authenticated user, or returns an empty DTO.
     */
    public VehicleDTO getVehicle(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with email: " + email));
        return vehicleRepository.findByUserId(user.getId())
                .map(this::toVehicleDTO)
                .orElse(new VehicleDTO());
    }

    /**
     * Toggles email notification preference for the authenticated user.
     * Requirement 10.5
     */
    @Transactional
    public void updateNotificationPreferences(String email, boolean emailNotificationsEnabled) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with email: " + email));

        user.setEmailNotificationsEnabled(emailNotificationsEnabled);
        userRepository.save(user);
    }

    /**
     * Recomputes the aggregate rating for a user from all received reviews.
     * Called by ReviewService after a new review is submitted.
     * Requirement 2.2
     */
    @Transactional
    public void recomputeAggregateRating(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with id: " + userId));

        Double avg = reviewRepository.computeAverageRatingForUser(userId);
        long count = reviewRepository.countByRevieweeId(userId);

        user.setAggregateRating(avg);
        user.setReviewCount((int) count);
        userRepository.save(user);
    }

    /**
     * GET /users/me/trust-stats — returns driver trust score and total points redeemed on their trips.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTrustStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        // Count total points redeemed by passengers on this driver's trips
        // We approximate: trustScore IS the total points redeemed (1:1 mapping)
        long completedTrips = bookingRepository.findByPassengerIdAndStatus(user.getId(), BookingStatus.COMPLETED).size();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("trustScore", user.getTrustScore());
        stats.put("totalPointsRedeemedOnMyTrips", user.getTrustScore()); // 1 trust = 1 point redeemed
        stats.put("completedTripsAsDriver", completedTrips);
        return stats;
    }

    /**
     * GET /users/me/points-history — returns all completed bookings with points earned per ride.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPointsHistory(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
        List<Booking> completed = bookingRepository.findByPassengerIdAndStatus(
                user.getId(), BookingStatus.COMPLETED);
        return completed.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(b -> {
                    float fare = b.getFareLocked() * b.getSeatsBooked();
                    int pts = Math.min(10, Math.max(5, 5 + (int)(fare / 50)));
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("bookingId", b.getId());
                    m.put("origin", b.getTrip().getOriginAddress());
                    m.put("destination", b.getTrip().getDestinationAddress());
                    m.put("departureTime", b.getTrip().getDepartureTime());
                    m.put("fare", fare);
                    m.put("seats", b.getSeatsBooked());
                    m.put("pointsEarned", pts);
                    m.put("createdAt", b.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ---- Mapping helpers ----

    private UserProfileDTO toUserProfileDTO(User user, VehicleDTO vehicleDTO) {
        // Return null aggregateRating when user has no reviews (Requirement 2.3)
        Double rating = (user.getReviewCount() == 0) ? null : user.getAggregateRating();
        return new UserProfileDTO(
                user.getId(),
                user.getFullName(),
                user.getPhotoUrl(),
                rating,
                user.getReviewCount(),
                vehicleDTO,
                user.isVerified()
        );
    }

    private VehicleDTO toVehicleDTO(Vehicle vehicle) {
        return new VehicleDTO(
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getColor(),
                vehicle.getLicensePlate(),
                vehicle.getPassengerCapacity()
        );
    }
}
