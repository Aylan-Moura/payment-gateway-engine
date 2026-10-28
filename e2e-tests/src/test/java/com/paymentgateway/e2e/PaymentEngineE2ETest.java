package com.paymentgateway.e2e;

import com.paymentgateway.core.PaymentCoreApplication;
import com.paymentgateway.worker.ProcessorWorkerApplication;
import com.paymentgateway.worker.repository.TransactionEntity;
import com.paymentgateway.worker.repository.WorkerTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
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

    static {
        // DefaultCredentialsProvider (usado pelo AwsConfig) exige credenciais mesmo no LocalStack.
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
    }

    @Container
    static final PostgreSQLContainer<?> POSTGRESQL = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("payment_db")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("e2e-schema.sql");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.1-management-alpine"));

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.3"))
            .withServices(LocalStackContainer.Service.S3);

    @BeforeAll
    static void createReceiptBucket() {
        try (S3Client s3 = S3Client.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test", "test")))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build()) {
            s3.createBucket(bucket -> bucket.bucket("payment-receipts"));
        } catch (Exception e) {
            String message = String.valueOf(e.getMessage());
            if (!message.contains("BucketAlreadyOwnedByYou") && !message.contains("BucketAlreadyExists")) {
                throw e;
            }
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRESQL::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRESQL::getUsername);
        registry.add("spring.datasource.password", POSTGRESQL::getPassword);
        // No contexto combinado os dois servicos trazem migracoes V2 diferentes;
        // o schema e criado pelo init script do container.
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
        registry.add("aws.s3.bucket", () -> "payment-receipts");
        registry.add("aws.s3.path-style-access", () -> "true");
        registry.add("aws.s3.endpoint-url", () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WorkerTransactionRepository transactionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void contextLoadsAndContainersAreRunning() {
        assertTrue(POSTGRESQL.isRunning());
        assertTrue(RABBITMQ.isRunning());
        assertTrue(LOCALSTACK.isRunning());
    }

    @Test
    void shouldProcessPaymentEndToEnd() throws Exception {
        String email = "e2e-" + UUID.randomUUID() + "@exemplo.com";
        String apiKey = UUID.randomUUID().toString();
        UUID merchantId = createMerchant(email, apiKey);

        // Step 1: Create payment via REST API (authenticated as the merchant)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(tokenFor(email));
        headers.set("X-API-Key", apiKey);
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
        UUID transactionId = extractTransactionId(response.getBody());
        assertNotNull(transactionId);

        TransactionEntity initial = transactionRepository.findById(transactionId).orElse(null);
        assertNotNull(initial);
        // O worker pode processar muito rapido; aceitamos PENDING ou ja APPROVED aqui.
        assertTrue(List.of("PENDING", "APPROVED").contains(initial.getStatus()));

        // Step 2: Wait for worker to process the message and update status
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            TransactionEntity updated = transactionRepository.findById(transactionId).orElse(null);
            assertNotNull(updated);
            assertEquals("APPROVED", updated.getStatus());
        });
    }

    @Test
    void shouldRejectPaymentWithoutToken() throws Exception {
        // HttpClient do JDK: o HttpURLConnection do TestRestTemplate nao le 401 em streaming.
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://localhost:" + port + "/api/v1/payments"))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        var response = java.net.http.HttpClient.newHttpClient()
                .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    private UUID createMerchant(String email, String apiKey) {
        UUID merchantId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO merchants (id, company_name, email, password_hash, api_key, webhook_url, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
            merchantId, "E2E Merchant", email, "not-used", apiKey, null, LocalDateTime.now());
        return merchantId;
    }

    private String tokenFor(String email) {
        return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 300_000))
            .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret)), SignatureAlgorithm.HS256)
            .compact();
    }

    private UUID extractTransactionId(String body) throws Exception {
        JsonNode node = new ObjectMapper().readTree(body);
        return UUID.fromString(node.get("transactionId").asText());
    }
}