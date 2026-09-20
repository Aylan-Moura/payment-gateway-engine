package com.paymentgateway.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

class ApiKeyResolverTest {

    private KeyResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new RateLimitConfig().apiKeyResolver();
    }

    @Test
    void shouldResolveApiKeyFromXApiKeyHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/payments")
                        .header(RateLimitConfig.API_KEY_HEADER, "store-key-123")
                        .build());

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("store-key-123")
                .verifyComplete();
    }

    @Test
    void shouldResolveAnonymousKeyWhenHeaderIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, "/api/v1/payments").build());

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext(RateLimitConfig.ANONYMOUS_API_KEY)
                .verifyComplete();
    }
}