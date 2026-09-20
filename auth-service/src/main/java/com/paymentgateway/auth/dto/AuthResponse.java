package com.paymentgateway.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(description = "Resposta com os tokens JWT de acesso, refresh e identificadores do merchant")
public class AuthResponse {
    @Schema(description = "Token JWT de acesso", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "Token JWT de refresh", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "ID do merchant (usado como merchantId nas transações)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID merchantId;

    @Schema(description = "API Key do merchant (header X-API-Key no api-gateway)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String apiKey;
}