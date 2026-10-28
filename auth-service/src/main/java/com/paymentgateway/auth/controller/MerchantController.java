package com.paymentgateway.auth.controller;

import com.paymentgateway.auth.dto.UpdateWebhookRequest;
import com.paymentgateway.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/merchants")
@Tag(name = "Merchants", description = "Configuracoes do merchant autenticado")
public class MerchantController {

    private final AuthService authService;

    public MerchantController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Atualiza a URL de webhook do merchant autenticado",
            description = "Requer token Bearer JWT. Envie webhookUrl vazio para remover a configuracao.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook atualizado"),
            @ApiResponse(responseCode = "400", description = "URL invalida"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/me/webhook")
    public ResponseEntity<Map<String, String>> updateWebhook(@RequestBody UpdateWebhookRequest request,
                                                             Authentication authentication) {
        String webhookUrl = authService.updateWebhookUrl(authentication.getName(), request.getWebhookUrl());
        return ResponseEntity.ok(Collections.singletonMap("webhookUrl", webhookUrl));
    }
}
