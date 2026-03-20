package com.ridesharing.service;

import com.ridesharing.client.PaymentGatewayClient;
import com.ridesharing.common.exception.ExternalServiceException;
import com.ridesharing.common.exception.ResourceNotFoundException;
import com.ridesharing.domain.*;
import com.ridesharing.repository.BookingRepository;
import com.ridesharing.repository.TransactionRepository;
import com.ridesharing.repository.TripRepository;
import com.ridesharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Handles all payment lifecycle operations:
 * - Hold on booking confirmation (Req 7.1, 7.2)
 * - Capture + driver payout on trip completion (Req 7.3, 7.4)
 * - Refund on booking cancellation (Req 7.5, 7.6, 7.7)
 * - Payment failure handling (Req 7.8, 7.9)
 * - Admin-initiated refund (Req 7.10)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String CURRENCY = "INR";

    private final PaymentGatewayClient paymentGatewayClient;
    private final BookingRepository bookingRepository;
    private final TripRepository tripRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    /**
     * Called on booking confirmation: creates a payment hold and persists a HOLD transaction.
     * On gateway failure: sets Booking.status = PAYMENT_FAILED, does NOT decrement available_seats,
     * and throws ExternalServiceException (502).
     *
     * @param bookingId the confirmed booking
     */
    @Transactional
    public void createHold(java.util.UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND",
                        "Booking not found: " + bookingId));

        try {
            String paymentIntentId = paymentGatewayClient.createHold(
                    booking.getFareLocked(), CURRENCY);

            booking.setPaymentIntentId(paymentIntentId);
            bookingRepository.save(booking);

            persistTransaction(booking.getPassenger(), booking,
                    "HOLD", booking.getFareLocked(), CURRENCY, "COMPLETED", paymentIntentId);

        } catch (Exception e) {
            log.error("Payment hold failed for booking {}: {}", bookingId, e.getMessage(), e);

            // Mark booking as PAYMENT_FAILED; do NOT restore available_seats (Req 7.8)
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            bookingRepository.save(booking);

            persistTransaction(booking.getPassenger(), booking,
                    "HOLD", booking.getFareLocked(), CURRENCY, "FAILED", null);

            throw new ExternalServiceException("PAYMENT_GATEWAY_ERROR",
                    "Payment gateway failed to create hold. Please try again.", e);
        }
    }

    /**
     * Called on trip completion: captures the hold and transfers the driver's share.
     * Driver payout = fareLocked × (1 − serviceFeeRate).
     *
     * @param bookingId the completed booking
     */
    @Transactional
    public void captureAndPayout(java.util.UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND",
                        "Booking not found: " + bookingId));

        String paymentIntentId = booking.getPaymentIntentId();
        if (paymentIntentId == null) {
            log.warn("No paymentIntentId on booking {} — skipping capture", bookingId);
            return;
        }

        try {
            paymentGatewayClient.captureHold(paymentIntentId);

            persistTransaction(booking.getPassenger(), booking,
                    "CAPTURE", booking.getFareLocked(), CURRENCY, "COMPLETED", paymentIntentId);

            // Driver payout = fare × (1 − serviceFeeRate)
            Trip trip = booking.getTrip();
            double driverShare = booking.getFareLocked() * (1.0 - trip.getServiceFeeRate());
            driverShare = Math.round(driverShare * 100.0) / 100.0;

            persistTransaction(trip.getDriver(), booking,
                    "PAYOUT", (float) driverShare, CURRENCY, "COMPLETED", paymentIntentId);

        } catch (Exception e) {
            log.error("Capture/payout failed for booking {}: {}", bookingId, e.getMessage(), e);

            persistTransaction(booking.getPassenger(), booking,
                    "CAPTURE", booking.getFareLocked(), CURRENCY, "FAILED", paymentIntentId);

            throw new ExternalServiceException("PAYMENT_GATEWAY_ERROR",
                    "Payment gateway failed to capture hold.", e);
        }
    }

    /**
     * Called on booking cancellation. Applies refund policy:
     * - > 24h before departure: full refund
     * - 2–24h before departure: 50% refund
     *
     * @param bookingId the cancelled booking
     */
    @Transactional
    public void processRefund(java.util.UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOKING_NOT_FOUND",
                        "Booking not found: " + bookingId));

        String paymentIntentId = booking.getPaymentIntentId();
        if (paymentIntentId == null) {
            log.warn("No paymentIntentId on booking {} — skipping refund", bookingId);
            return;
        }

        Trip trip = booking.getTrip();
        long hoursUntilDeparture = ChronoUnit.HOURS.between(
                LocalDateTime.now(), trip.getDepartureTime());

        double refundAmount;
        if (hoursUntilDeparture > 24) {
            refundAmount = booking.getFareLocked(); // full refund
        } else {
            refundAmount = booking.getFareLocked() * 0.5; // 50% refund
        }
        refundAmount = Math.round(refundAmount * 100.0) / 100.0;

        try {
            String refundId = paymentGatewayClient.refund(paymentIntentId, refundAmount);

            persistTransaction(booking.getPassenger(), booking,
                    "REFUND", (float) refundAmount, CURRENCY, "COMPLETED", refundId);

        } catch (Exception e) {
            log.error("Refund failed for booking {}: {}", bookingId, e.getMessage(), e);

            persistTransaction(booking.getPassenger(), booking,
                    "REFUND", (float) refundAmount, CURRENCY, "FAILED", null);

            throw new ExternalServiceException("PAYMENT_GATEWAY_ERROR",
                    "Payment gateway failed to process refund.", e);
        }
    }

    /**
     * Admin-initiated refund for an exact specified amount (Req 7.10).
     *
     * @param paymentIntentId the original payment intent to refund against
     * @param user            the user receiving the refund
     * @param booking         the associated booking
     * @param amount          exact refund amount
     */
    @Transactional
    public void adminRefund(String paymentIntentId, User user, Booking booking, double amount) {
        try {
            String refundId = paymentGatewayClient.refund(paymentIntentId, amount);

            persistTransaction(user, booking,
                    "REFUND", (float) amount, CURRENCY, "COMPLETED", refundId);

        } catch (Exception e) {
            log.error("Admin refund failed for paymentIntent {}: {}", paymentIntentId, e.getMessage(), e);

            persistTransaction(user, booking,
                    "REFUND", (float) amount, CURRENCY, "FAILED", null);

            throw new ExternalServiceException("PAYMENT_GATEWAY_ERROR",
                    "Payment gateway failed to process admin refund.", e);
        }
    }

    // ---- Private helpers ----

    private void persistTransaction(User user, Booking booking,
                                    String type, float amount, String currency,
                                    String status, String stripeReference) {
        Transaction tx = Transaction.builder()
                .user(user)
                .booking(booking)
                .type(type)
                .amount(amount)
                .currency(currency)
                .status(status)
                .stripeReference(stripeReference)
                .build();
        transactionRepository.save(tx);
    }
}
