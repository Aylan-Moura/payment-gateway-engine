package com.paymentgateway.core.usecase;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import com.paymentgateway.core.usecase.port.out.PublishPaymentEventPort;
import com.paymentgateway.core.usecase.port.out.SaveTransactionPort;
import org.springframework.stereotype.Service;

@Service
public class CreateTransactionUseCase {
    private final SaveTransactionPort saveTransactionPort;
    private final PublishPaymentEventPort publishPaymentEventPort;

    public CreateTransactionUseCase(SaveTransactionPort saveTransactionPort, PublishPaymentEventPort publishPaymentEventPort) {
        this.saveTransactionPort = saveTransactionPort;
        this.publishPaymentEventPort = publishPaymentEventPort;
    }

    public Transaction execute(CreateTransactionCommand command) {
        Transaction transaction = new Transaction(command.getMerchantId(), command.getAmount(), command.getPaymentMethod());
        saveTransactionPort.save(transaction);
        publishPaymentEventPort.publish(transaction);
        return transaction;
    }
}
