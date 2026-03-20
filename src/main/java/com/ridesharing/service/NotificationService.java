package com.ridesharing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridesharing.domain.Booking;
import com.ridesharing.domain.BookingStatus;
import com.ridesharing.domain.Notification;
import com.ridesharing.domain.Trip;
import com.ridesharing.domain.TripStatus;
import com.ridesharing.domain.User;
import com.ridesharing.event.BookingCancelledEvent;
import com.ridesharing.event.BookingConfirmedEvent;
import com.ridesharing.event.PassengerBookedEvent;
import com.ridesharing.event.TripCancelledEvent;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.NotificationRepository;
import com.ridesharing.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles in-platform and email notifications for booking/trip lifecycle events.
 * Requirements 10.1 – 10.6
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;

    public NotificationService(NotificationRepository notificationRepository,
                                BookingRepository bookingRepository,
                                TripRepository tripRepository,
                                SimpMessagingTemplate messagingTemplate,
                                JavaMailSender mailSender,
                                ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.bookingRepository = bookingRepository;
        this.tripRepository = tripRepository;
        this.messagingTemplate = messagingTemplate;
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Event listeners
    // -------------------------------------------------------------------------

    /**
     * Notify driver when a passenger books a seat — driver must accept/reject.
     * Runs AFTER the booking transaction commits so the DB connection is free.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPassengerBooked(PassengerBookedEvent event) {
        Booking booking = event.getBooking();
        User driver = booking.getTrip().getDriver();
        User passenger = booking.getPassenger();
        Trip trip = booking.getTrip();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PASSENGER_BOOKED");
        payload.put("bookingId", booking.getId());
        payload.put("tripId", trip.getId());
        payload.put("passengerName", passenger.getFullName());
        payload.put("passengerPhone", passenger.getPhone());
        payload.put("seats", booking.getSeatsBooked());
        payload.put("departure", trip.getDepartureTime());
        payload.put("tipAmount", booking.getTipAmount());

        saveAndPush(driver, "PASSENGER_BOOKED", payload);

        // Email driver
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(driver.getEmail());
            msg.setSubject("RideSharing – New Booking Request");
            String tipLine = booking.getTipAmount() > 0
                ? String.format("\nTip offered: ₹%.0f 🎉", booking.getTipAmount())
                : "";
            msg.setText(String.format(
                """
                Hi %s,

                %s has booked %d seat(s) on your trip:
                  From: %s
                  To:   %s
                  Departure: %s
                  Fare: ₹%.0f%s

                Passenger phone: %s

                Please log in to Accept or Reject this booking.

                RideSharing Team
                """,
                driver.getFullName(),
                passenger.getFullName(),
                booking.getSeatsBooked(),
                trip.getOriginAddress(),
                trip.getDestinationAddress(),
                trip.getDepartureTime(),
                (double) booking.getFareLocked(),
                tipLine,
                passenger.getPhone() != null ? passenger.getPhone() : "not provided"
            ));
            mailSender.send(msg);
            log.info("Booking notification email sent to driver {}", driver.getEmail());
        } catch (MailException e) {
            log.warn("Could not send booking email to driver {}: {}", driver.getEmail(), e.getMessage());
        }
    }

    /**
     * Requirement 10.1 – notify passenger when booking is confirmed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        Booking booking = event.getBooking();
        User passenger = booking.getPassenger();
        Trip trip = booking.getTrip();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "BOOKING_CONFIRMED");
        payload.put("bookingId", booking.getId());
        payload.put("tripId", trip.getId());
        payload.put("departure", trip.getDepartureTime());

        saveAndPush(passenger, "BOOKING_CONFIRMED", payload);
    }

    /**
     * Requirement 10.2 – notify driver when a booking is cancelled.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onBookingCancelled(BookingCancelledEvent event) {
        Booking booking = event.getBooking();
        User driver = booking.getTrip().getDriver();

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "BOOKING_CANCELLED");
        payload.put("bookingId", booking.getId());
        payload.put("tripId", booking.getTrip().getId());

        saveAndPush(driver, "BOOKING_CANCELLED", payload);
    }

    /**
     * Requirement 10.3 – notify all confirmed passengers when a trip is cancelled.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTripCancelled(TripCancelledEvent event) {
        Trip trip = event.getTrip();
        List<Booking> confirmedBookings =
                bookingRepository.findByTripIdAndStatus(trip.getId(), BookingStatus.CONFIRMED);

        for (Booking booking : confirmedBookings) {
            User passenger = booking.getPassenger();
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "TRIP_CANCELLED");
            payload.put("tripId", trip.getId());
            payload.put("bookingId", booking.getId());
            saveAndPush(passenger, "TRIP_CANCELLED", payload);
        }
    }

    // -------------------------------------------------------------------------
    // Ride reminder scheduler – Requirement 10.6
    // -------------------------------------------------------------------------

    /**
     * Runs every minute; finds trips departing in ~2 hours and sends reminders
     * to confirmed passengers who have not yet received one.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sendRideReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusHours(1).plusMinutes(55);
        LocalDateTime windowEnd = now.plusHours(2).plusMinutes(5);

        // Find OPEN trips departing in the 2-hour window
        List<Trip> upcomingTrips = tripRepository.findByDepartureTimeBetweenAndStatus(
                windowStart, windowEnd, TripStatus.OPEN);

        for (Trip trip : upcomingTrips) {
            List<Booking> confirmedBookings =
                    bookingRepository.findByTripIdAndStatus(trip.getId(), BookingStatus.CONFIRMED);

            for (Booking booking : confirmedBookings) {
                User passenger = booking.getPassenger();

                // Check if a reminder was already sent for this booking
                boolean alreadySent = notificationRepository
                        .findByUserIdOrderByCreatedAtDesc(passenger.getId())
                        .stream()
                        .anyMatch(n -> "RIDE_REMINDER".equals(n.getType())
                                && n.getPayloadJson() != null
                                && n.getPayloadJson().contains(booking.getId().toString()));

                if (!alreadySent) {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("type", "RIDE_REMINDER");
                    payload.put("tripId", trip.getId());
                    payload.put("bookingId", booking.getId());
                    payload.put("departure", trip.getDepartureTime());

                    sendNotification(passenger, "RIDE_REMINDER", payload);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Core delivery logic — no blocking, no sleep
    // -------------------------------------------------------------------------

    /**
     * Persists a Notification record and pushes via WebSocket.
     * Email is sent separately per event handler.
     */
    private void saveAndPush(User user, String type, Map<String, Object> payload) {
        String payloadJson = toJson(payload);

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .payloadJson(payloadJson)
                .retryCount(0)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        try {
            pushWebSocket(user, payloadJson);
        } catch (Exception e) {
            log.warn("WebSocket push failed for user {}: {}", user.getId(), e.getMessage());
        }
    }

    /** @deprecated use saveAndPush — kept for sendSuspensionNotification */
    private void sendNotification(User user, String type, Map<String, Object> payload) {
        saveAndPush(user, type, payload);
    }

    // -------------------------------------------------------------------------
    // Delivery channels
    // -------------------------------------------------------------------------

    private void pushWebSocket(User user, String payloadJson) {
        messagingTemplate.convertAndSend(
                "/topic/user/" + user.getId() + "/notifications",
                payloadJson);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize notification payload", e);
            return "{}";
        }
    }

    private String formatSubject(String type) {
        return switch (type) {
            case "BOOKING_CONFIRMED" -> "Booking Confirmed";
            case "BOOKING_CANCELLED" -> "Booking Cancelled";
            case "PASSENGER_BOOKED" -> "New Booking Request";
            case "TRIP_CANCELLED" -> "Trip Cancelled";
            case "RIDE_REMINDER" -> "Ride Reminder – Departing Soon";
            case "ACCOUNT_SUSPENDED" -> "Account Suspended";
            case "ACCOUNT_UNSUSPENDED" -> "Account Reinstated";
            default -> type;
        };
    }

    /**
     * Sends a suspension or unsuspension notification to the affected user (Requirement 11.4).
     */
    public void sendSuspensionNotification(User user, boolean suspended) {
        String type = suspended ? "ACCOUNT_SUSPENDED" : "ACCOUNT_UNSUSPENDED";
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("userId", user.getId());
        sendNotification(user, type, payload);
    }
}
