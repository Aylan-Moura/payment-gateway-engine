package com.paymentgateway.core.adapters.out.persistence;

import com.paymentgateway.core.domain.PaymentMethod;
import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.domain.TransactionStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod method;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public TransactionEntity() {}

    public TransactionEntity(Transaction domain) {
        this.id = domain.getId();
        this.merchantId = domain.getMerchantId();
        this.amount = domain.getAmount();
        this.status = domain.getStatus();
        this.method = domain.getMethod();
    }

    public UUID getId() { return id; }
    public UUID getMerchantId() { return merchantId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionStatus getStatus() { return status; }
    public PaymentMethod getMethod() { return method; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
