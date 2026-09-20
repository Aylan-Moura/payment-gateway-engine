package com.paymentgateway.core.adapters.in.web;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.usecase.CreateTransactionUseCase;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Pagamentos", description = "Criação de transações de pagamento (PIX e Cartão)")
public class PaymentController {

    private final CreateTransactionUseCase useCase;

    public PaymentController(CreateTransactionUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "Cria uma nova transação de pagamento",
            description = "Recebe o pedido de pagamento e publica o evento para o processor-worker. Requer token Bearer JWT.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transação criada com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido (10 requisições por segundo por API Key)")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestBody PaymentRequest request) {
        CreateTransactionCommand cmd = new CreateTransactionCommand(
            request.getMerchantId(), request.getAmount(), request.getPaymentMethod()
        );
        Transaction tx = useCase.execute(cmd);
        PaymentResponse response = new PaymentResponse(tx.getId(), tx.getStatus().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}