package com.paymentgateway.auth.service;

import com.paymentgateway.auth.domain.MerchantEntity;
import com.paymentgateway.auth.dto.AuthResponse;
import com.paymentgateway.auth.dto.LoginRequest;
import com.paymentgateway.auth.dto.RefreshRequest;
import com.paymentgateway.auth.dto.RegisterRequest;
import com.paymentgateway.auth.repository.MerchantRepository;
import com.paymentgateway.auth.security.JwtUtil;
import com.paymentgateway.auth.security.MerchantUserDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
        var merchant = MerchantEntity.builder()
                .id(UUID.randomUUID())
                .companyName(request.getCompanyName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .apiKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();
        repository.save(merchant);
        
        var userDetails = new MerchantUserDetails(merchant);
        var jwtToken = jwtUtil.generateToken(userDetails);
        var refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        return new AuthResponse(jwtToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var merchant = repository.findByEmail(request.getEmail()).orElseThrow();
        var userDetails = new MerchantUserDetails(merchant);
        var jwtToken = jwtUtil.generateToken(userDetails);
        var refreshToken = jwtUtil.generateRefreshToken(userDetails);
        
        return new AuthResponse(jwtToken, refreshToken);
    }

    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String userEmail = jwtUtil.extractUsername(refreshToken);
        
        if (userEmail != null) {
            var merchant = repository.findByEmail(userEmail).orElseThrow();
            var userDetails = new MerchantUserDetails(merchant);
            
            if (jwtUtil.isTokenValid(refreshToken, userDetails)) {
                var accessToken = jwtUtil.generateToken(userDetails);
                return new AuthResponse(accessToken, refreshToken);
            }
        }
        throw new RuntimeException("Invalid refresh token");
    }
}
