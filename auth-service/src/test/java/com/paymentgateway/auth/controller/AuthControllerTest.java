package com.paymentgateway.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentgateway.auth.dto.LoginRequest;
import com.paymentgateway.auth.dto.RefreshRequest;
import com.paymentgateway.auth.dto.RegisterRequest;
import com.paymentgateway.auth.repository.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MerchantRepository merchantRepository;

    @BeforeEach
    void setUp() {
        merchantRepository.deleteAll();
    }

    @Test
    void shouldRegisterNewMerchant() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setCompanyName("Test Company");
        request.setEmail("test@company.com");
        request.setPassword("securePassword123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.merchantId").exists())
                .andExpect(jsonPath("$.apiKey").exists());

        assertTrue(merchantRepository.findByEmail("test@company.com").isPresent());
    }

    @Test
    void shouldRegisterNewMerchantWithWebhookUrl() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setCompanyName("Webhook Company");
        request.setEmail("webhook@company.com");
        request.setPassword("securePassword123");
        request.setWebhookUrl(" https://loja.exemplo.com/webhooks/payments ");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        var merchant = merchantRepository.findByEmail("webhook@company.com").orElseThrow();
        assertEquals("https://loja.exemplo.com/webhooks/payments", merchant.getWebhookUrl());
    }

    @Test
    void shouldRejectInvalidWebhookUrl() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setCompanyName("Invalid Webhook Company");
        request.setEmail("invalid-webhook@company.com");
        request.setPassword("securePassword123");
        request.setWebhookUrl("ftp://loja.exemplo.com/webhook");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        assertFalse(merchantRepository.findByEmail("invalid-webhook@company.com").isPresent());
    }

    @Test
    void shouldRejectWebhookUpdateWithoutToken() throws Exception {
        mockMvc.perform(put("/api/v1/merchants/me/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"webhookUrl\":\"https://loja.exemplo.com/webhooks\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateWebhookForAuthenticatedMerchant() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setCompanyName("Update Webhook Company");
        register.setEmail("update-webhook@company.com");
        register.setPassword("securePassword123");

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(put("/api/v1/merchants/me/webhook")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"webhookUrl\":\"https://loja.exemplo.com/v2/webhooks\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl").value("https://loja.exemplo.com/v2/webhooks"));

        assertEquals("https://loja.exemplo.com/v2/webhooks",
                merchantRepository.findByEmail("update-webhook@company.com").orElseThrow().getWebhookUrl());
    }

    @Test
    void shouldRejectInvalidWebhookUrlOnUpdate() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setCompanyName("Invalid Update Company");
        register.setEmail("invalid-update@company.com");
        register.setPassword("securePassword123");

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(response).get("token").asText();

        mockMvc.perform(put("/api/v1/merchants/me/webhook")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"webhookUrl\":\"not-a-url\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFailLoginWithIncorrectPassword() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setCompanyName("Test Company");
        register.setEmail("test2@company.com");
        register.setPassword("securePassword123");
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setEmail("test2@company.com");
        login.setPassword("wrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized()); // 401
    }

    @Test
    void shouldLoginSuccessfullyAndReturnJwt() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setCompanyName("Test Company");
        register.setEmail("test3@company.com");
        register.setPassword("securePassword123");
        
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setEmail("test3@company.com");
        login.setPassword("securePassword123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.merchantId").exists())
                .andExpect(jsonPath("$.apiKey").exists());
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setCompanyName("Test Company");
        register.setEmail("test4@company.com");
        register.setPassword("securePassword123");
        
        String response = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = objectMapper.readTree(response).get("refreshToken").asText();

        RefreshRequest refresh = new RefreshRequest();
        refresh.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.merchantId").exists())
                .andExpect(jsonPath("$.apiKey").exists());
    }
}
