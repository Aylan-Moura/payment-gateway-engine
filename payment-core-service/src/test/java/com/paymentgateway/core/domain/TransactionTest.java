package com.paymentgateway.core.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public class TransactionTest {

    @Test
    void shouldCreateTransactionWithValidData() {
        Transaction tx = new Transaction(UUID.randomUUID(), new BigDecimal("100.50"), "PIX");
        
        assertNotNull(tx.getId());
        assertEquals(new BigDecimal("100.50"), tx.getAmount());
        assertEquals(TransactionStatus.PENDING, tx.getStatus());
        assertEquals(PaymentMethod.PIX, tx.getMethod());
    }

    @Test
    void shouldThrowExceptionWhenAmountIsZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Transaction(UUID.randomUUID(), BigDecimal.ZERO, "CREDIT_CARD")
        );
        
        assertThrows(IllegalArgumentException.class, () -> 
            new Transaction(UUID.randomUUID(), new BigDecimal("-10.00"), "PIX")
        );
    }

    @Test
    void shouldThrowExceptionWhenPaymentMethodIsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> 
            new Transaction(UUID.randomUUID(), new BigDecimal("50.00"), "BOLETO")
        );
    }
}
