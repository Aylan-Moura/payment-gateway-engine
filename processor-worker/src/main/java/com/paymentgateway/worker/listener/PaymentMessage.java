package com.paymentgateway.worker.listener;

import lombok.Data;
import java.util.UUID;
import java.math.BigDecimal;

@Data
public class PaymentMessage {
    private UUID transactionId;
    private UUID merchantId;
    private BigDecimal amount;
    private String paymentMethod;
    private String status;
    private String webhookUrl;
}
