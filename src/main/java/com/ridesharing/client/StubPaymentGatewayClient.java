package com.ridesharing.client;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Stub implementation of {@link PaymentGatewayClient} that returns fake IDs.
 * Used in development / testing when no real payment gateway is configured.
 */
@Component
public class StubPaymentGatewayClient implements PaymentGatewayClient {

    @Override
    public String createHold(double amount, String currency) {
        return "stub_pi_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void captureHold(String paymentIntentId) {
        // no-op for stub
    }

    @Override
    public String refund(String paymentIntentId, double amount) {
        return "stub_re_" + UUID.randomUUID().toString().replace("-", "");
    }
}
