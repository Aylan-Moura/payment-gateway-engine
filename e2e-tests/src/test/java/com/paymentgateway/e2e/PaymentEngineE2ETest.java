package com.paymentgateway.e2e;

import com.paymentgateway.core.PaymentCoreApplication;
import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.worker.ProcessorWorkerApplication;
import com.paymentgateway.worker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    classes = {PaymentCoreApplication.class, ProcessorWorkerApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class PaymentEngineE2ETest {

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("payment_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.12-management-alpine"));

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.3"))
            .withServices(LocalStackContainer.Service.S3);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("aws.s3.bucket", LOCALSTACK::getBucketName);
        registry.add("aws.endpoint-url", LOCALSTACK::getEndpointOverride);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void contextLoadsAndContainersAreRunning() {
        assertTrue(POSTGRESQL.isRunning());
        assertTrue(RABBITMQ.isRunning());
        assertTrue(LOCALSTACK.isRunning());
    }

    @Test
    void shouldProcessPaymentEndToEnd() {
        UUID merchantId = UUID.randomUUID();
        
        // Step 1: Create payment via REST API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = String.format(
            "{\"merchantId\":\"%s\",\"amount\":150.00,\"paymentMethod\":\"PIX\"}", 
            merchantId
        );
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/payments", 
            HttpMethod.POST, 
            request, 
            String.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());

        // Extract transaction ID from response
        Transaction createdTransaction = restTemplate.getForObject(
            "/api/v1/payments/" + extractTransactionId(response.getBody()), 
            Transaction.class
        );
        assertNotNull(createdTransaction);
        assertEquals("PENDING", createdTransaction.getStatus());

        // Step 2: Wait for worker to process the message and update status
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            Transaction updated = transactionRepository.findById(createdTransaction.getId()).orElse(null);
            assertNotNull(updated);
            assertEquals("APPROVED", updated.getStatus());
        });
    }

    private UUID extractTransactionId(String body) {
        // Simple extraction assuming JSON format {"id":"uuid",...}
        int start = body.indexOf("\"id\":\"") + 6;
        int end = body.indexOf("\"", start);
        return UUID.fromString(body.substring(start, end));
    }
}