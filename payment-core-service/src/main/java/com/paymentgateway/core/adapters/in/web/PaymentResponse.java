package com.paymentgateway.core.adapters.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resposta da criação de uma transação de pagamento")
public class PaymentResponse {
    @Schema(description = "ID da transação criada", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID transactionId;

    @Schema(description = "Status da transação", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
    private String status;
}