package com.paymentgateway.core.adapters.out.persistence;

import com.paymentgateway.core.domain.MerchantAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@JdbcTest
@Import(MerchantAccountAdapter.class)
public class MerchantAccountAdapterTest {

    @Autowired
    private MerchantAccountAdapter adapter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID merchantId;
    private String email;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS merchants");
        jdbcTemplate.execute("""
                CREATE TABLE merchants (
                    id UUID PRIMARY KEY,
                    company_name VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    api_key VARCHAR(255) NOT NULL UNIQUE,
                    webhook_url VARCHAR(500),
                    created_at TIMESTAMP NOT NULL
                )
                """);

        merchantId = UUID.randomUUID();
        email = "merchant@example.com";
        jdbcTemplate.update("""
                        INSERT INTO merchants (id, company_name, email, password_hash, api_key, webhook_url, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                merchantId, "Merchant", email, "hash", "api-key", "https://loja.exemplo.com/webhooks",
                LocalDateTime.now());
    }

    @Test
    void shouldFindMerchantByEmail() {
        Optional<MerchantAccount> found = adapter.findByEmail(email);

        assertTrue(found.isPresent());
        assertEquals(merchantId, found.get().id());
        assertEquals("api-key", found.get().apiKey());
        assertEquals("https://loja.exemplo.com/webhooks", found.get().webhookUrl());
    }

    @Test
    void shouldFindMerchantById() {
        assertTrue(adapter.findById(merchantId).isPresent());
    }

    @Test
    void shouldReturnEmptyForUnknownMerchant() {
        assertTrue(adapter.findByEmail("unknown@example.com").isEmpty());
        assertTrue(adapter.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankOrNullInput() {
        assertTrue(adapter.findByEmail(null).isEmpty());
        assertTrue(adapter.findByEmail("  ").isEmpty());
        assertTrue(adapter.findById(null).isEmpty());
    }
}
