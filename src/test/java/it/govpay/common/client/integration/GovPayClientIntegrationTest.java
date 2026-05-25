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
package it.govpay.common.client.integration;

import it.govpay.common.entity.ConnettoreEntity;
import it.govpay.common.entity.TipoAutenticazione;
import it.govpay.common.client.model.Connettore;
import it.govpay.common.repository.ConnettoreEntityRepository;
import it.govpay.common.client.service.ConnettoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test end-to-end che testa l'intero flusso:
 * Database → Repository → Converter → Service → Factory → RestTemplate
 */
@SpringBootTest
@ActiveProfiles("test")
class GovPayClientIntegrationTest {

    @Autowired
    private ConnettoreEntityRepository repository;

    @Autowired
    private ConnettoreService connettoreService;

    @Test
    void testEndToEnd_BasicAuth() {
        // 1. Verifica dati nel database
        List<ConnettoreEntity> entities = repository.findByCodConnettore("TEST_BASIC");
        assertNotNull(entities);
        assertFalse(entities.isEmpty());
        assertTrue(entities.stream().anyMatch(e -> "URL".equals(e.getCodProprieta())));
        assertTrue(entities.stream().anyMatch(e -> "HTTPUSER".equals(e.getCodProprieta())));

        // 2. Carica connettore tramite service
        Connettore connettore = connettoreService.getConnettore("TEST_BASIC");
        assertNotNull(connettore);
        assertEquals("TEST_BASIC", connettore.getIdConnettore());
        assertEquals(TipoAutenticazione.HTTP_BASIC, connettore.getTipoAutenticazione());

        // 3. Ottieni RestTemplate configurato
        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_BASIC");
        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());

        // 4. Verifica che l'interceptor Basic Auth sia configurato (+ GdeCapturingInterceptor)
        assertEquals(2, restTemplate.getInterceptors().size());

        // 5. Test interceptor con mock request (first interceptor is BasicAuth, GdeCapturing is last)
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com"));

        try {
            interceptor.intercept(mockRequest, new byte[0], (request, body) -> null);
            // Verifica che l'header Authorization sia stato aggiunto
            assertTrue(mockRequest.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION));
            String authHeader = mockRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            assertNotNull(authHeader);
            assertTrue(authHeader.startsWith("Basic "));
        } catch (IOException e) {
            // Expected - mock execution returns null
        }
    }

    @Test
    void testEndToEnd_ApiKey() {
        // Carica e verifica connettore API Key
        Connettore connettore = connettoreService.getConnettore("TEST_APIKEY");
        assertNotNull(connettore);
        assertEquals(TipoAutenticazione.API_KEY, connettore.getTipoAutenticazione());

        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_APIKEY");
        assertNotNull(restTemplate);
        // 1 ApiKey + 1 GdeCapturing
        assertEquals(2, restTemplate.getInterceptors().size());

        // Test interceptor (first is ApiKey, GdeCapturing is last)
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com"));

        try {
            interceptor.intercept(mockRequest, new byte[0], (request, body) -> null);
            assertTrue(mockRequest.getHeaders().containsHeader("X-API-Key"));
            assertEquals("test-api-key-123", mockRequest.getHeaders().getFirst("X-API-Key"));
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    void testEndToEnd_CustomHeaders() {
        // Carica connettore con custom headers
        Connettore connettore = connettoreService.getConnettore("TEST_CUSTOM_HEADERS");
        assertNotNull(connettore);
        assertNotNull(connettore.getCustomHeaders());
        assertEquals(2, connettore.getCustomHeaders().size());

        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_CUSTOM_HEADERS");
        assertNotNull(restTemplate);
        // 1 CustomHeaders + 1 GdeCapturing
        assertEquals(2, restTemplate.getInterceptors().size());

        // Test interceptor (first is CustomHeaders, GdeCapturing is last)
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com"));

        try {
            interceptor.intercept(mockRequest, new byte[0], (request, body) -> null);
            assertTrue(mockRequest.getHeaders().containsHeader("X-Api-Version"));
            assertTrue(mockRequest.getHeaders().containsHeader("X-Trace-Id"));
            assertEquals("2.0", mockRequest.getHeaders().getFirst("X-Api-Version"));
            assertEquals("test-trace", mockRequest.getHeaders().getFirst("X-Trace-Id"));
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    void testEndToEnd_AzureSubscriptionKey() {
        Connettore connettore = connettoreService.getConnettore("TEST_AZURE");
        assertNotNull(connettore);
        assertEquals("test-subscription-key", connettore.getSubscriptionKeyValue());

        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_AZURE");
        assertNotNull(restTemplate);
        // 1 SubscriptionKey + 1 GdeCapturing
        assertEquals(2, restTemplate.getInterceptors().size());

        // Test interceptor (first is SubscriptionKey, GdeCapturing is last)
        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com"));

        try {
            interceptor.intercept(mockRequest, new byte[0], (request, body) -> null);
            assertTrue(mockRequest.getHeaders().containsHeader("Ocp-Apim-Subscription-Key"));
            assertEquals("test-subscription-key",
                    mockRequest.getHeaders().getFirst("Ocp-Apim-Subscription-Key"));
        } catch (IOException e) {
            // Expected
        }
    }

    @Test
    void testEndToEnd_Combined() {
        // Test connettore combinato: API Key + Subscription Key + Custom Headers
        Connettore connettore = connettoreService.getConnettore("TEST_COMBINED");
        assertNotNull(connettore);
        assertEquals(TipoAutenticazione.API_KEY, connettore.getTipoAutenticazione());
        assertEquals("combined-api-key", connettore.getApiKey());
        assertEquals("combined-subscription", connettore.getSubscriptionKeyValue());
        assertNotNull(connettore.getCustomHeaders());
        assertEquals(1, connettore.getCustomHeaders().size());

        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_COMBINED");
        assertNotNull(restTemplate);
        // Should have 3 interceptors: API Key + Subscription Key + Custom Headers + GdeCapturing
        assertEquals(4, restTemplate.getInterceptors().size());

        // Test tutti gli interceptors (GdeCapturing is last but does not add headers)
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://test.com"));
        for (ClientHttpRequestInterceptor interceptor : restTemplate.getInterceptors()) {
            try {
                interceptor.intercept(mockRequest, new byte[0], (request, body) -> null);
            } catch (IOException e) {
                // Expected
            }
        }

        // Verifica che tutti gli header siano stati aggiunti
        assertTrue(mockRequest.getHeaders().containsHeader("X-API-Key"));
        assertTrue(mockRequest.getHeaders().containsHeader("Ocp-Apim-Subscription-Key"));
        assertTrue(mockRequest.getHeaders().containsHeader("X-Partner-Id"));
        assertEquals("combined-api-key", mockRequest.getHeaders().getFirst("X-API-Key"));
        assertEquals("combined-subscription",
                mockRequest.getHeaders().getFirst("Ocp-Apim-Subscription-Key"));
        assertEquals("PARTNER_001", mockRequest.getHeaders().getFirst("X-Partner-Id"));
    }

    @Test
    void testEndToEnd_DatabaseToRestTemplate_AllTypes() {
        // Test che tutti i tipi di autenticazione carichino correttamente
        String[] connectors = {
                "TEST_BASIC",
                "TEST_APIKEY",
                "TEST_CUSTOM_HEADERS",
                "TEST_AZURE",
                "TEST_HTTP_HEADER",
                "TEST_OAUTH2",
                "TEST_COMBINED",
                "TEST_NONE"
        };

        for (String codConnettore : connectors) {
            // Carica dal database
            List<ConnettoreEntity> entities = repository.findByCodConnettore(codConnettore);
            assertFalse(entities.isEmpty(), "Entities not found for " + codConnettore);

            // Converti in model
            Connettore connettore = connettoreService.getConnettore(codConnettore);
            assertNotNull(connettore, "Connettore not loaded for " + codConnettore);

            // Crea RestTemplate
            RestTemplate restTemplate = connettoreService.getRestTemplate(codConnettore);
            assertNotNull(restTemplate, "RestTemplate not created for " + codConnettore);
        }
    }

    @Test
    void testEndToEnd_CacheIntegration() {
        // Test cache disabled
        assertFalse(connettoreService.isCacheEnabled());

        // Load twice
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_BASIC");
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_BASIC");

        // With cache disabled, should be different instances
        assertNotNull(rt1);
        assertNotNull(rt2);
        assertNotSame(rt1, rt2);
    }

    @Test
    void testEndToEnd_FilterDisabledConnectors() {
        // TEST_DISABLED ha ABILITATO=false
        List<Connettore> abilitati = connettoreService.getAllAbilitati();

        assertNotNull(abilitati);
        assertFalse(abilitati.isEmpty());

        // TEST_DISABLED non dovrebbe essere nella lista
        assertTrue(abilitati.stream()
                .noneMatch(c -> "TEST_DISABLED".equals(c.getIdConnettore())));

        // Ma dovrebbe esistere nel database
        List<ConnettoreEntity> entities = repository.findByCodConnettore("TEST_DISABLED");
        assertFalse(entities.isEmpty());
    }
}
