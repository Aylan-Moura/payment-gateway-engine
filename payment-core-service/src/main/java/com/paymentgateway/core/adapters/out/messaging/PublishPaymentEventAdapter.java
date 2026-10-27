package com.paymentgateway.core.adapters.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.domain.TransactionStatus;
import com.paymentgateway.core.usecase.port.out.PublishPaymentEventPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublishPaymentEventAdapter implements PublishPaymentEventPort {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String exchange;
    private final String routingKey;

    public PublishPaymentEventAdapter(RabbitTemplate rabbitTemplate,
                                      ObjectMapper objectMapper,
                                      @Value("${app.rabbitmq.exchange:payment.exchange}") String exchange,
                                      @Value("${app.rabbitmq.routing-key:payment.created}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(Transaction transaction) {
        PaymentEventMessage message = new PaymentEventMessage(
                transaction.getId(),
                transaction.getMerchantId(),
                transaction.getAmount(),
                transaction.getMethod().name(),
                transaction.getStatus().name(),
                transaction.getWebhookUrl()
        );
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(exchange, routingKey, jsonMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize payment event", e);
        }
    }
}