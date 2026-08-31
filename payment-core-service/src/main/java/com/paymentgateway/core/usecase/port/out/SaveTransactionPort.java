package com.paymentgateway.core.usecase.port.out;
import com.paymentgateway.core.domain.Transaction;

public interface SaveTransactionPort {
    void save(Transaction transaction);
}
