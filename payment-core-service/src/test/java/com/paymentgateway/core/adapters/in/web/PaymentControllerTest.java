package com.paymentgateway.core.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.usecase.CreateTransactionUseCase;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(PaymentController.class)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateTransactionUseCase createTransactionUseCase;

    @Test
    void shouldCreatePaymentSuccessfully() throws Exception {
        UUID merchantId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest(merchantId, new BigDecimal("200.00"), "CREDIT_CARD");
        
        Transaction tx = new Transaction(merchantId, new BigDecimal("200.00"), "CREDIT_CARD");
        
        when(createTransactionUseCase.execute(any(CreateTransactionCommand.class))).thenReturn(tx);

        mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
