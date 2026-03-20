package com.ridesharing.unit;

import com.ridesharing.client.PaymentGatewayClient;
import com.ridesharing.domain.*;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TransactionRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.UserRepository;
import com.ridesharing.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentService.
 * Validates: Requirements 7.1–7.10
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentGatewayClient paymentGatewayClient;
    @Mock private BookingRepository bookingRepository;
    @Mock private TripRepository tripRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private PaymentService paymentService;

    // ---- Refund policy boundaries ----

    @Test
    void processRefund_moreThan24HoursBeforeDeparture_fullRefund() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Booking booking = buildBookingWithDeparture(bookingId, LocalDateTime.now().plusHours(26), 100.0f);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentGatewayClient.refund(eq("pi_test"), anyDouble())).thenReturn("re_full");

        paymentService.processRefund(bookingId);

        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
        verify(paymentGatewayClient).refund(eq("pi_test"), amountCaptor.capture());
        assertThat(amountCaptor.getValue()).isEqualTo(100.0);
    }

    @Test
    void processRefund_exactly24HoursBeforeDeparture_fiftyPercentRefund() throws Exception {
        UUID bookingId = UUID.randomUUID();
        // Exactly 24h → hoursUntilDeparture = 24, which is NOT > 24, so 50% refund
        Booking booking = buildBookingWithDeparture(bookingId, LocalDateTime.now().plusHours(24), 100.0f);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentGatewayClient.refund(eq("pi_test"), anyDouble())).thenReturn("re_half");

        paymentService.processRefund(bookingId);

        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
        verify(paymentGatewayClient).refund(eq("pi_test"), amountCaptor.capture());
        assertThat(amountCaptor.getValue()).isEqualTo(50.0);
    }

    @Test
    void processRefund_exactly2HoursBeforeDeparture_fiftyPercentRefund() throws Exception {
        UUID bookingId = UUID.randomUUID();
        Booking booking = buildBookingWithDeparture(bookingId, LocalDateTime.now().plusHours(2), 100.0f);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentGatewayClient.refund(eq("pi_test"), anyDouble())).thenReturn("re_half");

        paymentService.processRefund(bookingId);

        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
        verify(paymentGatewayClient).refund(eq("pi_test"), amountCaptor.capture());
        assertThat(amountCaptor.getValue()).isEqualTo(50.0);
    }

    // ---- Payout calculation ----

    @Test
    void captureAndPayout_zeroFeeRate_driverGetsFullFare() throws Exception {
        UUID bookingId = UUID.randomUUID();
        User driver = buildUser("driver@example.com");
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildTrip(driver, 0.0); // 0% service fee
        Booking booking = buildBookingForCapture(bookingId, trip, passenger, 100.0f);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doNothing().when(paymentGatewayClient).captureHold("pi_test");

        paymentService.captureAndPayout(bookingId);

        // Verify payout transaction: driver should get 100% of fare
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        Transaction payoutTx = txCaptor.getAllValues().stream()
                .filter(t -> "PAYOUT".equals(t.getType()))
                .findFirst()
                .orElseThrow();
        assertThat((double) payoutTx.getAmount()).isEqualTo(100.0);
    }

    @Test
    void captureAndPayout_hundredPercentFeeRate_driverGetsNothing() throws Exception {
        UUID bookingId = UUID.randomUUID();
        User driver = buildUser("driver@example.com");
        User passenger = buildUser("passenger@example.com");
        Trip trip = buildTrip(driver, 1.0); // 100% service fee
        Booking booking = buildBookingForCapture(bookingId, trip, passenger, 100.0f);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        doNothing().when(paymentGatewayClient).captureHold("pi_test");

        paymentService.captureAndPayout(bookingId);

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        Transaction payoutTx = txCaptor.getAllValues().stream()
                .filter(t -> "PAYOUT".equals(t.getType()))
                .findFirst()
                .orElseThrow();
        assertThat((double) payoutTx.getAmount()).isEqualTo(0.0);
    }

    // ---- helpers ----

    private User buildUser(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hash")
                .fullName("Test User")
                .build();
    }

    private Trip buildTrip(User driver, double serviceFeeRate) {
        return Trip.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .serviceFeeRate(serviceFeeRate)
                .baseFarePerKm(5.0)
                .totalSeats(4)
                .availableSeats(3)
                .departureTime(LocalDateTime.now().plusHours(5))
                .status(TripStatus.OPEN)
                .originAddress("Origin")
                .destinationAddress("Destination")
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .build();
    }

    private Booking buildBookingWithDeparture(UUID bookingId, LocalDateTime departureTime, float fare) {
        User driver = buildUser("driver@example.com");
        User passenger = buildUser("passenger@example.com");
        Trip trip = Trip.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .serviceFeeRate(0.1)
                .baseFarePerKm(5.0)
                .totalSeats(4)
                .availableSeats(3)
                .departureTime(departureTime)
                .status(TripStatus.OPEN)
                .originAddress("Origin")
                .destinationAddress("Destination")
                .originLat(0.0).originLng(0.0)
                .destinationLat(1.0).destinationLng(1.0)
                .build();
        return Booking.builder()
                .id(bookingId)
                .trip(trip)
                .passenger(passenger)
                .status(BookingStatus.CONFIRMED)
                .fareLocked(fare)
                .distanceKm(10.0f)
                .seatsBooked(1)
                .paymentIntentId("pi_test")
                .build();
    }

    private Booking buildBookingForCapture(UUID bookingId, Trip trip, User passenger, float fare) {
        return Booking.builder()
                .id(bookingId)
                .trip(trip)
                .passenger(passenger)
                .status(BookingStatus.CONFIRMED)
                .fareLocked(fare)
                .distanceKm(10.0f)
                .seatsBooked(1)
                .paymentIntentId("pi_test")
                .build();
    }
}
