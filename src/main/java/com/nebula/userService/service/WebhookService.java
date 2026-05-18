package com.nebula.userService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private final List<String> webhookUrls;

    private final int timeoutMs;

    public WebhookService(@Value("${webhooks.urls:}") String webhookUrlsConfig,
                          @Value("${webhooks.timeout-ms:2000}") int timeoutMs,
                          ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webhookUrls = Arrays.stream(webhookUrlsConfig.split(","))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .toList();
        this.timeoutMs = timeoutMs;
    }

    @PostConstruct
    public void configureTimeout() {
        restTemplate.setRequestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
            setConnectTimeout(timeoutMs);
            setReadTimeout(timeoutMs);
        }});
    }

    public void publishEvent(String eventType, EventPayload payload) {
        if (webhookUrls.isEmpty()) {
            return;
        }

        payload.setEventType(eventType);
        payload.setTimestamp(LocalDateTime.now().toString());

        for (String url : webhookUrls) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                String body = objectMapper.writeValueAsString(payload);
                HttpEntity<String> request = new HttpEntity<>(body, headers);
                restTemplate.postForEntity(url, request, Void.class);
                log.info("Evento enviado para webhook {}: {}", url, eventType);
            } catch (Exception e) {
                log.error("Falha ao enviar evento para webhook {}: {}", url, e.getMessage());
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventPayload {
        private String eventType;
        private String timestamp;
        private String username;
        private String sessionId;
        private String ipAddress;
        private String userAgent;
        private String details;
    }
}
