package com.ridesharing.service;

import com.ridesharing.common.exception.ConflictException;
import com.ridesharing.common.exception.ResourceGoneException;
import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.domain.*;
import com.ridesharing.dto.ReviewRequestDTO;
import com.ridesharing.dto.ReviewResponseDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.ReviewRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final int REVIEW_WINDOW_DAYS = 7;

    private final ReviewRepository reviewRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    /**
     * POST /bookings/{id}/review — passenger reviews the driver from a booking.
     */
    @Transactional
    public ReviewResponseDTO createReviewFromBooking(UUID bookingId, ReviewRequestDTO dto, String reviewerEmail) {
        User reviewer = findUserByEmail(reviewerEmail);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND",
                        "Booking not found with id: " + bookingId));

        if (!booking.getPassenger().getId().equals(reviewer.getId())) {
            throw new ResourceNotFoundException("NOT_YOUR_BOOKING",
                    "You can only review trips from your own bookings.");
        }

        Trip trip = booking.getTrip();
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new ResourceNotFoundException("TRIP_NOT_COMPLETED",
                    "Reviews can only be submitted for completed trips.");
        }

        // Default reviewee to the driver
        UUID revieweeId = dto.getRevieweeId() != null ? dto.getRevieweeId() : trip.getDriver().getId();
        dto.setRevieweeId(revieweeId);

        return createReview(trip.getId(), dto, reviewerEmail);
    }

    /**
     * POST /trips/{id}/reviews — submit a review for a completed trip.
     * Requirements 9.1–9.6
     */
    @Transactional
    public ReviewResponseDTO createReview(UUID tripId, ReviewRequestDTO dto, String reviewerEmail) {
        User reviewer = findUserByEmail(reviewerEmail);

        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("TRIP_NOT_FOUND",
                        "Trip not found with id: " + tripId));

        // Requirement 9.1: trip must be COMPLETED
        if (trip.getStatus() != TripStatus.COMPLETED) {
            throw new ResourceNotFoundException("TRIP_NOT_COMPLETED",
                    "Reviews can only be submitted for completed trips.");
        }

        // Requirement 9.3: reviewer must have a confirmed booking on the trip
        // (driver can also review passengers — check if reviewer is driver or confirmed passenger)
        boolean isDriver = trip.getDriver().getId().equals(reviewer.getId());
        boolean isPassenger = !bookingRepository
                .findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED)
                .isEmpty()
                && bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED)
                .stream()
                .anyMatch(b -> b.getPassenger().getId().equals(reviewer.getId()));

        if (!isDriver && !isPassenger) {
            throw new ResourceNotFoundException("NOT_A_PARTICIPANT",
                    "You must be a participant of this trip to leave a review.");
        }

        // Requirement 9.4: 7-day review window (use departureTime as approximation)
        LocalDateTime windowEnd = trip.getDepartureTime().plusDays(REVIEW_WINDOW_DAYS);
        if (LocalDateTime.now().isAfter(windowEnd)) {
            throw new ResourceGoneException("REVIEW_WINDOW_EXPIRED",
                    "The 7-day review window for this trip has expired.");
        }

        // Resolve reviewee
        UUID revieweeId = dto.getRevieweeId();
        User reviewee = userRepository.findById(revieweeId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "Reviewee not found with id: " + revieweeId));

        // Requirement 9.5: no duplicate reviews
        if (reviewRepository.existsByTripIdAndReviewerIdAndRevieweeId(tripId, reviewer.getId(), revieweeId)) {
            throw new ConflictException("DUPLICATE_REVIEW",
                    "You have already reviewed this user for this trip.");
        }

        // Persist review
        Review review = Review.builder()
                .trip(trip)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .build();

        review = reviewRepository.save(review);

        // Requirement 9.6: recompute and update reviewee's aggregate rating
        Double avg = reviewRepository.computeAverageRatingForUser(revieweeId);
        long count = reviewRepository.countByRevieweeId(revieweeId);
        reviewee.setAggregateRating(avg != null ? avg : 0.0);
        reviewee.setReviewCount((int) count);
        userRepository.save(reviewee);

        return toResponseDTO(review);
    }

    /**
     * GET /users/{id}/reviews — return all reviews for a user ordered by createdAt DESC.
     * Requirement 9.7
     */
    public List<ReviewResponseDTO> getReviewsForUser(UUID userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found with id: " + userId));

        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // ---- Private helpers ----

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND",
                        "User not found: " + email));
    }

    private ReviewResponseDTO toResponseDTO(Review review) {
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
