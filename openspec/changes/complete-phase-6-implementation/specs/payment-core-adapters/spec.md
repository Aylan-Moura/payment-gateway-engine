# Spec Delta: Payment Core Outbound Adapters

## Purpose

Provides persistence and message publishing capabilities for transaction commands in the payment core service, persisting records into PostgreSQL and publishing events onto RabbitMQ.

## ADDED Requirements

### Requirement: Transaction Persistence
The payment core service SHALL persist all incoming transaction entities to the PostgreSQL database with initial PENDING status before attempting to publish any message to the broker.

#### Scenario: Transaction saved to database successfully
- **WHEN** a valid payment request is processed by the core use case
- **THEN** a transaction record is created and stored in the database with status PENDING and a unique UUID

#### Scenario: Transaction database failure rejects request
- **WHEN** the database is unreachable or fails to save the transaction record
- **THEN** an error is returned to the client and no event is published to RabbitMQ

### Requirement: Payment Event Publishing
The payment core service SHALL publish a payment created event containing the transaction details to the `payment.created` queue on RabbitMQ upon successful database persistence.

#### Scenario: Event published after database commit
- **WHEN** the transaction is successfully committed to the database
- **THEN** a payload with transaction ID, merchant ID, amount, and payment method is sent to the `payment.created` exchange/queue
