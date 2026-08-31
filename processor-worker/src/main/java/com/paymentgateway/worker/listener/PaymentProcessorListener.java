package com.paymentgateway.worker.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.worker.service.PaymentProcessorService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessorListener {

    private final PaymentProcessorService service;
    private final ObjectMapper objectMapper;

    public PaymentProcessorListener(PaymentProcessorService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "payment.created")
    public void receiveMessage(String message) {
        try {
            PaymentMessage paymentMessage = objectMapper.readValue(message, PaymentMessage.class);
            service.process(paymentMessage);
        } catch (Exception e) {
            throw new RuntimeException("Error processing message", e); 
        }
    }
}
