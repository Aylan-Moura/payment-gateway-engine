package com.paymentgateway.core.adapters.in.web;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.usecase.CreateTransactionUseCase;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final CreateTransactionUseCase useCase;

    public PaymentController(CreateTransactionUseCase useCase) {
        this.useCase = useCase;
    }

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
