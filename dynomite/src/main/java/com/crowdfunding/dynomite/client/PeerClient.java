package com.crowdfunding.dynomite.client;

import com.crowdfunding.dynomite.api.dto.KvPutRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;

@Component
public class PeerClient {

    private final WebClient webClient;

    public PeerClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Optional<String> getLocalOnly(String baseUrl, String key, Duration timeout) {
        try {
            String url = baseUrl + "/v1/internal/kv/" + key;
            String value = webClient.get()
                    .uri(url)
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout)
                    .block();
            return Optional.ofNullable(value);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    public void putLocalOnly(String baseUrl, String key, KvPutRequest request, Duration timeout) {
        String url = baseUrl + "/v1/internal/kv/" + key;
        webClient.put()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .timeout(timeout)
                .block();
    }

    public void deleteLocalOnly(String baseUrl, String key, Duration timeout) {
        String url = baseUrl + "/v1/internal/kv/" + key;
        webClient.delete()
                .uri(url)
                .retrieve()
                .toBodilessEntity()
                .timeout(timeout)
                .block();
    }
}

