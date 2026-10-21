package com.paymentgateway.worker.web;

import com.paymentgateway.worker.repository.TransactionEntity;
import com.paymentgateway.worker.repository.WorkerTransactionRepository;
import com.paymentgateway.worker.service.ReceiptService;
import com.paymentgateway.worker.service.S3ReceiptService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/receipts")
public class ReceiptController {

    private final WorkerTransactionRepository transactionRepository;
    private final S3ReceiptService s3ReceiptService;
    private final ReceiptService receiptService;

    public ReceiptController(WorkerTransactionRepository transactionRepository,
                             S3ReceiptService s3ReceiptService,
                             ReceiptService receiptService) {
        this.transactionRepository = transactionRepository;
        this.s3ReceiptService = s3ReceiptService;
        this.receiptService = receiptService;
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<byte[]> download(@PathVariable("transactionId") UUID transactionId) {
        byte[] pdf = loadOrGenerate(transactionId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"receipt-" + transactionId + ".pdf\"")
                .body(pdf);
    }

    /**
     * Prefere o comprovante arquivado no S3; se ainda nao existir
     * (ex.: transacao anterior a geracao de PDF), gera a partir do banco.
     */
    private byte[] loadOrGenerate(UUID transactionId) {
        try {
            return s3ReceiptService.downloadReceipt(transactionId);
        } catch (S3Exception e) {
            if (e.statusCode() != HttpStatus.NOT_FOUND.value()) {
                throw e;
            }
        }

        TransactionEntity transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Transaction not found: " + transactionId));

        return receiptService.generateReceiptPdf(transaction);
    }
}
