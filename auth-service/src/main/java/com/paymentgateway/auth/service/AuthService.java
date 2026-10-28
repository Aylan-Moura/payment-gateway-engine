package com.paymentgateway.auth.service;

import com.paymentgateway.auth.domain.MerchantEntity;
import com.paymentgateway.auth.dto.AuthResponse;
import com.paymentgateway.auth.dto.LoginRequest;
import com.paymentgateway.auth.dto.RefreshRequest;
import com.paymentgateway.auth.dto.RegisterRequest;
import com.paymentgateway.auth.exception.InvalidWebhookUrlException;
import com.paymentgateway.auth.repository.MerchantRepository;
import com.paymentgateway.auth.security.JwtUtil;
import com.paymentgateway.auth.security.MerchantUserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final MerchantRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthService(MerchantRepository repository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (repository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        var merchant = MerchantEntity.builder()
                .id(UUID.randomUUID())
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .apiKey(UUID.randomUUID().toString())
                .webhookUrl(validateWebhookUrl(request.getWebhookUrl()))
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(merchant);
        
        var userDetails = new MerchantUserDetails(merchant);
        var jwtToken = jwtUtil.generateToken(userDetails);
        var refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        return new AuthResponse(jwtToken, refreshToken, merchant.getId(), merchant.getApiKey());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var merchant = repository.findByEmail(request.getEmail()).orElseThrow();
        var userDetails = new MerchantUserDetails(merchant);
        var jwtToken = jwtUtil.generateToken(userDetails);
        var refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        return new AuthResponse(jwtToken, refreshToken, merchant.getId(), merchant.getApiKey());
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String userEmail = jwtUtil.extractUsername(refreshToken);
        
        if (userEmail != null) {
            var merchant = repository.findByEmail(userEmail).orElseThrow();
            var userDetails = new MerchantUserDetails(merchant);
            
            if (jwtUtil.isTokenValid(refreshToken, userDetails)) {
                var accessToken = jwtUtil.generateToken(userDetails);
                return new AuthResponse(accessToken, refreshToken, merchant.getId(), merchant.getApiKey());
            }
        }
        throw new RuntimeException("Invalid refresh token");
    }

    /**
     * Atualiza a URL de webhook do merchant autenticado.
     *
     * @return a URL persistida ou {@code null} quando a configuracao foi removida.
     */
    public String updateWebhookUrl(String email, String webhookUrl) {
        var merchant = repository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated merchant not found: " + email));
        String validated = validateWebhookUrl(webhookUrl);
        merchant.setWebhookUrl(validated);
        repository.save(merchant);
        return validated;
    }

    /**
     * Normaliza e valida a URL de webhook informada no registro.
     * Retorna {@code null} quando nao informada (o worker usa a URL padrao configurada).
     */
    private String validateWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return null;
        }

        String trimmed = webhookUrl.trim();
        if (trimmed.length() > 500) {
            throw new InvalidWebhookUrlException("Webhook URL must have at most 500 characters");
        }

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!httpScheme || uri.getHost() == null) {
                throw new InvalidWebhookUrlException("Webhook URL must be an absolute http(s) URL");
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidWebhookUrlException("Webhook URL is invalid");
        }

        return trimmed;
    }
}
