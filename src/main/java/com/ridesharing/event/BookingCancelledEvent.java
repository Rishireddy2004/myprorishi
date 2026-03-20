package com.ridesharing.event;

import com.ridesharing.domain.Booking;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Published when a booking is cancelled.
 * NotificationService (task 14) will consume this event to notify the driver.
 */
public class BookingCancelledEvent extends ApplicationEvent {

    private final UUID bookingId;
    private final Booking booking;

    public BookingCancelledEvent(Object source, Booking booking) {
        super(source);
        this.bookingId = booking.getId();
        this.booking = booking;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public Booking getBooking() {
        return booking;
    }
}
