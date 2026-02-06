package it.govpay.common.client.service;

import it.govpay.common.client.model.Connettore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "govpay.client.cache.enabled=false"
})
class ConnettoreServiceNoCacheTest {

    @Autowired
    private ConnettoreService connettoreService;

    @Test
    void testCacheDisabled() {
        assertFalse(connettoreService.isCacheEnabled());
    }

    @Test
    void testGetConnettore_BasicAuth() {
        Connettore connettore = connettoreService.getConnettore("TEST_BASIC");

        assertNotNull(connettore);
        assertEquals("TEST_BASIC", connettore.getIdConnettore());
        assertEquals("https://api.test-basic.com", connettore.getUrl());
        assertEquals("testuser", connettore.getHttpUser());
        assertEquals("testpass", connettore.getHttpPassw());
        assertTrue(connettore.isAbilitato());
        assertEquals(5000, connettore.getConnectionTimeout());
        assertEquals(30000, connettore.getReadTimeout());
    }

    @Test
    void testGetConnettore_ApiKey() {
        Connettore connettore = connettoreService.getConnettore("TEST_APIKEY");

        assertNotNull(connettore);
        assertEquals("TEST_APIKEY", connettore.getIdConnettore());
        assertEquals("https://api.test-apikey.com", connettore.getUrl());
        assertEquals("test-api-key-123", connettore.getApiKey());
        assertEquals("X-API-Key", connettore.getApiId());
    }

    @Test
    void testGetConnettore_CustomHeaders() {
        Connettore connettore = connettoreService.getConnettore("TEST_CUSTOM_HEADERS");

        assertNotNull(connettore);
        assertEquals("TEST_CUSTOM_HEADERS", connettore.getIdConnettore());
        assertNotNull(connettore.getCustomHeaders());
        assertEquals(2, connettore.getCustomHeaders().size());
        assertEquals("2.0", connettore.getCustomHeaders().get("X-Api-Version"));
        assertEquals("test-trace", connettore.getCustomHeaders().get("X-Trace-Id"));
    }

    @Test
    void testGetConnettore_Azure() {
        Connettore connettore = connettoreService.getConnettore("TEST_AZURE");

        assertNotNull(connettore);
        assertEquals("TEST_AZURE", connettore.getIdConnettore());
        assertEquals("test-subscription-key", connettore.getSubscriptionKeyValue());
    }

    @Test
    void testGetConnettore_Combined() {
        Connettore connettore = connettoreService.getConnettore("TEST_COMBINED");

        assertNotNull(connettore);
        assertEquals("TEST_COMBINED", connettore.getIdConnettore());
        assertEquals("combined-api-key", connettore.getApiKey());
        assertEquals("combined-subscription", connettore.getSubscriptionKeyValue());
        assertNotNull(connettore.getCustomHeaders());
        assertEquals(1, connettore.getCustomHeaders().size());
        assertEquals("PARTNER_001", connettore.getCustomHeaders().get("X-Partner-Id"));
    }

    @Test
    void testGetConnettore_NotFound() {
        assertThrows(RuntimeException.class, () -> {
            connettoreService.getConnettore("NON_EXISTENT");
        });
    }

    @Test
    void testGetRestTemplate_BasicAuth() {
        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_BASIC");

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
    }

    @Test
    void testGetRestTemplate_ApiKey() {
        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_APIKEY");

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
    }

    @Test
    void testGetRestTemplate_CustomHeaders() {
        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_CUSTOM_HEADERS");

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
    }

    @Test
    void testGetRestTemplate_Combined() {
        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_COMBINED");

        assertNotNull(restTemplate);
        assertFalse(restTemplate.getInterceptors().isEmpty());
        // Should have 3 interceptors: API Key + Subscription Key + Custom Headers + GdeCapturingInterceptor
        assertEquals(4, restTemplate.getInterceptors().size());
    }

    @Test
    void testGetRestTemplate_None() {
        RestTemplate restTemplate = connettoreService.getRestTemplate("TEST_NONE");

        assertNotNull(restTemplate);
        // Only GdeCapturingInterceptor
        assertEquals(1, restTemplate.getInterceptors().size());
    }

    @Test
    void testGetAllConnettori() {
        var connettori = connettoreService.getAllConnettori();

        assertNotNull(connettori);
        assertFalse(connettori.isEmpty());
        // From data.sql we have 9 connectors total
        assertTrue(connettori.size() >= 9);
    }

    @Test
    void testGetAllAbilitati() {
        var connettori = connettoreService.getAllAbilitati();

        assertNotNull(connettori);
        assertFalse(connettori.isEmpty());
        // TEST_DISABLED has ABILITATO=false, should not be in this list
        assertTrue(connettori.stream()
                .noneMatch(c -> "TEST_DISABLED".equals(c.getIdConnettore())));
        // All others should be present
        assertTrue(connettori.stream()
                .anyMatch(c -> "TEST_BASIC".equals(c.getIdConnettore())));
    }

    @Test
    void testGetCacheSize_WhenCacheDisabled() {
        // Cache is disabled, size should be 0
        assertEquals(0, connettoreService.getCacheSize());
    }

    @Test
    void testIsInCache_WhenCacheDisabled() {
        // Cache is disabled, nothing should be in cache
        assertFalse(connettoreService.isInCache("TEST_BASIC"));
    }

    @Test
    void testInvalidateCache_WhenCacheDisabled() {
        // Should not throw exception even when cache is disabled
        assertDoesNotThrow(() -> connettoreService.invalidateCache("TEST_BASIC"));
    }

    @Test
    void testRefreshCache_WhenCacheDisabled() {
        // Should not throw exception even when cache is disabled
        assertDoesNotThrow(() -> connettoreService.refreshCache());
    }

    @Test
    void testReloadConnettore_WhenCacheDisabled() {
        // Should not throw exception even when cache is disabled
        assertDoesNotThrow(() -> connettoreService.reloadConnettore("TEST_BASIC"));
    }

    @Test
    void testGetRestTemplate_MultipleCalls_NoCaching() {
        // With cache disabled, each call should create a new instance
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_BASIC");
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_BASIC");

        assertNotNull(rt1);
        assertNotNull(rt2);
        // With cache disabled, these should be different instances
        assertNotSame(rt1, rt2);
    }
}
