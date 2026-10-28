package com.paymentgateway.core.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.core.config.SecurityConfig;
import com.paymentgateway.core.domain.MerchantAccount;
import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.security.JwtAuthenticationFilter;
import com.paymentgateway.core.security.JwtVerifier;
import com.paymentgateway.core.usecase.CreateTransactionUseCase;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import com.paymentgateway.core.usecase.port.out.MerchantAccountPort;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
public class PaymentControllerTest {

    private static final String EMAIL = "merchant@test.com";
    private static final String API_KEY = "api-key-123";
    private static final String TOKEN = "valid-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateTransactionUseCase createTransactionUseCase;

    @MockBean
    private MerchantAccountPort merchantAccountPort;

    @MockBean
    private JwtVerifier jwtVerifier;

    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        when(jwtVerifier.extractSubject(TOKEN)).thenReturn(EMAIL);
        when(merchantAccountPort.findByEmail(EMAIL))
                .thenReturn(Optional.of(new MerchantAccount(merchantId, API_KEY, null)));
    }

    @Test
    void shouldCreatePaymentSuccessfully() throws Exception {
        PaymentRequest request = new PaymentRequest(merchantId, new BigDecimal("200.00"), "CREDIT_CARD");
        Transaction tx = new Transaction(merchantId, new BigDecimal("200.00"), "CREDIT_CARD");
        when(createTransactionUseCase.execute(any(CreateTransactionCommand.class))).thenReturn(tx);

        mockMvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldAcceptRequestWithoutMerchantIdAndUseAuthenticatedMerchant() throws Exception {
        PaymentRequest request = new PaymentRequest(null, new BigDecimal("50.00"), "PIX");
        Transaction tx = new Transaction(merchantId, new BigDecimal("50.00"), "PIX");
        when(createTransactionUseCase.execute(any(CreateTransactionCommand.class))).thenReturn(tx);

        mockMvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectRequestWithoutToken() throws Exception {
        PaymentRequest request = new PaymentRequest(merchantId, new BigDecimal("10.00"), "PIX");

        mockMvc.perform(post("/api/v1/payments")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInvalidToken() throws Exception {
        when(jwtVerifier.extractSubject("bad-token")).thenThrow(new JwtException("invalid"));
        PaymentRequest request = new PaymentRequest(merchantId, new BigDecimal("10.00"), "PIX");

        mockMvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectWrongApiKey() throws Exception {
        PaymentRequest request = new PaymentRequest(merchantId, new BigDecimal("10.00"), "PIX");

        mockMvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .header("X-API-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectMerchantIdFromAnotherMerchant() throws Exception {
        PaymentRequest request = new PaymentRequest(UUID.randomUUID(), new BigDecimal("10.00"), "PIX");

        mockMvc.perform(post("/api/v1/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN)
                        .header("X-API-Key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
