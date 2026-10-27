package com.paymentgateway.core.usecase;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import com.paymentgateway.core.usecase.port.out.MerchantWebhookPort;
import com.paymentgateway.core.usecase.port.out.PublishPaymentEventPort;
import com.paymentgateway.core.usecase.port.out.SaveTransactionPort;
import org.springframework.stereotype.Service;

@Service
public class CreateTransactionUseCase {
    private final SaveTransactionPort saveTransactionPort;
    private final PublishPaymentEventPort publishPaymentEventPort;
    private final MerchantWebhookPort merchantWebhookPort;

    public CreateTransactionUseCase(SaveTransactionPort saveTransactionPort,
                                    PublishPaymentEventPort publishPaymentEventPort,
                                    MerchantWebhookPort merchantWebhookPort) {
        this.saveTransactionPort = saveTransactionPort;
        this.publishPaymentEventPort = publishPaymentEventPort;
        this.merchantWebhookPort = merchantWebhookPort;
    }

    public Transaction execute(CreateTransactionCommand command) {
        Transaction transaction = new Transaction(command.getMerchantId(), command.getAmount(), command.getPaymentMethod());
        transaction.setWebhookUrl(merchantWebhookPort.findWebhookUrl(command.getMerchantId()));
        saveTransactionPort.save(transaction);
        publishPaymentEventPort.publish(transaction);
        return transaction;
    }
}
