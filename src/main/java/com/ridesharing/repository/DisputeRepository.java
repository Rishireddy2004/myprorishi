package com.ridesharing.repository;

import com.ridesharing.domain.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    /** Count disputes by status for admin metrics (Requirement 11.1). */
    long countByStatus(String status);
}
