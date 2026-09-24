# Spec Delta: End-to-End Payment Verification

## Purpose

Validates the end-to-end integration and data flow across all distributed services and backing infrastructure using Testcontainers.

## ADDED Requirements

### Requirement: Full Payment Lifecycle Test Coverage
The automated test suite SHALL verify the complete transaction path from initial merchant authentication to message consumption and final status update using Testcontainers.

#### Scenario: Complete payment lifecycle succeeds
- **WHEN** an authenticated merchant creates a payment request via the API Gateway
- **THEN** the request is rate-limited and routed to the Core Service, saved in PostgreSQL, published to RabbitMQ, processed by the Worker, stored with receipt in S3, and verified as APPROVED

#### Scenario: DLQ routing verified under worker failure
- **WHEN** an invalid or poisonous payment message arrives in RabbitMQ
- **THEN** the processor worker rejects the message after retry limits and the message resides in `payment.dlq`
