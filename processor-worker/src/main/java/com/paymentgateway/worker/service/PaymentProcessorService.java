package com.paymentgateway.worker.service;

import com.paymentgateway.worker.listener.PaymentMessage;
import com.paymentgateway.worker.repository.TransactionEntity;
import com.paymentgateway.worker.repository.WorkerTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessorService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorService.class);

    private final WorkerTransactionRepository transactionRepository;
    private final ReceiptService receiptService;
    private final S3ReceiptService s3ReceiptService;
    private final WebhookService webhookService;
    private final String defaultWebhookUrl;

    public PaymentProcessorService(WorkerTransactionRepository transactionRepository,
                                   ReceiptService receiptService,
                                   S3ReceiptService s3ReceiptService,
                                   WebhookService webhookService,
                                   @Value("${app.webhook.default-url:}") String defaultWebhookUrl) {
        this.transactionRepository = transactionRepository;
        this.receiptService = receiptService;
        this.s3ReceiptService = s3ReceiptService;
        this.webhookService = webhookService;
        this.defaultWebhookUrl = defaultWebhookUrl;
    }

    public void process(PaymentMessage message) {
        TransactionEntity transaction = transactionRepository.findById(message.getTransactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        transaction.setStatus("APPROVED");
        transactionRepository.save(transaction);

        byte[] receipt = receiptService.generateReceiptPdf(transaction);
        s3ReceiptService.uploadReceipt(transaction.getId(), receipt);

        String webhookUrl = resolveWebhookUrl(message);
        if (webhookUrl == null) {
            log.warn("No webhook URL configured for transaction {}; skipping notification", transaction.getId());
            return;
        }

        webhookService.sendWebhook(transaction.getId(), webhookUrl, "APPROVED");
    }

    /**
     * Prefere a URL configurada pelo merchant (persistida na transacao e enviada
     * no evento) e cai para a URL padrao da aplicacao quando ausente.
     */
    private String resolveWebhookUrl(PaymentMessage message) {
        if (message.getWebhookUrl() != null && !message.getWebhookUrl().isBlank()) {
            return message.getWebhookUrl().trim();
        }
        if (defaultWebhookUrl != null && !defaultWebhookUrl.isBlank()) {
            return defaultWebhookUrl.trim();
        }
        return null;
    }
}
