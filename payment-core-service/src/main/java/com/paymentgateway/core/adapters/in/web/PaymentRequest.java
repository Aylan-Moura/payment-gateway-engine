package com.paymentgateway.core.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pedido de criação de uma transação de pagamento")
public class PaymentRequest {
    @Schema(description = "ID do merchant", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID merchantId;

    @Schema(description = "Valor do pagamento", example = "200.00")
    private BigDecimal amount;

    @Schema(description = "Método de pagamento: PIX ou CREDIT_CARD", example = "PIX", allowableValues = {"PIX", "CREDIT_CARD"})
    private String paymentMethod;
}