package com.crowdfunding.dynomite.client;

import com.crowdfunding.dynomite.api.dto.KvPutRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

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
            String value = webClient.get()
                    .uri(buildInternalKeyUri(baseUrl, key))
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
        webClient.put()
                .uri(buildInternalKeyUri(baseUrl, key))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .timeout(timeout)
                .block();
    }

    public boolean deleteLocalOnly(String baseUrl, String key, Duration timeout) {
        try {
            webClient.delete()
                    .uri(buildInternalKeyUri(baseUrl, key))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(timeout)
                    .block();
            return true;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw ex;
        }
    }

    static String buildInternalKeyUri(String baseUrl, String key) {
        return UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("v1", "internal", "kv", key)
                .build()
                .encode()
                .toUriString();
    }
}
