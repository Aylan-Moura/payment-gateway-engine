package com.paymentgateway.core.adapters.out.persistence;

import com.paymentgateway.core.domain.Transaction;
import com.paymentgateway.core.usecase.port.out.SaveTransactionPort;
import org.springframework.stereotype.Component;

@Component
public class SaveTransactionAdapter implements SaveTransactionPort {

    private final TransactionRepository repository;

    public SaveTransactionAdapter(TransactionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity(transaction);
        repository.save(entity);
    }
}