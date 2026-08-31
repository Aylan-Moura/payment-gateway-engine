package com.paymentgateway.worker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class S3ReceiptServiceTest {

    @Mock
    private S3Client s3Client;

    private S3ReceiptService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new S3ReceiptService(s3Client, "payment-receipts-bucket");
    }

    @Test
    void shouldUploadReceiptToS3() {
        UUID txId = UUID.randomUUID();
        String receiptContent = "Receipt for transaction " + txId;
        
        service.uploadReceipt(txId, receiptContent);
        
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        
        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("payment-receipts-bucket", request.bucket());
        assertEquals("receipts/" + txId + ".txt", request.key());
    }
}
