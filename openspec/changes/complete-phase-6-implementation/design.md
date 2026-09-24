# Design: Complete Missing Engine Features and End-to-End Flow

## Context

The microservices architecture is established (API Gateway, Auth Service, Payment Core Service, Processor Worker) with shared PostgreSQL, Redis, and RabbitMQ infrastructure (see proposal.md). However, outbound ports in `payment-core-service` and message processing logic in `processor-worker` are currently stubbed out, and E2E integration tests only test context loading.

## Goals / Non-Goals

**Goals:**
- Implement clean architecture persistence and messaging adapters for `payment-core-service`.
- Connect `processor-worker` to handle transaction updates, S3 receipt generation, and HTTP webhook retries.
- Implement robust Testcontainers E2E tests validating the full transaction lifecycle.
- Provide Grafana visualization dashboards.

**Non-Goals:**
- Implementing multi-region active-active database replication.
- Adding third-party real payment gateway integrations (Stripe/Pix APIs) beyond the simulated engine.

## Decisions

### 1. Spring Data JPA for Persistence in Core Service
- **Decision:** Implement `SaveTransactionPort` using Spring Data JPA repository backed by PostgreSQL.
- **Rationale:** Aligns with existing stack (`auth-service` uses Spring Data JPA / Flyway) and provides clean separation within the Clean Architecture outer layer.
- **Alternatives Considered:** Raw JDBC or R2DBC (rejected due to standard JPA familiarity and consistency with auth-service).

### 2. Spring AMQP RabbitTemplate for Event Publishing
- **Decision:** Implement `PublishPaymentEventPort` using Spring's `RabbitTemplate` to publish serialized JSON events to `payment.created`.
- **Rationale:** Reliable, declarative messaging integration native to Spring Boot.

### 3. Testcontainers for E2E Integration Testing
- **Decision:** Use Testcontainers JUnit 5 extension to spin up ephemeral containers for PostgreSQL, RabbitMQ, and LocalStack (S3) during integration tests.
- **Rationale:** Guarantees production-parity environment in CI/CD without external service dependencies.

## Risks / Trade-offs

- **[Risk]** Testcontainers startup time in CI environment.
  - **Mitigation:** Utilize image caching and parallel test execution settings in Maven Surefire.
- **[Risk]** Network latency or race conditions between worker message consumption and core persistence.
  - **Mitigation:** Implement idempotent status transitions and retry mechanisms.
