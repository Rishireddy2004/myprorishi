package com.ridesharing.repository;

import com.ridesharing.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    /** Admin user search by email or full name (Requirement 11.2). */
    List<User> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(String email, String fullName);

    /** Count non-suspended users for metrics (Requirement 11.1). */
    long countByIsSuspendedFalse();
}
