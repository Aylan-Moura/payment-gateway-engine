package com.paymentgateway.core.domain;

public enum PaymentMethod {
    PIX,
    CREDIT_CARD;

    public static PaymentMethod fromString(String method) {
        for (PaymentMethod pm : PaymentMethod.values()) {
            if (pm.name().equalsIgnoreCase(method)) {
                return pm;
            }
        }
        throw new IllegalArgumentException("Invalid payment method: " + method);
    }
}
