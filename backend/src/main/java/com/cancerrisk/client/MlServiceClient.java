package com.cancerrisk.client;

import com.cancerrisk.exception.MlServiceException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Thin client for the Python ML service. Network failures and 5xx responses
 * are retried with linear backoff, 4xx responses are surfaced immediately.
 */
@Slf4j
@Component
public class MlServiceClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    private final RestTemplate restTemplate;
    private final MlClientProperties properties;
    private final AtomicReference<Map<String, Object>> lastModelInfo = new AtomicReference<>();

    public MlServiceClient(@Qualifier("mlRestTemplate") RestTemplate restTemplate, MlClientProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public MlPrediction predict(Map<String, Double> features, String reference) {
        Map<String, Object> body = reference == null
                ? Map.of("features", features)
                : Map.of("features", features, "reference", reference);
        return withRetry("predict", () -> restTemplate.postForObject("/predict", body, MlPrediction.class));
    }

    /** Model metadata, falls back to the last good response when the service is down. */
    public Map<String, Object> modelInfo() {
        try {
            Map<String, Object> info = withRetry("model info", () -> restTemplate
                    .exchange("/model/info", HttpMethod.GET, null, MAP_TYPE)
                    .getBody());
            lastModelInfo.set(info);
            return info;
        } catch (MlServiceException ex) {
            Map<String, Object> cached = lastModelInfo.get();
            if (cached != null) {
                log.warn("ML service unavailable, serving cached model info");
                return cached;
            }
            throw ex;
        }
    }

    private <T> T withRetry(String operation, Supplier<T> call) {
        int attempts = Math.max(1, properties.maxAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                T result = call.get();
                if (result == null) {
                    throw MlServiceException.unavailable();
                }
                return result;
            } catch (HttpClientErrorException ex) {
                log.warn("ML service rejected {}: {} {}", operation, ex.getStatusCode(), ex.getResponseBodyAsString());
                throw MlServiceException.rejected(ex.getStatusText());
            } catch (ResourceAccessException | HttpServerErrorException ex) {
                log.warn("ML service {} attempt {}/{} failed: {}", operation, attempt, attempts, ex.getMessage());
                if (attempt < attempts) {
                    sleep(properties.backoff().multipliedBy(attempt));
                }
            }
        }
        throw MlServiceException.unavailable();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw MlServiceException.unavailable();
        }
    }
}
