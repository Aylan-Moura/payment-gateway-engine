package com.paymentgateway.core.usecase.port.out;
import com.paymentgateway.core.domain.Transaction;

public interface PublishPaymentEventPort {
    void publish(Transaction transaction);
}
