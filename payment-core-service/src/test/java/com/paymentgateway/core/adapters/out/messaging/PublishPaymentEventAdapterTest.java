package com.paymentgateway.core.adapters.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.core.domain.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class PublishPaymentEventAdapterTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPublishPaymentEventSuccessfully() throws Exception {
        PublishPaymentEventAdapter adapter = new PublishPaymentEventAdapter(rabbitTemplate, objectMapper, "payment.exchange", "payment.created");
        Transaction tx = new Transaction(UUID.randomUUID(), new BigDecimal("250.00"), "CREDIT_CARD");

        adapter.publish(tx);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq("payment.exchange"), eq("payment.created"), captor.capture());

        PaymentEventMessage sent = objectMapper.readValue(captor.getValue(), PaymentEventMessage.class);
        assertEquals(tx.getId(), sent.getTransactionId());
        assertEquals(tx.getMerchantId(), sent.getMerchantId());
        assertEquals(tx.getAmount(), sent.getAmount());
        assertEquals("CREDIT_CARD", sent.getPaymentMethod());
        assertEquals("PENDING", sent.getStatus());
    }
}