package com.ridesharing.event;

import com.ridesharing.domain.Trip;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Published when a driver cancels an open trip.
 * NotificationService (task 14) will consume this event to notify confirmed passengers.
 */
public class TripCancelledEvent extends ApplicationEvent {

    private final UUID tripId;
    private final Trip trip;

    public TripCancelledEvent(Object source, Trip trip) {
        super(source);
        this.tripId = trip.getId();
        this.trip = trip;
    }

    public UUID getTripId() {
        return tripId;
    }

    public Trip getTrip() {
        return trip;
    }
}
