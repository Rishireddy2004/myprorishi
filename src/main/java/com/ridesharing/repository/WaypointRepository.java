package com.ridesharing.repository;

import com.ridesharing.domain.Waypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WaypointRepository extends JpaRepository<Waypoint, UUID> {

    List<Waypoint> findByTripIdOrderBySequenceOrder(UUID tripId);
}
