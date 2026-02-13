package it.govpay.common.client.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.web.client.RestTemplate;

import it.govpay.common.client.gde.GdeCapturingInterceptor;
import it.govpay.common.client.model.Connettore;
import it.govpay.common.client.oauth2.Oauth2ClientCredentialsManager;
import it.govpay.common.entity.TipoAutenticazione;

class RestTemplateFactoryTest {

    private RestTemplateFactory factory;
    private Oauth2ClientCredentialsManager mockOauth2Manager;

    // GdeCapturingInterceptor is always added to all RestTemplates
    private static final int GDE_INTERCEPTOR_COUNT = 1;

    @BeforeEach
    void setUp() {
        mockOauth2Manager = mock(Oauth2ClientCredentialsManager.class);
        factory = new RestTemplateFactory(mockOauth2Manager);
    }

    @Test
    void testCreateRestTemplate_BasicAuth() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_BASIC")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.HTTP_BASIC)
                .httpUser("testuser")
                .httpPassw("testpass")
                .connectionTimeout(5000)
                .readTimeout(30000)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertEquals("https://api.test.com/", restTemplate.getUriTemplateHandler().expand("/").toString());
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // 1 BasicAuth + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
        assertTrue(restTemplate.getInterceptors().stream().anyMatch(GdeCapturingInterceptor.class::isInstance));
    }

    @Test
    void testCreateRestTemplate_ApiKey() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_APIKEY")
                .url("https://api.key.com")
                .tipoAutenticazione(TipoAutenticazione.API_KEY)
                .apiKey("test-api-key")
                .apiId("X-API-Key")
                .connectionTimeout(5000)
                .readTimeout(30000)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // 1 ApiKey + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_ApiKey_DefaultHeaderName() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_APIKEY_DEFAULT")
                .url("https://api.key.com")
                .tipoAutenticazione(TipoAutenticazione.API_KEY)
                .apiKey("test-api-key")
                .apiId(null) // Should default to X-API-Key
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
    }

    @Test
    void testCreateRestTemplate_HttpHeader() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_HEADER")
                .url("https://api.header.com")
                .tipoAutenticazione(TipoAutenticazione.HTTP_HEADER)
                .httpHeaderName("X-Custom-Auth")
                .httpHeaderValue("secret-value")
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // 1 HttpHeader + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_OAuth2() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_OAUTH2")
                .url("https://api.oauth.com")
                .tipoAutenticazione(TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS)
                .oauth2ClientCredentialsClientId("client-id")
                .oauth2ClientCredentialsClientSecret("client-secret")
                .oauth2ClientCredentialsUrlTokenEndpoint("https://auth.com/token")
                .oauth2ClientCredentialsScope("read write")
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // 1 OAuth2 + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_None() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_NONE")
                .url("https://api.none.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Only GdeCapturing interceptor
        assertEquals(GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
        assertTrue(restTemplate.getInterceptors().stream().anyMatch(GdeCapturingInterceptor.class::isInstance));
    }

    @Test
    void testCreateRestTemplate_WithSubscriptionKey() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_AZURE")
                .url("https://api.azure.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .subscriptionKeyValue("azure-subscription-key")
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // 1 SubscriptionKey + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_WithCustomHeaders() {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Api-Version", "2.0");
        customHeaders.put("X-Trace-Id", "trace-123");
        customHeaders.put("X-Client", "GovPay");

        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_CUSTOM")
                .url("https://api.custom.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .customHeaders(customHeaders)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // 1 CustomHeaders + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_Combined() {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Partner-Id", "PARTNER_001");

        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_COMBINED")
                .url("https://api.combined.com")
                .tipoAutenticazione(TipoAutenticazione.API_KEY)
                .apiKey("api-key-123")
                .apiId("X-API-Key")
                .subscriptionKeyValue("azure-key")
                .customHeaders(customHeaders)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // Should have 3 interceptors: API Key + Subscription Key + Custom Headers + GdeCapturing
        assertEquals(3 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_MultipleCustomHeaders() {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Header-1", "value-1");
        customHeaders.put("X-Header-2", "value-2");
        customHeaders.put("X-Header-3", "value-3");

        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_MULTI_HEADERS")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .customHeaders(customHeaders)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // 1 CustomHeaders + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_Timeouts() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_TIMEOUTS")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .connectionTimeout(10000)
                .readTimeout(60000)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Timeouts are set in RestTemplateBuilder, verificato dalla creazione senza eccezioni
    }

    @Test
    void testCreateRestTemplate_HttpHeaderWithMultipleValues() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_MULTI_HEADER")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.HTTP_HEADER)
                .httpHeaderName("X-Header-1")
                .httpHeaderValue("value-1")
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
    }

    @Test
    void testCreateRestTemplate_BasicAuthAndSubscriptionKey() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_BASIC_AZURE")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.HTTP_BASIC)
                .httpUser("user")
                .httpPassw("pass")
                .subscriptionKeyValue("azure-key")
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Should have 2 interceptors: Basic Auth + Subscription Key + GdeCapturing
        assertEquals(2 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_AllFeaturesCombined() {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Partner", "PARTNER_001");
        customHeaders.put("X-Request-Source", "GovPay");

        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_ALL")
                .url("https://api.all.com")
                .tipoAutenticazione(TipoAutenticazione.HTTP_BASIC)
                .httpUser("testuser")
                .httpPassw("testpass")
                .subscriptionKeyValue("azure-key")
                .customHeaders(customHeaders)
                .connectionTimeout(8000)
                .readTimeout(45000)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Should have 3 interceptors: Basic Auth + Subscription Key + Custom Headers + GdeCapturing
        assertEquals(3 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_NullTimeouts() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_NULL_TIMEOUTS")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .connectionTimeout(null)
                .readTimeout(null)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Should create RestTemplate without exception when timeouts are null
    }

    @Test
    void testCreateRestTemplate_EmptyCustomHeaders() {
        Map<String, String> emptyHeaders = new HashMap<>();

        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_EMPTY_HEADERS")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .customHeaders(emptyHeaders)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Should not add interceptor for empty custom headers, only GdeCapturing
        assertEquals(GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_NullSubscriptionKey() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_NULL_SUBSCRIPTION")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .subscriptionKeyValue(null)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Only GdeCapturing
        assertEquals(GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_BlankSubscriptionKey() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_BLANK_SUBSCRIPTION")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .subscriptionKeyValue("   ")
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // Blank subscription key should not add interceptor, only GdeCapturing
        assertEquals(GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_OAuth2WithAllFields() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_OAUTH2_FULL")
                .url("https://api.oauth.com")
                .tipoAutenticazione(TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS)
                .oauth2ClientCredentialsClientId("client-id-123")
                .oauth2ClientCredentialsClientSecret("client-secret-456")
                .oauth2ClientCredentialsUrlTokenEndpoint("https://auth.example.com/oauth2/token")
                .oauth2ClientCredentialsScope("read write admin")
                .connectionTimeout(10000)
                .readTimeout(30000)
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // 1 OAuth2 + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    void testCreateRestTemplate_HttpHeaderWithSemicolonSeparator() {
        // Test HTTP_HEADER auth with header value containing special characters
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_HEADER_SPECIAL")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.HTTP_HEADER)
                .httpHeaderName("Authorization")
                .httpHeaderValue("Bearer token-with-special-chars:123")
                .build();

        RestTemplate restTemplate = factory.createRestTemplate(connettore);

        assertNotNull(restTemplate);
        // 1 HttpHeader + 1 GdeCapturing
        assertEquals(1 + GDE_INTERCEPTOR_COUNT, restTemplate.getInterceptors().size());
    }

    @Test
    @DisplayName("SSL con file non esistente lancia RuntimeException")
    void testCreateRestTemplate_SSL_FileNotFound() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST_SSL_BAD")
                .url("https://api.test.com")
                .tipoAutenticazione(TipoAutenticazione.SSL)
                .sslKsLocation("/non/existent/keystore.p12")
                .sslKsPasswd("password")
                .sslKsType("PKCS12")
                .connectionTimeout(5000)
                .readTimeout(30000)
                .build();

        assertThrows(RuntimeException.class, () -> factory.createRestTemplate(connettore));
    }

    @Nested
    @DisplayName("Interceptor Execution")
    class InterceptorExecution {

        private ClientHttpResponse mockResponse;
        private ClientHttpRequestExecution mockExecution;

        @BeforeEach
        void setUpInterceptorTests() throws IOException {
            mockResponse = mock(ClientHttpResponse.class);
            mockExecution = mock(ClientHttpRequestExecution.class);
            when(mockExecution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(mockResponse);
        }

        @Test
        @DisplayName("BasicAuth interceptor aggiunge header Authorization")
        void basicAuthInterceptor() throws IOException {
            Connettore connettore = Connettore.builder()
                    .idConnettore("TEST_BASIC_EXEC")
                    .url("https://api.test.com")
                    .tipoAutenticazione(TipoAutenticazione.HTTP_BASIC)
                    .httpUser("user")
                    .httpPassw("pass")
                    .build();

            RestTemplate restTemplate = factory.createRestTemplate(connettore);
            ClientHttpRequestInterceptor interceptor = findNonGdeInterceptor(restTemplate);

            MockClientHttpRequest request = new MockClientHttpRequest();
            interceptor.intercept(request, new byte[0], mockExecution);

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            assertNotNull(authHeader);
            assertTrue(authHeader.startsWith("Basic "));
        }

        @Test
        @DisplayName("ApiKey interceptor aggiunge header API Key")
        void apiKeyInterceptor() throws IOException {
            Connettore connettore = Connettore.builder()
                    .idConnettore("TEST_APIKEY_EXEC")
                    .url("https://api.test.com")
                    .tipoAutenticazione(TipoAutenticazione.API_KEY)
                    .apiKey("my-api-key-value")
                    .apiId("X-Custom-Api-Key")
                    .build();

            RestTemplate restTemplate = factory.createRestTemplate(connettore);
            ClientHttpRequestInterceptor interceptor = findNonGdeInterceptor(restTemplate);

            MockClientHttpRequest request = new MockClientHttpRequest();
            interceptor.intercept(request, new byte[0], mockExecution);

            assertEquals("my-api-key-value", request.getHeaders().getFirst("X-Custom-Api-Key"));
        }

        @Test
        @DisplayName("HttpHeader interceptor aggiunge headers con separatore ;")
        void httpHeaderInterceptor() throws IOException {
            Connettore connettore = Connettore.builder()
                    .idConnettore("TEST_HEADER_EXEC")
                    .url("https://api.test.com")
                    .tipoAutenticazione(TipoAutenticazione.HTTP_HEADER)
                    .httpHeaderName("X-Auth")
                    .httpHeaderValue("secret;X-Extra:extra-value")
                    .build();

            RestTemplate restTemplate = factory.createRestTemplate(connettore);
            ClientHttpRequestInterceptor interceptor = findNonGdeInterceptor(restTemplate);

            MockClientHttpRequest request = new MockClientHttpRequest();
            interceptor.intercept(request, new byte[0], mockExecution);

            // The CustomHeadersInterceptor splits by ; then by :
            // "X-Auth:secret" and "X-Extra:extra-value"
            assertEquals("secret", request.getHeaders().getFirst("X-Auth"));
            assertEquals("extra-value", request.getHeaders().getFirst("X-Extra"));
        }

        @Test
        @DisplayName("OAuth2 interceptor aggiunge header Bearer tramite Oauth2ClientCredentialsManager")
        void oauth2Interceptor() throws IOException {
            when(mockOauth2Manager.getAccessToken(anyString(), any(Connettore.class)))
                    .thenReturn("mock-oauth2-token");

            Connettore connettore = Connettore.builder()
                    .idConnettore("TEST_OAUTH2_EXEC")
                    .url("https://api.test.com")
                    .tipoAutenticazione(TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS)
                    .oauth2ClientCredentialsClientId("client-id")
                    .oauth2ClientCredentialsClientSecret("client-secret")
                    .oauth2ClientCredentialsUrlTokenEndpoint("https://auth.com/token")
                    .build();

            RestTemplate restTemplate = factory.createRestTemplate(connettore);
            ClientHttpRequestInterceptor interceptor = findNonGdeInterceptor(restTemplate);

            MockClientHttpRequest request = new MockClientHttpRequest();
            interceptor.intercept(request, new byte[0], mockExecution);

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            assertNotNull(authHeader);
            assertEquals("Bearer mock-oauth2-token", authHeader);
            verify(mockOauth2Manager).getAccessToken("TEST_OAUTH2_EXEC", connettore);
        }

        @Test
        @DisplayName("SubscriptionKey interceptor aggiunge header Ocp-Apim-Subscription-Key")
        void subscriptionKeyInterceptor() throws IOException {
            Connettore connettore = Connettore.builder()
                    .idConnettore("TEST_SUB_EXEC")
                    .url("https://api.test.com")
                    .tipoAutenticazione(TipoAutenticazione.NONE)
                    .subscriptionKeyValue("my-subscription-key")
                    .build();

            RestTemplate restTemplate = factory.createRestTemplate(connettore);
            // Find the SubscriptionKey interceptor (not GdeCapturing)
            ClientHttpRequestInterceptor interceptor = findNonGdeInterceptor(restTemplate);

            MockClientHttpRequest request = new MockClientHttpRequest();
            interceptor.intercept(request, new byte[0], mockExecution);

            assertEquals("my-subscription-key", request.getHeaders().getFirst("Ocp-Apim-Subscription-Key"));
        }

        @Test
        @DisplayName("GenericCustomHeaders interceptor aggiunge tutti gli headers custom")
        void genericCustomHeadersInterceptor() throws IOException {
            Map<String, String> customHeaders = new HashMap<>();
            customHeaders.put("X-Header-A", "value-a");
            customHeaders.put("X-Header-B", "value-b");

            Connettore connettore = Connettore.builder()
                    .idConnettore("TEST_GENERIC_EXEC")
                    .url("https://api.test.com")
                    .tipoAutenticazione(TipoAutenticazione.NONE)
                    .customHeaders(customHeaders)
                    .build();

            RestTemplate restTemplate = factory.createRestTemplate(connettore);
            ClientHttpRequestInterceptor interceptor = findNonGdeInterceptor(restTemplate);

            MockClientHttpRequest request = new MockClientHttpRequest();
            interceptor.intercept(request, new byte[0], mockExecution);

            assertEquals("value-a", request.getHeaders().getFirst("X-Header-A"));
            assertEquals("value-b", request.getHeaders().getFirst("X-Header-B"));
        }

        private ClientHttpRequestInterceptor findNonGdeInterceptor(RestTemplate restTemplate) {
            return restTemplate.getInterceptors().stream()
                    .filter(i -> !(i instanceof GdeCapturingInterceptor))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("No non-GDE interceptor found"));
        }
    }
}
