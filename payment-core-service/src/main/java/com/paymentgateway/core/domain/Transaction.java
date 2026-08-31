package com.paymentgateway.core.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class Transaction {
    private UUID id;
    private UUID merchantId;
    private BigDecimal amount;
    private TransactionStatus status;
    private PaymentMethod method;

    public Transaction(UUID merchantId, BigDecimal amount, String methodStr) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        this.id = UUID.randomUUID();
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = TransactionStatus.PENDING;
        this.method = PaymentMethod.fromString(methodStr);
    }

    public UUID getId() { return id; }
    public UUID getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public PaymentMethod getMethod() { return method; }
    
    public void approve() {
        this.status = TransactionStatus.APPROVED;
    }
    
    public void reject() {
        this.status = TransactionStatus.REJECTED;
    }
}
