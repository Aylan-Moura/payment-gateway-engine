package com.paymentgateway.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RateLimitIntegrationTest {

    private static final int BURST_CAPACITY = 10;

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private WebTestClient client;

    @Test
    void shouldAllowBurstAndThenRejectRequestsPerApiKey() {
        for (int i = 0; i < BURST_CAPACITY; i++) {
            client.get().uri("/test/rate-limited")
                    .header("X-API-Key", "store-key-123")
                    .exchange()
                    .expectStatus().isOk();
        }

        client.get().uri("/test/rate-limited")
                .header("X-API-Key", "store-key-123")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @TestConfiguration
    static class GatewayTestConfig {

        @Bean
        public RouteLocator rateLimitedRoute(RouteLocatorBuilder builder, RedisRateLimiter rateLimiter) {
            KeyResolver keyResolver = exchange -> Mono.justOrEmpty(
                            exchange.getRequest().getHeaders().getFirst("X-API-Key"))
                    .switchIfEmpty(Mono.just("anonymous"));

            return builder.routes()
                    .route("rate-limited-test", r -> r.path("/test/rate-limited")
                            .filters(f -> f
                                    .requestRateLimiter(config -> {
                                        config.setRateLimiter(rateLimiter);
                                        config.setKeyResolver(keyResolver);
                                    })
                                    .filter((exchange, chain) -> {
                                        exchange.getResponse().setStatusCode(HttpStatus.OK);
                                        return exchange.getResponse().setComplete();
                                    }))
                            .uri("no://op"))
                    .build();
        }
    }
}