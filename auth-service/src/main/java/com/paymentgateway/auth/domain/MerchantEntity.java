package com.paymentgateway.auth.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantEntity {
    @Id
    private UUID id;
    
    @Column(name = "company_name", nullable = false)
    private String companyName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
