package com.paymentgateway.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Solicitação de renovação do token de acesso")
public class RefreshRequest {
    @Schema(description = "Token JWT de refresh", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}