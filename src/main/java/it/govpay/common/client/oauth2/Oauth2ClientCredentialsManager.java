package it.govpay.common.client.oauth2;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import it.govpay.common.client.model.Connettore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Oauth2ClientCredentialsManager {

    private static final long SAFETY_MARGIN_SECONDS = 30;
    private static final int TOKEN_ENDPOINT_CONNECT_TIMEOUT_MS = 5000;
    private static final int TOKEN_ENDPOINT_READ_TIMEOUT_MS = 15000;

    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public String getAccessToken(String key, Connettore connettore) {
        CachedToken cached = tokenCache.get(key);
        if (cached != null && !cached.isExpired()) {
            log.debug("Token OAuth2 in cache per connettore: {}", key);
            return cached.accessToken();
        }

        synchronized (key.intern()) {
            // Double-check dopo aver acquisito il lock
            cached = tokenCache.get(key);
            if (cached != null && !cached.isExpired()) {
                return cached.accessToken();
            }

            log.info("Negoziazione token OAuth2 per connettore: {}", key);
            CachedToken newToken = refreshToken(connettore);
            tokenCache.put(key, newToken);
            return newToken.accessToken();
        }
    }

    CachedToken refreshToken(Connettore connettore) {
        RestTemplate tokenRestTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofMillis(TOKEN_ENDPOINT_CONNECT_TIMEOUT_MS))
                .readTimeout(Duration.ofMillis(TOKEN_ENDPOINT_READ_TIMEOUT_MS))
                .build();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", connettore.getOauth2ClientCredentialsClientId());
        formData.add("client_secret", connettore.getOauth2ClientCredentialsClientSecret());

        if (StringUtils.hasText(connettore.getOauth2ClientCredentialsScope())) {
            formData.add("scope", connettore.getOauth2ClientCredentialsScope());
        }

        String tokenEndpoint = connettore.getOauth2ClientCredentialsUrlTokenEndpoint();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        RequestEntity<MultiValueMap<String, String>> requestEntity = RequestEntity
                .post(tokenEndpoint)
                .headers(headers)
                .body(formData);

        try {
            ResponseEntity<TokenResponse> response = tokenRestTemplate.exchange(
                    requestEntity, TokenResponse.class);

            TokenResponse tokenResponse = response.getBody();
            if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
                throw new Oauth2TokenException(
                        "Risposta token endpoint vuota o senza access_token per connettore: " +
                                connettore.getIdConnettore());
            }

            long issuedAt = System.currentTimeMillis() / 1000;
            log.info("Token OAuth2 ottenuto per connettore: {}, expires_in: {}s",
                    connettore.getIdConnettore(), tokenResponse.expiresIn());

            return new CachedToken(tokenResponse.accessToken(), issuedAt, tokenResponse.expiresIn());
        } catch (RestClientException e) {
            throw new Oauth2TokenException(
                    "Errore nella negoziazione del token OAuth2 per connettore: " +
                            connettore.getIdConnettore() + " - endpoint: " + tokenEndpoint, e);
        }
    }

    public void invalidateToken(String key) {
        log.debug("Invalidazione token OAuth2 per connettore: {}", key);
        tokenCache.remove(key);
    }

    public void clearAll() {
        log.info("Pulizia completa cache token OAuth2");
        tokenCache.clear();
    }

    record CachedToken(String accessToken, long issuedAtSeconds, long expiresInSeconds) {
        boolean isExpired() {
            long currentTimeSeconds = System.currentTimeMillis() / 1000;
            return currentTimeSeconds >= issuedAtSeconds + expiresInSeconds - SAFETY_MARGIN_SECONDS;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {}

    public static class Oauth2TokenException extends RuntimeException {
        public Oauth2TokenException(String message) {
            super(message);
        }

        public Oauth2TokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
