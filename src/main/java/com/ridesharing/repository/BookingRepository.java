package com.ridesharing.repository;

import com.ridesharing.domain.Booking;
import com.ridesharing.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findByTripIdAndStatus(UUID tripId, BookingStatus status);

    List<Booking> findByTripId(UUID tripId);

    List<Booking> findByPassengerId(UUID passengerId);

    List<Booking> findByPassengerIdAndStatus(UUID passengerId, BookingStatus status);

    long countByTripIdAndStatus(UUID tripId, BookingStatus status);

    /** Count completed trips driven by a specific driver (via trip.driver.id). */
    @org.springframework.data.jpa.repository.Query(
        "SELECT COUNT(DISTINCT b.trip.id) FROM Booking b WHERE b.trip.driver.id = :driverId AND b.status = 'COMPLETED'")
    long countCompletedTripsByDriverId(@org.springframework.data.repository.query.Param("driverId") UUID driverId);
}
