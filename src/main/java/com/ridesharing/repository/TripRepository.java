package com.ridesharing.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ridesharing.domain.Trip;
import com.ridesharing.domain.TripStatus;

import jakarta.persistence.LockModeType;

@Repository
public interface TripRepository extends JpaRepository<Trip, UUID> {

    /**
     * Fetch a Trip with a pessimistic write lock to prevent overbooking
     * when multiple passengers book concurrently (Requirement 5.1).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trip t WHERE t.id = :id")
    Optional<Trip> findByIdWithLock(UUID id);

    /**
     * Find OPEN trips whose origin or destination falls within the given lat/lng bounding box
     * and whose departure time is on the requested date (Requirements 4.1, 4.2).
     *
     * The bounding box is applied to both origin and destination so that trips passing
     * through the search area are included.
     */
    @Query("SELECT t FROM Trip t " +
           "WHERE t.status = :status " +
           "AND t.departureTime >= :dayStart AND t.departureTime < :dayEnd " +
           "AND (" +
           "  (t.originLat BETWEEN :minLat AND :maxLat AND t.originLng BETWEEN :minLng AND :maxLng) " +
           "  OR " +
           "  (t.destinationLat BETWEEN :minLat AND :maxLat AND t.destinationLng BETWEEN :minLng AND :maxLng)" +
           ")")
    List<Trip> findByBoundingBox(@Param("minLat") double minLat,
                                 @Param("maxLat") double maxLat,
                                 @Param("minLng") double minLng,
                                 @Param("maxLng") double maxLng,
                                 @Param("dayStart") LocalDateTime dayStart,
                                 @Param("dayEnd") LocalDateTime dayEnd,
                                 @Param("status") TripStatus status);

    /**
     * Convenience overload that defaults status to OPEN.
     */
    default List<Trip> findByBoundingBox(double minLat, double maxLat,
                                         double minLng, double maxLng,
                                         LocalDateTime dayStart, LocalDateTime dayEnd) {
        return findByBoundingBox(minLat, maxLat, minLng, maxLng, dayStart, dayEnd, TripStatus.OPEN);
    }

    /**
     * Find trips departing within a time window with a given status.
     * Used by NotificationService to send ride reminders (Requirement 10.6).
     */
    List<Trip> findByDepartureTimeBetweenAndStatus(LocalDateTime from, LocalDateTime to, TripStatus status);

    /** Count trips by status for admin metrics (Requirement 11.1). */
    long countByStatus(TripStatus status);

    /** Find trips by driver id for admin user detail (Requirement 11.2). */
    List<Trip> findByDriverId(UUID driverId);

    /**
     * Admin trip search by trip id string, origin address, destination address, or driver name
     * (Requirement 11.3).
     */
    @Query("SELECT t FROM Trip t JOIN t.driver d WHERE " +
           "LOWER(t.originAddress) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(t.destinationAddress) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(d.fullName) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Trip> searchByAdmin(@Param("q") String query);

    /** Find all OPEN trips for fallback search when geocoding returns stub coords. */
    List<Trip> findByStatus(TripStatus status);
}
