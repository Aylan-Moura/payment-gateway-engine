package com.paymentgateway.worker.listener;

import com.paymentgateway.worker.service.PaymentProcessorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentProcessorListenerTest {

    @Mock
    private PaymentProcessorService paymentProcessorService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentProcessorListener listener;

    @Test
    void shouldProcessPaymentSuccessfully() throws Exception {
        String message = "{\"transactionId\":\"" + UUID.randomUUID() + "\",\"amount\":100.00,\"status\":\"PENDING\"}";
        PaymentMessage pm = new PaymentMessage();
        
        when(objectMapper.readValue(message, PaymentMessage.class)).thenReturn(pm);
        
        listener.receiveMessage(message);
        
        verify(paymentProcessorService, times(1)).process(pm);
    }

    @Test
    void shouldThrowExceptionWhenProcessingFails() throws Exception {
        String message = "{\"transactionId\":\"" + UUID.randomUUID() + "\",\"amount\":100.00,\"status\":\"PENDING\"}";
        PaymentMessage pm = new PaymentMessage();
        
        when(objectMapper.readValue(message, PaymentMessage.class)).thenReturn(pm);
        doThrow(new RuntimeException("Simulated processing failure"))
            .when(paymentProcessorService).process(pm);
            
        assertThrows(RuntimeException.class, () -> listener.receiveMessage(message));
    }
}
