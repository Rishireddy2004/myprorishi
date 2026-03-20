package com.ridesharing.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boarding_waypoint_id")
    private Waypoint boardingWaypoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alighting_waypoint_id")
    private Waypoint alightingWaypoint;

    /**
     * Fare locked at booking confirmation time; never updated after insert (Requirement 6.5).
     */
    @Column(nullable = false, updatable = false)
    private float fareLocked;

    @Column(nullable = false)
    private float distanceKm;

    @Column(nullable = false)
    private int seatsBooked;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column
    private String paymentIntentId;

    /** Optional tip amount given by passenger to driver */
    @Column(nullable = false)
    @Builder.Default
    private float tipAmount = 0f;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
