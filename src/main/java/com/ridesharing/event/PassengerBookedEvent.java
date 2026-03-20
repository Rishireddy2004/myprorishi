package com.ridesharing.event;

import com.ridesharing.domain.Booking;
import org.springframework.context.ApplicationEvent;

public class PassengerBookedEvent extends ApplicationEvent {
    private final Booking booking;

    public PassengerBookedEvent(Object source, Booking booking) {
        super(source);
        this.booking = booking;
    }

    public Booking getBooking() {
        return booking;
    }
}
