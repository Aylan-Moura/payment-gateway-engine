package com.paymentgateway.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String ANONYMOUS_API_KEY = "anonymous";

    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER))
                .switchIfEmpty(Mono.just(ANONYMOUS_API_KEY));
    }

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 10);
    }
}