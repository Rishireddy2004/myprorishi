package com.ridesharing.repository;

import com.ridesharing.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByRevieweeIdOrderByCreatedAtDesc(UUID revieweeId);

    boolean existsByTripIdAndReviewerIdAndRevieweeId(UUID tripId, UUID reviewerId, UUID revieweeId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewee.id = :userId")
    Double computeAverageRatingForUser(@Param("userId") UUID userId);

    long countByRevieweeId(UUID revieweeId);
}
