package com.paymentgateway.core.adapters.out.messaging;

import com.paymentgateway.core.domain.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @Test
    void shouldPublishPaymentEventSuccessfully() {
        PublishPaymentEventAdapter adapter = new PublishPaymentEventAdapter(rabbitTemplate, "payment.exchange", "payment.created");
        Transaction tx = new Transaction(UUID.randomUUID(), new BigDecimal("250.00"), "CREDIT_CARD");

        adapter.publish(tx);

        ArgumentCaptor<PaymentEventMessage> captor = ArgumentCaptor.forClass(PaymentEventMessage.class);
        verify(rabbitTemplate).convertAndSend(eq("payment.exchange"), eq("payment.created"), captor.capture());

        PaymentEventMessage sent = captor.getValue();
        assertEquals(tx.getId(), sent.getTransactionId());
        assertEquals(tx.getMerchantId(), sent.getMerchantId());
        assertEquals(tx.getAmount(), sent.getAmount());
        assertEquals("CREDIT_CARD", sent.getPaymentMethod());
        assertEquals("PENDING", sent.getStatus());
    }
}