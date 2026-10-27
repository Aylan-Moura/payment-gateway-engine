package com.paymentgateway.worker.service;

import com.paymentgateway.worker.listener.PaymentMessage;
import com.paymentgateway.worker.repository.TransactionEntity;
import com.paymentgateway.worker.repository.WorkerTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PaymentProcessorServiceTest {

    @Mock
    private WorkerTransactionRepository transactionRepository;

    @Mock
    private ReceiptService receiptService;

    @Mock
    private S3ReceiptService s3ReceiptService;

    @Mock
    private WebhookService webhookService;

    private PaymentProcessorService service;
    private TransactionEntity transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionEntity();
        transaction.setId(UUID.randomUUID());
    }

    @Test
    void shouldUseMerchantWebhookFromMessage() {
        service = new PaymentProcessorService(transactionRepository, receiptService, s3ReceiptService,
                webhookService, "https://fallback.exemplo.com/webhook");
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(receiptService.generateReceiptPdf(any())).thenReturn(new byte[]{1});

        service.process(message("https://loja.exemplo.com/webhooks"));

        verify(webhookService).sendWebhook(eq(transaction.getId()), eq("https://loja.exemplo.com/webhooks"), eq("APPROVED"));
    }

    @Test
    void shouldFallbackToDefaultWebhookWhenMerchantHasNone() {
        service = new PaymentProcessorService(transactionRepository, receiptService, s3ReceiptService,
                webhookService, "https://fallback.exemplo.com/webhook");
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(receiptService.generateReceiptPdf(any())).thenReturn(new byte[]{1});

        service.process(message(null));

        verify(webhookService).sendWebhook(eq(transaction.getId()), eq("https://fallback.exemplo.com/webhook"), eq("APPROVED"));
    }

    @Test
    void shouldSkipWebhookWhenNoUrlIsConfigured() {
        service = new PaymentProcessorService(transactionRepository, receiptService, s3ReceiptService,
                webhookService, "");
        when(transactionRepository.findById(transaction.getId())).thenReturn(Optional.of(transaction));
        when(receiptService.generateReceiptPdf(any())).thenReturn(new byte[]{1});

        service.process(message("  "));

        verify(webhookService, never()).sendWebhook(any(), any(), any());
        verify(s3ReceiptService).uploadReceipt(eq(transaction.getId()), any());
    }

    private PaymentMessage message(String webhookUrl) {
        PaymentMessage message = new PaymentMessage();
        message.setTransactionId(transaction.getId());
        message.setMerchantId(UUID.randomUUID());
        message.setAmount(new BigDecimal("99.90"));
        message.setStatus("PENDING");
        message.setWebhookUrl(webhookUrl);
        return message;
    }
}
