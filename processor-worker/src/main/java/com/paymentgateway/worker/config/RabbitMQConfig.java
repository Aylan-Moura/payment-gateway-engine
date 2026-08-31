package com.paymentgateway.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "payment.exchange";
    public static final String DLQ = "payment.dlq";
    public static final String DLQ_EXCHANGE = "payment.dlx";
    public static final String QUEUE_CREATED = "payment.created";
    
    @Bean
    Queue dlq() {
        return QueueBuilder.durable(DLQ).build();
    }
    
    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }
    
    @Bean
    Binding dlqBinding() {
        return BindingBuilder.bind(dlq()).to(deadLetterExchange()).with(DLQ);
    }
    
    @Bean
    Queue paymentCreatedQueue() {
        return QueueBuilder.durable(QUEUE_CREATED)
            .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLQ)
            .build();
    }
    
    @Bean
    DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }
    
    @Bean
    Binding binding() {
        return BindingBuilder.bind(paymentCreatedQueue()).to(exchange()).with("payment.created.routing");
    }
}
