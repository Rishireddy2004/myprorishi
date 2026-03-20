package com.ridesharing.client;

/**
 * Abstraction over a payment gateway (e.g. Stripe, Razorpay).
 * Implementations may be real or stub.
 */
public interface PaymentGatewayClient {

    /**
     * Creates a payment hold (authorization) for the given amount.
     *
     * @param amount   amount to hold
     * @param currency ISO-4217 currency code (e.g. "INR", "USD")
     * @return paymentIntentId returned by the gateway
     */
    String createHold(double amount, String currency);

    /**
     * Captures a previously created hold.
     *
     * @param paymentIntentId the ID returned by {@link #createHold}
     */
    void captureHold(String paymentIntentId);

    /**
     * Issues a (partial or full) refund against a payment intent.
     *
     * @param paymentIntentId the original payment intent
     * @param amount          amount to refund
     * @return refundId returned by the gateway
     */
    String refund(String paymentIntentId, double amount);
}
