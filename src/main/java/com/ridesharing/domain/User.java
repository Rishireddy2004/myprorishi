package com.ridesharing.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Column
    private String phone;

    @Column
    private String photoUrl;

    @Column
    private Double aggregateRating;

    @Column(nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isSuspended = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailNotificationsEnabled = true;

    /** Loyalty points earned from completed rides */
    @Column(nullable = false)
    @Builder.Default
    private int loyaltyPoints = 0;

    /** Trust score: incremented when passengers redeem points on this driver's trips */
    @Column(nullable = false)
    @Builder.Default
    private int trustScore = 0;

    /** Role: PASSENGER, DRIVER, BOTH, or ADMIN */
    @Column(nullable = false)
    @Builder.Default
    private String role = "PASSENGER";

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
