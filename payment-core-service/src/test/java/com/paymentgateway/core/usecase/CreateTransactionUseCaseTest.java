package com.paymentgateway.core.usecase;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.domain.TransactionStatus;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import com.paymentgateway.core.usecase.port.out.MerchantWebhookPort;
import com.paymentgateway.core.usecase.port.out.PublishPaymentEventPort;
import com.paymentgateway.core.usecase.port.out.SaveTransactionPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateTransactionUseCaseTest {

    @Mock
    private SaveTransactionPort savePort;

    @Mock
    private PublishPaymentEventPort publishPort;

    @Mock
    private MerchantWebhookPort merchantWebhookPort;

    @InjectMocks
    private CreateTransactionUseCase useCase;

    @Test
    void shouldCreateSaveAndPublishTransaction() {
        UUID merchantId = UUID.randomUUID();
        CreateTransactionCommand cmd = new CreateTransactionCommand(merchantId, new BigDecimal("150.00"), "PIX");
        when(merchantWebhookPort.findWebhookUrl(merchantId)).thenReturn("https://loja.exemplo.com/webhooks");
        
        Transaction tx = useCase.execute(cmd);
        
        assertEquals(TransactionStatus.PENDING, tx.getStatus());
        assertEquals(new BigDecimal("150.00"), tx.getAmount());
        assertEquals("https://loja.exemplo.com/webhooks", tx.getWebhookUrl());
        
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(savePort).save(txCaptor.capture());
        assertEquals(tx.getId(), txCaptor.getValue().getId());
        assertEquals("https://loja.exemplo.com/webhooks", txCaptor.getValue().getWebhookUrl());
        
        verify(publishPort).publish(txCaptor.getValue());
    }

    @Test
    void shouldCreateTransactionWithoutWebhookWhenMerchantHasNone() {
        UUID merchantId = UUID.randomUUID();
        CreateTransactionCommand cmd = new CreateTransactionCommand(merchantId, new BigDecimal("10.00"), "PIX");
        when(merchantWebhookPort.findWebhookUrl(merchantId)).thenReturn(null);

        Transaction tx = useCase.execute(cmd);

        assertNull(tx.getWebhookUrl());
        verify(savePort).save(tx);
    }
}
