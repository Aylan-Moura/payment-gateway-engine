package com.paymentgateway.auth.repository;

import com.paymentgateway.auth.domain.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<MerchantEntity, UUID> {
    Optional<MerchantEntity> findByEmail(String email);
    Optional<MerchantEntity> findByApiKey(String apiKey);
}
