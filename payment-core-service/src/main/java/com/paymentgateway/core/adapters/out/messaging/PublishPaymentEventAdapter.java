package com.paymentgateway.core.adapters.out.messaging;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.domain.TransactionStatus;
import com.paymentgateway.core.usecase.port.out.PublishPaymentEventPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublishPaymentEventAdapter implements PublishPaymentEventPort {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public PublishPaymentEventAdapter(RabbitTemplate rabbitTemplate,
                                      @Value("${app.rabbitmq.exchange:payment.exchange}") String exchange,
                                      @Value("${app.rabbitmq.routing-key:payment.created}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
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
                transaction.getStatus().name()
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}