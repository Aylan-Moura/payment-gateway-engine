package com.paymentgateway.core.usecase;

import com.paymentgateway.core.domain.MerchantAccount;
import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.usecase.port.in.CreateTransactionCommand;
import com.paymentgateway.core.usecase.port.out.MerchantAccountPort;
import com.paymentgateway.core.usecase.port.out.PublishPaymentEventPort;
import com.paymentgateway.core.usecase.port.out.SaveTransactionPort;
import org.springframework.stereotype.Service;

@Service
public class CreateTransactionUseCase {
    private final SaveTransactionPort saveTransactionPort;
    private final PublishPaymentEventPort publishPaymentEventPort;
    private final MerchantAccountPort merchantAccountPort;

    public CreateTransactionUseCase(SaveTransactionPort saveTransactionPort,
                                    PublishPaymentEventPort publishPaymentEventPort,
                                    MerchantAccountPort merchantAccountPort) {
        this.saveTransactionPort = saveTransactionPort;
        this.publishPaymentEventPort = publishPaymentEventPort;
        this.merchantAccountPort = merchantAccountPort;
    }

    public Transaction execute(CreateTransactionCommand command) {
        Transaction transaction = new Transaction(command.getMerchantId(), command.getAmount(), command.getPaymentMethod());
        transaction.setWebhookUrl(merchantAccountPort.findById(command.getMerchantId())
                .map(MerchantAccount::webhookUrl)
                .orElse(null));
        saveTransactionPort.save(transaction);
        publishPaymentEventPort.publish(transaction);
        return transaction;
    }
}
