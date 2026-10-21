CREATE TABLE webhook_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID,
    status VARCHAR(50),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt TIMESTAMP
);
