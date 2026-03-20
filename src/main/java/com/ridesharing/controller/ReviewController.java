package com.ridesharing.controller;

import com.ridesharing.dto.ReviewRequestDTO;
import com.ridesharing.dto.ReviewResponseDTO;
import com.ridesharing.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * POST /bookings/{id}/review — submit a review from a booking (passenger reviews driver).
     * Derives tripId and revieweeId from the booking.
     */
    @PostMapping("/bookings/{id}/review")
    public ResponseEntity<ReviewResponseDTO> createReviewFromBooking(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReviewResponseDTO response = reviewService.createReviewFromBooking(id, dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /trips/{id}/reviews — submit a review for a completed trip.
     * Requirements 9.1–9.6
     */
    @PostMapping("/trips/{id}/reviews")
    public ResponseEntity<ReviewResponseDTO> createReview(
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        ReviewResponseDTO response = reviewService.createReview(id, dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /users/{id}/reviews — return all reviews for a user ordered by createdAt DESC.
     * Requirement 9.7
     */
    @GetMapping("/users/{id}/reviews")
    public ResponseEntity<List<ReviewResponseDTO>> getReviewsForUser(@PathVariable UUID id) {
        return ResponseEntity.ok(reviewService.getReviewsForUser(id));
    }
}
