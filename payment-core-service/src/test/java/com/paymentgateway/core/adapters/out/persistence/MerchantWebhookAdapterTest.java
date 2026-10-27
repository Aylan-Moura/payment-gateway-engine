package com.paymentgateway.core.adapters.out.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MerchantWebhookAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MerchantWebhookAdapter adapter;

    @Test
    void shouldReturnMerchantWebhookUrl() {
        UUID merchantId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any()))
                .thenReturn(List.of("https://loja.exemplo.com/webhooks"));

        assertEquals("https://loja.exemplo.com/webhooks", adapter.findWebhookUrl(merchantId));
    }

    @Test
    void shouldReturnNullWhenMerchantHasNoWebhook() {
        UUID merchantId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any()))
                .thenReturn(List.of());

        assertNull(adapter.findWebhookUrl(merchantId));
    }

    @Test
    void shouldReturnNullWhenDatabaseIsUnavailable() {
        UUID merchantId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any()))
                .thenThrow(new DataAccessResourceFailureException("merchants table not available"));

        assertNull(adapter.findWebhookUrl(merchantId));
    }

    @Test
    void shouldReturnNullForNullOrBlankValues() {
        UUID merchantId = UUID.randomUUID();
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any()))
                .thenReturn(List.of("  "));

        assertNull(adapter.findWebhookUrl(merchantId));
    }
}
