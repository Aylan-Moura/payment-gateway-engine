package com.paymentgateway.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Dados para registro de um novo merchant")
public class RegisterRequest {
    @Schema(description = "Nome da empresa", example = "Loja Exemplo LTDA")
    private String companyName;

    @Schema(description = "Email do merchant", example = "loja@exemplo.com")
    private String email;

    @Schema(description = "Senha do merchant", example = "senha-segura")
    private String password;

    @Schema(description = "URL de webhook que recebera as notificacoes de pagamento (opcional)",
            example = "https://loja.exemplo.com/webhooks/payments")
    private String webhookUrl;
}