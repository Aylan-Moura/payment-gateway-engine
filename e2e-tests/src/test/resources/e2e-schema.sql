-- Schema usado apenas pelo teste E2E combinado (payment-core + worker).
-- Em producao cada servico aplica suas proprias migracoes Flyway.
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    merchant_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webhook_log (
    id BIGSERIAL PRIMARY KEY,
    transaction_id UUID,
    status VARCHAR(50),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt TIMESTAMP
);
