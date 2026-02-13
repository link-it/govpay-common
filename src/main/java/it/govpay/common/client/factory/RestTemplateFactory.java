package it.govpay.common.client.factory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import it.govpay.common.client.gde.GdeCapturingInterceptor;
import it.govpay.common.client.model.Connettore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestTemplateFactory {

    public RestTemplate createRestTemplate(Connettore connettore) {
        log.info("Creazione RestTemplate per connettore: {}", connettore.getIdConnettore());

        RestTemplateBuilder builder = new RestTemplateBuilder()
                .rootUri(connettore.getUrl());

        // Set timeouts only if configured
        if (connettore.getConnectionTimeout() != null) {
            builder = builder.connectTimeout(Duration.ofMillis(connettore.getConnectionTimeout()));
        }
        if (connettore.getReadTimeout() != null) {
            builder = builder.readTimeout(Duration.ofMillis(connettore.getReadTimeout()));
        }

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

        switch (connettore.getTipoAutenticazione()) {
            case HTTP_BASIC -> {
                log.debug("Configurazione HTTP Basic Auth per connettore: {}", connettore.getIdConnettore());
                interceptors.add(new BasicAuthInterceptor(connettore.getHttpUser(), connettore.getHttpPassw()));
            }
            case API_KEY -> {
                log.debug("Configurazione API Key per connettore: {}", connettore.getIdConnettore());
                String headerName = connettore.getApiId() != null ? connettore.getApiId() : "X-API-Key";
                interceptors.add(new ApiKeyInterceptor(headerName, connettore.getApiKey()));
            }
            case HTTP_HEADER -> {
                log.debug("Configurazione Custom Headers per connettore: {}", connettore.getIdConnettore());
                String customHeader = connettore.getHttpHeaderName() + ":" + connettore.getHttpHeaderValue();
                interceptors.add(new CustomHeadersInterceptor(customHeader));
            }
            case OAUTH2_CLIENT_CREDENTIALS -> {
                log.debug("Configurazione OAuth2 per connettore: {}", connettore.getIdConnettore());
                interceptors.add(new OAuth2Interceptor(connettore));
            }
            case SSL -> {
                log.debug("Configurazione SSL/TLS per connettore: {}", connettore.getIdConnettore());
                try {
                    HttpComponentsClientHttpRequestFactory factory = createSslRequestFactory(connettore);
                    RestTemplate restTemplate = builder.requestFactory(() -> factory).build();

                    // Add GDE capturing interceptor for SSL connections too
                    List<ClientHttpRequestInterceptor> sslInterceptors = new ArrayList<>(interceptors);
                    sslInterceptors.add(new GdeCapturingInterceptor());
                    restTemplate.setInterceptors(sslInterceptors);

                    log.debug("Aggiunto GdeCapturingInterceptor per connettore SSL: {}", connettore.getIdConnettore());
                    return restTemplate;
                } catch (Exception e) {
                    log.error("Errore durante la configurazione SSL per connettore: {}",
                            connettore.getIdConnettore(), e);
                    throw new RuntimeException("Errore configurazione SSL", e);
                }
            }
            case NONE -> log.debug("Nessuna autenticazione configurata per connettore: {}",
                    connettore.getIdConnettore());
        }

        // Gestione Subscription Key (Azure APIM) se presente
        if (StringUtils.hasText(connettore.getSubscriptionKeyValue())) {
            log.debug("Configurazione Subscription Key per connettore: {}", connettore.getIdConnettore());
            interceptors.add(new SubscriptionKeyInterceptor(connettore.getSubscriptionKeyValue()));
        }

        // Gestione Custom Headers generici se presenti
        if (connettore.getCustomHeaders() != null && !connettore.getCustomHeaders().isEmpty()) {
            log.debug("Configurazione {} Custom Headers per connettore: {}",
                    connettore.getCustomHeaders().size(), connettore.getIdConnettore());
            interceptors.add(new GenericCustomHeadersInterceptor(connettore.getCustomHeaders()));
        }

        // Add GDE capturing interceptor LAST so it captures all headers after other interceptors add them
        interceptors.add(new GdeCapturingInterceptor());
        log.debug("Aggiunto GdeCapturingInterceptor per connettore: {}", connettore.getIdConnettore());

        RestTemplate restTemplate = builder.build();
        restTemplate.setInterceptors(interceptors);

        log.info("RestTemplate creato con successo per connettore: {}", connettore.getIdConnettore());
        return restTemplate;
    }

    private HttpComponentsClientHttpRequestFactory createSslRequestFactory(Connettore connettore) throws Exception {
        SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();

        if (StringUtils.hasText(connettore.getSslKsLocation())) {
            KeyStore keyStore = KeyStore.getInstance(
                    StringUtils.hasText(connettore.getSslKsType()) ?
                            connettore.getSslKsType() : "PKCS12");
            try (FileInputStream fis = new FileInputStream(connettore.getSslKsLocation())) {
                keyStore.load(fis, connettore.getSslKsPasswd().toCharArray());
            }
            sslContextBuilder.loadKeyMaterial(keyStore,
                    connettore.getSslKsPasswd().toCharArray());
        }

        if (StringUtils.hasText(connettore.getSslTsLocation())) {
            KeyStore trustStore = KeyStore.getInstance(
                    StringUtils.hasText(connettore.getSslTsType()) ?
                            connettore.getSslTsType() : "JKS");
            try (FileInputStream fis = new FileInputStream(connettore.getSslTsLocation())) {
                trustStore.load(fis, connettore.getSslTsPasswd().toCharArray());
            }
            sslContextBuilder.loadTrustMaterial(trustStore, null);
        }

        SSLContext sslContext = sslContextBuilder.build();
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext, NoopHostnameVerifier.INSTANCE);

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(socketFactory)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connettore.getConnectionTimeout());
        factory.setConnectionRequestTimeout(connettore.getReadTimeout());

        return factory;
    }

    private static class BasicAuthInterceptor implements ClientHttpRequestInterceptor {
        private final String encodedCredentials;

        public BasicAuthInterceptor(String username, String password) {
            String credentials = username + ":" + password;
            this.encodedCredentials = Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
            return execution.execute(request, body);
        }
    }

    private static class ApiKeyInterceptor implements ClientHttpRequestInterceptor {
        private final String headerName;
        private final String apiKey;

        public ApiKeyInterceptor(String headerName, String apiKey) {
            this.headerName = headerName != null ? headerName : "X-API-Key";
            this.apiKey = apiKey;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(headerName, apiKey);
            return execution.execute(request, body);
        }
    }

    private static class CustomHeadersInterceptor implements ClientHttpRequestInterceptor {
        private final String customHeaders;

        public CustomHeadersInterceptor(String customHeaders) {
            this.customHeaders = customHeaders;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            if (StringUtils.hasText(customHeaders)) {
                String[] headers = customHeaders.split(";");
                for (String header : headers) {
                    String[] keyValue = header.split(":", 2);
                    if (keyValue.length == 2) {
                        request.getHeaders().set(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }
            return execution.execute(request, body);
        }
    }

    private static class OAuth2Interceptor implements ClientHttpRequestInterceptor {
        private final Connettore connettore;
        private String accessToken;
        private long tokenExpiration;

        public OAuth2Interceptor(Connettore connettore) {
            this.connettore = connettore;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            if (accessToken == null || System.currentTimeMillis() >= tokenExpiration) {
                refreshToken();
            }
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            return execution.execute(request, body);
        }

        private void refreshToken() {
            log.debug("Implementazione refresh token OAuth2 - da completare con logica specifica");
        }
    }

    private static class SubscriptionKeyInterceptor implements ClientHttpRequestInterceptor {
        private static final String SUBSCRIPTION_KEY_HEADER = "Ocp-Apim-Subscription-Key";
        private final String subscriptionKey;

        public SubscriptionKeyInterceptor(String subscriptionKey) {
            this.subscriptionKey = subscriptionKey;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(SUBSCRIPTION_KEY_HEADER, subscriptionKey);
            return execution.execute(request, body);
        }
    }

    private static class GenericCustomHeadersInterceptor implements ClientHttpRequestInterceptor {
        private final java.util.Map<String, String> customHeaders;

        public GenericCustomHeadersInterceptor(java.util.Map<String, String> customHeaders) {
            this.customHeaders = customHeaders;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            customHeaders.forEach((headerName, headerValue) -> {
                request.getHeaders().set(headerName, headerValue);
                log.debug("Custom header applicato: {} = {}", headerName, headerValue);
            });
            return execution.execute(request, body);
        }
    }
}
