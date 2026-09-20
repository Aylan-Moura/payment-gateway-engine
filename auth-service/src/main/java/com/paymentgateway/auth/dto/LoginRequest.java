package com.paymentgateway.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Credenciais de login do merchant")
public class LoginRequest {
    @Schema(description = "Email do merchant", example = "loja@exemplo.com")
    private String email;

    @Schema(description = "Senha do merchant", example = "senha-segura")
    private String password;
}