/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2026 Link.it srl (http://www.link.it).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.govpay.common.client.oauth2;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.micrometer.observation.ObservationRegistry;

import it.govpay.common.client.factory.ConnettoreClientObservationConvention;
import it.govpay.common.client.model.Connettore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Oauth2ClientCredentialsManager {

    private static final long SAFETY_MARGIN_SECONDS = 30;
    private static final int TOKEN_ENDPOINT_CONNECT_TIMEOUT_MS = 5000;
    private static final int TOKEN_ENDPOINT_READ_TIMEOUT_MS = 15000;

    private final ConcurrentHashMap<String, CachedToken> tokenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    private ObservationRegistry observationRegistry;

    /**
     * Se nel contesto e' presente un {@link ObservationRegistry}, anche la
     * negoziazione del token OAuth2 pubblica {@code http.client.requests}
     * con il tag {@code connettore}.
     */
    @Autowired
    public void setObservationRegistry(ObjectProvider<ObservationRegistry> observationRegistryProvider) {
        this.observationRegistry = observationRegistryProvider.getIfAvailable();
    }

    public String getAccessToken(String key, Connettore connettore) {
        CachedToken cached = tokenCache.get(key);
        if (cached != null && !cached.isExpired()) {
            log.debug("Token OAuth2 in cache per connettore: {}", key);
            return cached.accessToken();
        }

        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
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
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(TOKEN_ENDPOINT_CONNECT_TIMEOUT_MS))
                        .build())
                .build();
        var httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
        var factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectionRequestTimeout(TOKEN_ENDPOINT_READ_TIMEOUT_MS);
        RestTemplate tokenRestTemplate = new RestTemplate(factory);
        if (observationRegistry != null) {
            tokenRestTemplate.setObservationRegistry(observationRegistry);
            tokenRestTemplate.setObservationConvention(
                    new ConnettoreClientObservationConvention(connettore.getIdConnettore()));
        }

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
