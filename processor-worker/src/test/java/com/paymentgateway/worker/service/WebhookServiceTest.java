package com.paymentgateway.worker.service;

import com.paymentgateway.worker.domain.WebhookLog;
import com.paymentgateway.worker.repository.WebhookLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
public class WebhookServiceTest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private RestTemplate restTemplate;

    @MockBean
    private WebhookLogRepository repository;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void shouldSendWebhookSuccessfully() {
        UUID txId = UUID.randomUUID();
        String url = "http://merchant.com/webhook";
        
        mockServer.expect(ExpectedCount.once(), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        webhookService.sendWebhook(txId, url, "APPROVED");
        
        mockServer.verify();
        verify(repository).save(any(WebhookLog.class));
    }

    @Test
    void shouldRetryOnFailure() {
        UUID txId = UUID.randomUUID();
        String url = "http://merchant.com/webhook";
        
        mockServer.expect(ExpectedCount.times(3), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(RuntimeException.class, () -> webhookService.sendWebhook(txId, url, "APPROVED"));
        
        mockServer.verify();
    }
}
