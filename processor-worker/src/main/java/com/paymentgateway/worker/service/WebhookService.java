package com.paymentgateway.worker.service;

import com.paymentgateway.worker.domain.WebhookLog;
import com.paymentgateway.worker.repository.WebhookLogRepository;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WebhookService {

    private final RestTemplate restTemplate;
    private final WebhookLogRepository repository;

    public WebhookService(RestTemplate restTemplate, WebhookLogRepository repository) {
        this.restTemplate = restTemplate;
        this.repository = repository;
    }

    @Retryable(
      value = { Exception.class }, 
      maxAttempts = 3,
      backoff = @Backoff(delay = 100)
    )
    public void sendWebhook(UUID transactionId, String url, String status) {
        logAttempt(transactionId, status);
        
        try {
            restTemplate.postForEntity(url, status, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Webhook failed", e);
        }
    }

    @Recover
    public void recover(Exception e, UUID transactionId, String url, String status) {
        logAttempt(transactionId, "FAILED_DELIVERY");
        throw new RuntimeException("Webhook delivery failed after retries", e);
    }

    private void logAttempt(UUID transactionId, String status) {
        WebhookLog log = new WebhookLog();
        log.setTransactionId(transactionId);
        log.setStatus(status);
        log.setLastAttempt(LocalDateTime.now());
        log.setAttemptCount(1);
        repository.save(log);
    }
}
