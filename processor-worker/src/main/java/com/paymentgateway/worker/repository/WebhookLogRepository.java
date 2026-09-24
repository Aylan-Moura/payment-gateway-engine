package com.paymentgateway.worker.repository;

import com.paymentgateway.worker.domain.WebhookLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
    @Query("SELECT w FROM WebhookLog w WHERE w.transactionId = :transactionId ORDER BY w.lastAttempt DESC")
    Optional<WebhookLog> findByTransactionId(UUID transactionId);
}
