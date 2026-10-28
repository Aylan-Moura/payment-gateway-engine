package com.paymentgateway.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Atualizacao da URL de webhook do merchant autenticado")
public class UpdateWebhookRequest {

    @Schema(description = "Nova URL de webhook. Envie vazio ou null para remover.",
            example = "https://loja.exemplo.com/webhooks/payments")
    private String webhookUrl;
}
