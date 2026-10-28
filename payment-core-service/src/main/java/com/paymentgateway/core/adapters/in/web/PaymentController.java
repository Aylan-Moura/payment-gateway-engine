package com.paymentgateway.core.adapters.in.web;

import com.paymentgateway.core.domain.MerchantAccount;
import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.exception.ForbiddenException;
import com.paymentgateway.core.exception.UnauthorizedException;
import com.paymentgateway.core.usecase.CreateTransactionUseCase;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import com.paymentgateway.core.usecase.port.out.MerchantAccountPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Pagamentos", description = "Criação de transações de pagamento (PIX e Cartão)")
public class PaymentController {

    private final CreateTransactionUseCase useCase;
    private final MerchantAccountPort merchantAccountPort;

    public PaymentController(CreateTransactionUseCase useCase, MerchantAccountPort merchantAccountPort) {
        this.useCase = useCase;
        this.merchantAccountPort = merchantAccountPort;
    }

    @Operation(summary = "Cria uma nova transação de pagamento",
            description = "Requer token Bearer JWT e o header X-API-Key do mesmo merchant. "
                    + "O merchantId do corpo, quando informado, precisa pertencer ao merchant autenticado.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transação criada com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "API key inválida ou merchantId de outro merchant"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido (10 requisições por segundo por API Key)")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request,
                                                         Authentication authentication,
                                                         @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        MerchantAccount merchant = merchantAccountPort.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Merchant not found"));

        if (!apiKeysMatch(apiKey, merchant.apiKey())) {
            throw new ForbiddenException("Invalid API key");
        }

        UUID merchantId = request.getMerchantId() == null ? merchant.id() : request.getMerchantId();
        if (!merchant.id().equals(merchantId)) {
            throw new ForbiddenException("merchantId does not belong to the authenticated merchant");
        }

        CreateTransactionCommand cmd = new CreateTransactionCommand(
            merchantId, request.getAmount(), request.getPaymentMethod()
        );
        Transaction tx = useCase.execute(cmd);
        PaymentResponse response = new PaymentResponse(tx.getId(), tx.getStatus().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private boolean apiKeysMatch(String provided, String expected) {
        if (provided == null || provided.isBlank() || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
