package com.paymentgateway.core.adapters.out.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentEventMessage {
    private UUID transactionId;
    private UUID merchantId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;

    public PaymentEventMessage() {}

    public PaymentEventMessage(UUID transactionId, UUID merchantId, BigDecimal amount, String paymentMethod, String status) {
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public UUID getTransactionId() { return transactionId; }
    public UUID getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
}