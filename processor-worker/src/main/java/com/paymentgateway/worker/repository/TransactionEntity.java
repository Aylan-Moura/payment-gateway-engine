package com.paymentgateway.worker.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Data
public class TransactionEntity {
    @Id
    private UUID id;
    private BigDecimal amount;
    private String status;
}