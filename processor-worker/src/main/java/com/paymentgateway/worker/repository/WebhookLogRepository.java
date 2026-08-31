package com.paymentgateway.worker.repository;

import com.paymentgateway.worker.domain.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
}
