package it.govpay.common.client.factory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.ObjectMapper;

import it.govpay.common.client.gde.GdeCapturingInterceptor;
import it.govpay.common.client.model.Connettore;
import it.govpay.common.client.oauth2.Oauth2ClientCredentialsManager;
import it.govpay.common.entity.TipoAutenticazione;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestTemplateFactory {

    private final Oauth2ClientCredentialsManager oauth2TokenManager;
    private final ObjectMapper objectMapper;

    public RestTemplateFactory(Oauth2ClientCredentialsManager oauth2TokenManager, ObjectMapper objectMapper) {
        this.oauth2TokenManager = oauth2TokenManager;
        this.objectMapper = objectMapper;
    }

    public RestTemplate createRestTemplate(Connettore connettore) {
        log.info("Creazione RestTemplate per connettore: {}", connettore.getIdConnettore());

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();

        if (connettore.getTipoAutenticazione().equals(TipoAutenticazione.SSL)) {
            return createSslRestTemplate(connettore, interceptors);
        }

        addAuthInterceptor(connettore, interceptors);
        addCommonInterceptors(connettore, interceptors);

        RestTemplate restTemplate = new RestTemplate(createRequestFactory(connettore));
        restTemplate.setInterceptors(interceptors);
        configureObjectMapper(restTemplate);

        log.info("RestTemplate creato con successo per connettore: {}", connettore.getIdConnettore());
        return restTemplate;
    }

    private HttpComponentsClientHttpRequestFactory createRequestFactory(Connettore connettore) {
        ConnectionConfig.Builder connConfigBuilder = ConnectionConfig.custom();
        if (connettore.getConnectionTimeout() != null) {
            connConfigBuilder.setConnectTimeout(Timeout.ofMilliseconds(connettore.getConnectionTimeout()));
        }

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connConfigBuilder.build())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        if (connettore.getReadTimeout() != null) {
            factory.setConnectionRequestTimeout(connettore.getReadTimeout());
        }
        return factory;
    }

    private void addAuthInterceptor(Connettore connettore, List<ClientHttpRequestInterceptor> interceptors) {
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
                interceptors.add(new CustomHeadersInterceptor(connettore.getHttpHeaderName() + ":" + connettore.getHttpHeaderValue()));
            }
            case OAUTH2_CLIENT_CREDENTIALS -> {
                log.debug("Configurazione OAuth2 per connettore: {}", connettore.getIdConnettore());
                interceptors.add(new OAuth2Interceptor(connettore.getIdConnettore(), connettore, oauth2TokenManager));
            }
            case NONE -> log.debug("Nessuna autenticazione configurata per connettore: {}",
                    connettore.getIdConnettore());
            default -> { /* SSL gestito separatamente */ }
        }
    }

    private void addCommonInterceptors(Connettore connettore, List<ClientHttpRequestInterceptor> interceptors) {
        if (StringUtils.hasText(connettore.getSubscriptionKeyValue())) {
            log.debug("Configurazione Subscription Key per connettore: {}", connettore.getIdConnettore());
            interceptors.add(new SubscriptionKeyInterceptor(connettore.getSubscriptionKeyValue()));
        }
        if (connettore.getCustomHeaders() != null && !connettore.getCustomHeaders().isEmpty()) {
            log.debug("Configurazione {} Custom Headers per connettore: {}",
                    connettore.getCustomHeaders().size(), connettore.getIdConnettore());
            interceptors.add(new GenericCustomHeadersInterceptor(connettore.getCustomHeaders()));
        }
        interceptors.add(new GdeCapturingInterceptor());
        log.debug("Aggiunto GdeCapturingInterceptor per connettore: {}", connettore.getIdConnettore());
    }

    private RestTemplate createSslRestTemplate(Connettore connettore,
            List<ClientHttpRequestInterceptor> interceptors) {
        log.debug("Configurazione SSL/TLS per connettore: {}", connettore.getIdConnettore());
        try {
            HttpComponentsClientHttpRequestFactory factory = createSslRequestFactory(connettore);
            RestTemplate restTemplate = new RestTemplate(factory);

            addCommonInterceptors(connettore, interceptors);
            restTemplate.setInterceptors(interceptors);
            configureObjectMapper(restTemplate);
            return restTemplate;
        } catch (Exception e) {
            log.error("Errore durante la configurazione SSL per connettore: {}",
                    connettore.getIdConnettore(), e);
            throw new SslConfigurationException("Errore configurazione SSL per connettore: "
                    + connettore.getIdConnettore(), e);
        }
    }

    @SuppressWarnings("removal")
    private void configureObjectMapper(RestTemplate restTemplate) {
        restTemplate.getMessageConverters().removeIf(
                c -> c instanceof org.springframework.http.converter.json.MappingJackson2HttpMessageConverter);
        if (objectMapper instanceof tools.jackson.databind.json.JsonMapper jsonMapper) {
            restTemplate.getMessageConverters().add(new JacksonJsonHttpMessageConverter(jsonMapper));
        }
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
        TlsSocketStrategy tlsStrategy = new DefaultClientTlsStrategy(
                sslContext, NoopHostnameVerifier.INSTANCE);

        HttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofMilliseconds(connettore.getConnectionTimeout()))
                        .build())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
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
        private final String connettoreKey;
        private final Connettore connettore;
        private final Oauth2ClientCredentialsManager tokenManager;

        public OAuth2Interceptor(String connettoreKey, Connettore connettore,
                Oauth2ClientCredentialsManager tokenManager) {
            this.connettoreKey = connettoreKey;
            this.connettore = connettore;
            this.tokenManager = tokenManager;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                ClientHttpRequestExecution execution) throws IOException {
            String accessToken = tokenManager.getAccessToken(connettoreKey, connettore);
            request.getHeaders().setBearerAuth(accessToken);
            return execution.execute(request, body);
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

    public static class SslConfigurationException extends RuntimeException {
    	
        private static final long serialVersionUID = 1L;

		public SslConfigurationException(String message, Throwable cause) { 
            super(message, cause);
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
