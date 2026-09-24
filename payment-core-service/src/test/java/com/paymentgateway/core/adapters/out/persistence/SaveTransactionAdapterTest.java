package com.paymentgateway.core.adapters.out.persistence;

import com.paymentgateway.core.domain.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(SaveTransactionAdapter.class)
@ActiveProfiles("test")
public class SaveTransactionAdapterTest {

    @Autowired
    private SaveTransactionAdapter adapter;

    @Autowired
    private TransactionRepository repository;

    @Test
    void shouldSaveTransactionSuccessfully() {
        Transaction tx = new Transaction(UUID.randomUUID(), new BigDecimal("100.00"), "PIX");
        adapter.save(tx);

        Optional<TransactionEntity> found = repository.findById(tx.getId());
        assertTrue(found.isPresent());
        assertEquals(tx.getAmount(), found.get().getAmount());
        assertEquals(tx.getStatus(), found.get().getStatus());
        assertEquals(tx.getMethod(), found.get().getMethod());
    }
}