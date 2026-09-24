# Proposal: Complete Missing Engine Features and End-to-End Flow

## Why

The Payment Gateway Engine has all microservices, API Gateway routing, and infrastructure containers configured, but critical internal adapters in `payment-core-service` (database persistence and RabbitMQ publishing) and the core execution logic in `processor-worker` (transaction completion, S3 receipt upload, and webhook dispatch) remain unimplemented stubs. Connecting these remaining adapters and implementing a comprehensive Testcontainers-based E2E test suite is essential to deliver a fully functional, production-ready payment processing pipeline.

## What Changes

- Implement real JPA persistence and Flyway migrations in `payment-core-service` for `SaveTransactionPort`.
- Implement RabbitMQ message publisher in `payment-core-service` for `PublishPaymentEventPort`.
- Connect the message processing pipeline in `processor-worker` to update transaction records, generate receipts in S3, and trigger merchant webhooks with retry.
- Implement comprehensive end-to-end integration tests using Testcontainers in `e2e-tests` to validate the full transaction lifecycle from API Gateway down to S3 receipt and webhook delivery.
- Add Grafana dashboard provisioning for payment performance, throughput, and DLQ monitoring.

## Capabilities

### New Capabilities
- `payment-core-adapters`: Implementation of outgoing persistence (`SaveTransactionPort`) via PostgreSQL/Spring Data JPA and event publishing (`PublishPaymentEventPort`) via RabbitMQ.
- `payment-processor-pipeline`: End-to-end execution of received payment events in `processor-worker`, managing state transitions, S3 receipt generation, and webhook notifications.
- `e2e-payment-verification`: Multi-container end-to-end integration tests verifying complete payment workflows across Gateway, Core, Worker, Postgres, RabbitMQ, and S3.

### Modified Capabilities
<!-- No existing capabilities to modify -->

## Impact

- `payment-core-service`: Added dependencies for Spring Data JPA and Flyway migrations; added outbound persistence and messaging adapter classes.
- `processor-worker`: Integrated `PaymentProcessorService` with repository updates, `S3ReceiptService`, and `WebhookService`.
- `e2e-tests`: Expanded `PaymentEngineE2ETest` to cover live end-to-end transaction flows.
- Infrastructure: Added Grafana dashboard provision configs for visualization.
