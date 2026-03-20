package com.ridesharing.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridesharing.domain.*;
import com.ridesharing.event.BookingConfirmedEvent;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.NotificationRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 * Validates: Requirements 10.1–10.6
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private TripRepository tripRepository;
    @Mock private JavaMailSender mailSender;

    // Use a real SimpMessagingTemplate backed by a no-op channel to avoid Mockito issues on Java 25
    private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        // Create a no-op MessageChannel stub
        MessageChannel noOpChannel = (message, timeout) -> true;
        messagingTemplate = new SimpMessagingTemplate(noOpChannel);

        notificationService = new NotificationService(
                notificationRepository,
                bookingRepository,
                tripRepository,
                messagingTemplate,
                mailSender,
                new ObjectMapper()
        );
    }

    // ---- Retry count boundary ----

    @Test
    void maxRetryAttempts_isThree() {
        // Verify the MAX_RETRY_ATTEMPTS constant is 3 via reflection
        java.lang.reflect.Field[] fields = NotificationService.class.getDeclaredFields();
        int maxRetries = -1;
        for (java.lang.reflect.Field f : fields) {
            if (f.getName().equals("MAX_RETRY_ATTEMPTS")) {
                f.setAccessible(true);
                try { maxRetries = (int) f.get(null); } catch (Exception ignored) {}
            }
        }
        assertThat(maxRetries).isEqualTo(3);
    }

    @Test
    void onBookingConfirmed_webSocketSucceeds_onlyOneSaveOccurs() {
        User passenger = buildUser("passenger@example.com", false);
        Trip trip = buildTrip();
        Booking booking = buildBooking(trip, passenger);
        BookingConfirmedEvent event = new BookingConfirmedEvent(this, booking);

        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(passenger)
                .type("BOOKING_CONFIRMED")
                .retryCount(0)
                .isRead(false)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.onBookingConfirmed(event);

        // Only the initial save — no retry saves since delivery succeeded
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    // ---- Email preference gate ----

    @Test
    void onBookingConfirmed_emailNotificationsDisabled_emailNotSent() {
        User passenger = buildUser("passenger@example.com", false);
        Trip trip = buildTrip();
        Booking booking = buildBooking(trip, passenger);
        BookingConfirmedEvent event = new BookingConfirmedEvent(this, booking);

        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(passenger)
                .type("BOOKING_CONFIRMED")
                .retryCount(0)
                .isRead(false)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.onBookingConfirmed(event);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void onBookingConfirmed_emailNotificationsEnabled_emailIsSent() {
        User passenger = buildUser("passenger@example.com", true);
        Trip trip = buildTrip();
        Booking booking = buildBooking(trip, passenger);
        BookingConfirmedEvent event = new BookingConfirmedEvent(this, booking);

        Notification savedNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(passenger)
                .type("BOOKING_CONFIRMED")
                .retryCount(0)
                .isRead(false)
                .build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.onBookingConfirmed(event);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ---- helpers ----

    private User buildUser(String email, boolean emailNotificationsEnabled) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .fullName("Test User")
                .build();
        user.setEmailNotificationsEnabled(emailNotificationsEnabled);
        return user;
    }

    private Trip buildTrip() {
        return Trip.builder()
                .id(UUID.randomUUID())
                .driver(User.builder().id(UUID.randomUUID()).email("driver@example.com")
                        .passwordHash("hash").fullName("Driver").build())
                .status(TripStatus.OPEN)
                .departureTime(LocalDateTime.now().plusHours(5))
                .originAddress("Origin")
                .destinationAddress("Destination")
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .baseFarePerKm(5.0)
                .serviceFeeRate(0.1)
                .totalSeats(4)
                .availableSeats(3)
                .build();
    }

    private Booking buildBooking(Trip trip, User passenger) {
        return Booking.builder()
                .id(UUID.randomUUID())
                .trip(trip)
                .passenger(passenger)
                .status(BookingStatus.CONFIRMED)
                .fareLocked(50.0f)
                .distanceKm(10.0f)
                .seatsBooked(1)
                .build();
    }
}
