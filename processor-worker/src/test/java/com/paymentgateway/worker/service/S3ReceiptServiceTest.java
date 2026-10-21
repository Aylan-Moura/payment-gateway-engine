package com.paymentgateway.worker.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void shouldUploadReceiptToS3() throws Exception {
        UUID txId = UUID.randomUUID();
        byte[] receiptContent = "%PDF-1.4 fake receipt".getBytes();

        service.uploadReceipt(txId, receiptContent);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("payment-receipts-bucket", request.bucket());
        assertEquals("receipts/" + txId + ".pdf", request.key());
        assertEquals("application/pdf", request.contentType());

        byte[] uploaded = bodyCaptor.getValue().contentStreamProvider().newStream().readAllBytes();
        assertArrayEquals(receiptContent, uploaded);
    }
}
