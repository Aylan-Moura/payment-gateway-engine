package com.paymentgateway.core.usecase.port.in;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateTransactionCommand {
    private UUID merchantId;
    private BigDecimal amount;
    private String paymentMethod;
}
