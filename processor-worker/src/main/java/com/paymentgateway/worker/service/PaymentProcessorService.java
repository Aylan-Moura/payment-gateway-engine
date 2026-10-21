package com.paymentgateway.worker.service;

import com.paymentgateway.worker.listener.PaymentMessage;
import com.paymentgateway.worker.repository.TransactionEntity;
import com.paymentgateway.worker.repository.WorkerTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessorService {

    private final WorkerTransactionRepository transactionRepository;
    private final ReceiptService receiptService;
    private final S3ReceiptService s3ReceiptService;
    private final WebhookService webhookService;

    public PaymentProcessorService(WorkerTransactionRepository transactionRepository,
                                   ReceiptService receiptService,
                                   S3ReceiptService s3ReceiptService,
                                   WebhookService webhookService) {
        this.transactionRepository = transactionRepository;
        this.receiptService = receiptService;
        this.s3ReceiptService = s3ReceiptService;
        this.webhookService = webhookService;
    }

    public void process(PaymentMessage message) {
        TransactionEntity transaction = transactionRepository.findById(message.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus("APPROVED");
        transactionRepository.save(transaction);

        byte[] receipt = receiptService.generateReceiptPdf(transaction);
        s3ReceiptService.uploadReceipt(transaction.getId(), receipt);

        webhookService.sendWebhook(transaction.getId(), "http://merchant-mock-url/callback", "APPROVED");
    }
}
