# Spec Delta: Payment Processor Pipeline

## Purpose

Enables background workers to consume payment events from RabbitMQ, transition payment statuses, generate receipts to AWS S3, and trigger merchant webhook notifications with automatic retries.

## ADDED Requirements

### Requirement: Asynchronous Payment Processing
The processor worker SHALL consume messages from the `payment.created` queue and evaluate payment processing according to business rules.

#### Scenario: Payment successfully processed and approved
- **WHEN** a valid payment message is consumed from `payment.created`
- **THEN** the transaction status in the database is updated to APPROVED, a receipt is generated, and a webhook notification is dispatched

#### Scenario: Unrecoverable failure routed to Dead Letter Queue
- **WHEN** message processing encounters an unrecoverable error after max retry attempts
- **THEN** the message is acknowledged with rejection and routed to the `payment.dlq` Dead Letter Queue

### Requirement: S3 Receipt Generation
The processor worker SHALL generate a digital receipt and upload it to the configured AWS S3 bucket for every approved transaction.

#### Scenario: Receipt upload upon approval
- **WHEN** a payment is marked as APPROVED
- **THEN** a receipt JSON/PDF object is uploaded to the S3 bucket and the receipt URL or S3 key is recorded

### Requirement: Merchant Webhook Notification with Retries
The processor worker SHALL send an HTTP POST webhook notification to the merchant endpoint upon transaction completion, with up to 3 automatic retries in case of failure.

#### Scenario: Webhook sent successfully
- **WHEN** payment processing completes for a merchant with a registered webhook URL
- **THEN** an HTTP POST payload is delivered to the merchant and a successful log entry is recorded in `webhook_logs`

#### Scenario: Webhook retry on transient failure
- **WHEN** the merchant webhook endpoint returns a 5xx HTTP status code or connection timeout
- **THEN** up to 3 delivery attempts are executed before logging a delivery failure
