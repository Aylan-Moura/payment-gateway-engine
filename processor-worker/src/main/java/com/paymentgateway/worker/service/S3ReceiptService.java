package com.paymentgateway.worker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class S3ReceiptService {

    private final S3Client s3Client;
    private final String bucketName;

    public S3ReceiptService(S3Client s3Client, @Value("${aws.s3.bucket:payment-receipts-bucket}") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    public void uploadReceipt(UUID transactionId, String content) {
        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucketName)
                .key("receipts/" + transactionId + ".txt")
                .build();

        s3Client.putObject(putOb, RequestBody.fromString(content));
    }
}
