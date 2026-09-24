# Tasks

## 1. Payment Core Infrastructure

- [x] 1.1 Implement Flyway migration `V2__create_transactions_table.sql` and verify database schema update
- [x] 1.2 Implement Spring Data JPA `TransactionRepository` and wire it into `SaveTransactionPort` implementation
- [x] 1.3 Implement `PublishPaymentEventPort` using `RabbitTemplate` and verify successful event publishing in integration tests

## 2. Processor Worker Logic

- [x] 2.1 Implement `PaymentProcessorService` logic to update transaction status to APPROVED in database
- [x] 2.2 Implement `S3ReceiptService` to upload receipt objects to the configured S3 bucket
- [x] 2.3 Implement `WebhookService` with `@Retryable` logic and verify successful webhook delivery to a mock endpoint
- [x] 2.4 Update `PaymentProcessorListener` to orchestrate status update, receipt generation, and webhook dispatch

## 3. End-to-End Integration & Observability

- [x] 3.1 Implement Testcontainers setup in `e2e-tests` for the full pipeline (Postgres, RabbitMQ, S3)
- [x] 3.2 Implement E2E payment lifecycle test and verify all steps succeed via REST and RabbitMQ message flows
- [x] 3.3 Provision Grafana dashboard with metrics for payment success/failure and throughput
- [x] 3.4 Verify E2E test suite runs successfully in CI pipeline
