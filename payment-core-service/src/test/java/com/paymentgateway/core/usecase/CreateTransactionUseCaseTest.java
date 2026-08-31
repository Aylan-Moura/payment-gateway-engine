package com.paymentgateway.core.usecase;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.domain.TransactionStatus;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CreateTransactionUseCaseTest {

    @Mock
    private SaveTransactionPort savePort;

    @Mock
    private PublishPaymentEventPort publishPort;

    @InjectMocks
    private CreateTransactionUseCase useCase;

    @Test
    void shouldCreateSaveAndPublishTransaction() {
        UUID merchantId = UUID.randomUUID();
        CreateTransactionCommand cmd = new CreateTransactionCommand(merchantId, new BigDecimal("150.00"), "PIX");
        
        Transaction tx = useCase.execute(cmd);
        
        assertEquals(TransactionStatus.PENDING, tx.getStatus());
        assertEquals(new BigDecimal("150.00"), tx.getAmount());
        
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(savePort).save(txCaptor.capture());
        assertEquals(tx.getId(), txCaptor.getValue().getId());
        
        verify(publishPort).publish(txCaptor.getValue());
    }
}
