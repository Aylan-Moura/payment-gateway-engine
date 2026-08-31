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
                .andExpect(jsonPath("$.refreshToken").exists());

        assertTrue(merchantRepository.findByEmail("test@company.com").isPresent());
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
                .andExpect(jsonPath("$.refreshToken").exists());
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
                .andExpect(jsonPath("$.refreshToken").exists());
    }
}
