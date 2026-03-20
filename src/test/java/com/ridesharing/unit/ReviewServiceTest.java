package com.ridesharing.unit;

import com.ridesharing.common.exception.ResourceGoneException;
import com.ridesharing.common.exception.UnprocessableEntityException;
import com.ridesharing.domain.*;
import com.ridesharing.dto.ReviewRequestDTO;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.ReviewRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ReviewService.
 * Validates: Requirements 9.1–9.7
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private TripRepository tripRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    // ---- 7-day window boundary ----

    @Test
    void createReview_exactlySevenDaysAfterDeparture_throwsResourceGone() {
        UUID tripId = UUID.randomUUID();
        User reviewer = buildUser("reviewer@example.com");
        // Departure exactly 7 days ago → window has expired
        LocalDateTime departure = LocalDateTime.now().minusDays(7);
        Trip trip = buildCompletedTrip(tripId, departure, reviewer);

        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(reviewer));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(buildConfirmedBooking(trip, reviewer)));

        ReviewRequestDTO dto = buildReviewDto(trip.getDriver().getId(), 4);

        assertThatThrownBy(() -> reviewService.createReview(tripId, dto, "reviewer@example.com"))
                .isInstanceOf(ResourceGoneException.class)
                .hasMessageContaining("7-day");
    }

    @Test
    void createReview_withinSevenDays_doesNotThrowWindowExpired() {
        UUID tripId = UUID.randomUUID();
        User reviewer = buildUser("reviewer@example.com");
        // Departure 6 days ago → still within window
        LocalDateTime departure = LocalDateTime.now().minusDays(6);
        Trip trip = buildCompletedTrip(tripId, departure, reviewer);
        User reviewee = trip.getDriver();

        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(reviewer));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(buildConfirmedBooking(trip, reviewer)));
        when(userRepository.findById(reviewee.getId())).thenReturn(Optional.of(reviewee));
        when(reviewRepository.existsByTripIdAndReviewerIdAndRevieweeId(any(), any(), any())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            return r;
        });
        when(reviewRepository.computeAverageRatingForUser(reviewee.getId())).thenReturn(4.0);
        when(reviewRepository.countByRevieweeId(reviewee.getId())).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw ResourceGoneException
        reviewService.createReview(tripId, buildReviewDto(reviewee.getId(), 4), "reviewer@example.com");
    }

    // ---- Rating boundary ----

    @Test
    void createReview_ratingOfOne_isValid() {
        UUID tripId = UUID.randomUUID();
        User reviewer = buildUser("reviewer@example.com");
        LocalDateTime departure = LocalDateTime.now().minusDays(1);
        Trip trip = buildCompletedTrip(tripId, departure, reviewer);
        User reviewee = trip.getDriver();

        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(reviewer));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(buildConfirmedBooking(trip, reviewer)));
        when(userRepository.findById(reviewee.getId())).thenReturn(Optional.of(reviewee));
        when(reviewRepository.existsByTripIdAndReviewerIdAndRevieweeId(any(), any(), any())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.computeAverageRatingForUser(reviewee.getId())).thenReturn(1.0);
        when(reviewRepository.countByRevieweeId(reviewee.getId())).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Rating 1 should be accepted without exception
        reviewService.createReview(tripId, buildReviewDto(reviewee.getId(), 1), "reviewer@example.com");
    }

    @Test
    void createReview_ratingOfFive_isValid() {
        UUID tripId = UUID.randomUUID();
        User reviewer = buildUser("reviewer@example.com");
        LocalDateTime departure = LocalDateTime.now().minusDays(1);
        Trip trip = buildCompletedTrip(tripId, departure, reviewer);
        User reviewee = trip.getDriver();

        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(reviewer));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(buildConfirmedBooking(trip, reviewer)));
        when(userRepository.findById(reviewee.getId())).thenReturn(Optional.of(reviewee));
        when(reviewRepository.existsByTripIdAndReviewerIdAndRevieweeId(any(), any(), any())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.computeAverageRatingForUser(reviewee.getId())).thenReturn(5.0);
        when(reviewRepository.countByRevieweeId(reviewee.getId())).thenReturn(1L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Rating 5 should be accepted without exception
        reviewService.createReview(tripId, buildReviewDto(reviewee.getId(), 5), "reviewer@example.com");
    }

    // ---- Aggregate mean with 0 reviews ----

    @Test
    void createReview_firstReview_aggregateRatingSetFromRepository() {
        UUID tripId = UUID.randomUUID();
        User reviewer = buildUser("reviewer@example.com");
        LocalDateTime departure = LocalDateTime.now().minusDays(1);
        Trip trip = buildCompletedTrip(tripId, departure, reviewer);
        User reviewee = trip.getDriver();

        when(userRepository.findByEmail("reviewer@example.com")).thenReturn(Optional.of(reviewer));
        when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        when(bookingRepository.findByTripIdAndStatus(tripId, BookingStatus.CONFIRMED))
                .thenReturn(List.of(buildConfirmedBooking(trip, reviewer)));
        when(userRepository.findById(reviewee.getId())).thenReturn(Optional.of(reviewee));
        when(reviewRepository.existsByTripIdAndReviewerIdAndRevieweeId(any(), any(), any())).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        // Repository returns null when no reviews exist yet (before this one is counted)
        when(reviewRepository.computeAverageRatingForUser(reviewee.getId())).thenReturn(null);
        when(reviewRepository.countByRevieweeId(reviewee.getId())).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.createReview(tripId, buildReviewDto(reviewee.getId(), 4), "reviewer@example.com");

        // When avg is null, service should set aggregateRating to 0.0 (not throw NPE)
        assertThat(reviewee.getAggregateRating()).isEqualTo(0.0);
    }

    // ---- helpers ----

    private User buildUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .fullName("Test User")
                .build();
    }

    private Trip buildCompletedTrip(UUID tripId, LocalDateTime departureTime, User passenger) {
        User driver = buildUser("driver@example.com");
        return Trip.builder()
                .id(tripId)
                .driver(driver)
                .status(TripStatus.COMPLETED)
                .departureTime(departureTime)
                .originAddress("Origin")
                .destinationAddress("Destination")
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .baseFarePerKm(5.0)
                .serviceFeeRate(0.1)
                .totalSeats(4)
                .availableSeats(3)
                .build();
    }

    private Booking buildConfirmedBooking(Trip trip, User passenger) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .passenger(passenger)
                .status(BookingStatus.CONFIRMED)
                .fareLocked(50.0f)
                .distanceKm(10.0f)
                .seatsBooked(1)
                .build();
    }

    private ReviewRequestDTO buildReviewDto(UUID revieweeId, int rating) {
        ReviewRequestDTO dto = new ReviewRequestDTO();
        dto.setRevieweeId(revieweeId);
        dto.setRating(rating);
        dto.setComment("Test comment");
        return dto;
    }
}
