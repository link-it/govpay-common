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
        "govpay.client.cache.enabled=true"
})
class ConnettoreServiceWithCacheTest {

    @Autowired
    private ConnettoreService connettoreService;

    @Test
    void testCacheEnabled() {
        assertTrue(connettoreService.isCacheEnabled());
    }

    @Test
    void testGetRestTemplate_Caching() {
        // First call should load from DB and cache
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_BASIC");
        assertNotNull(rt1);

        // Second call should return cached instance
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_BASIC");
        assertNotNull(rt2);

        // With cache enabled, these should be the same instance
        assertSame(rt1, rt2);
    }

    @Test
    void testGetCacheSize_AfterLoading() {
        // Initially cache might have items from @PostConstruct
        int initialSize = connettoreService.getCacheSize();

        // Load a new connector
        connettoreService.getRestTemplate("TEST_BASIC");

        // Cache size should increase if it wasn't already loaded
        int newSize = connettoreService.getCacheSize();
        assertTrue(newSize >= initialSize);
        assertTrue(newSize > 0);
    }

    @Test
    void testIsInCache_AfterLoading() {
        // Load the connector
        connettoreService.getRestTemplate("TEST_BASIC");

        // Should now be in cache
        assertTrue(connettoreService.isInCache("TEST_BASIC"));
    }

    @Test
    void testIsInCache_NotLoaded() {
        // Clear any existing cache
        connettoreService.invalidateCache("TEST_APIKEY");

        // Should not be in cache
        assertFalse(connettoreService.isInCache("TEST_APIKEY"));
    }

    @Test
    void testInvalidateCache_RemovesFromCache() {
        // Load the connector
        connettoreService.getRestTemplate("TEST_CUSTOM_HEADERS");
        assertTrue(connettoreService.isInCache("TEST_CUSTOM_HEADERS"));

        // Invalidate
        connettoreService.invalidateCache("TEST_CUSTOM_HEADERS");

        // Should no longer be in cache
        assertFalse(connettoreService.isInCache("TEST_CUSTOM_HEADERS"));
    }

    @Test
    void testInvalidateCache_NextCallReloads() {
        // Load and get first instance
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_AZURE");

        // Invalidate
        connettoreService.invalidateCache("TEST_AZURE");

        // Load again - should be a new instance
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_AZURE");

        assertNotNull(rt1);
        assertNotNull(rt2);
        assertNotSame(rt1, rt2);
    }

    @Test
    void testReloadConnettore_ReplacesInCache() {
        // Load the connector
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_COMBINED");

        // Reload
        connettoreService.reloadConnettore("TEST_COMBINED");

        // Get again - should be a different instance
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_COMBINED");

        assertNotNull(rt1);
        assertNotNull(rt2);
        assertNotSame(rt1, rt2);
    }

    @Test
    void testRefreshCache_ReloadsAll() {
        // Load some connectors
        connettoreService.getRestTemplate("TEST_BASIC");
        connettoreService.getRestTemplate("TEST_APIKEY");
        connettoreService.getCacheSize();

        // Refresh entire cache
        connettoreService.refreshCache();

        // Cache should still have items (reloaded from DB)
        int sizeAfter = connettoreService.getCacheSize();
        assertTrue(sizeAfter > 0);
        // Size might be different if some connectors were not loaded before
        assertTrue(sizeAfter >= 0);
    }

    @Test
    void testGetConnettore_WorksWithCache() {
        Connettore connettore = connettoreService.getConnettore("TEST_HTTP_HEADER");

        assertNotNull(connettore);
        assertEquals("TEST_HTTP_HEADER", connettore.getIdConnettore());
        assertEquals("X-Auth-Token", connettore.getHttpHeaderName());
        assertEquals("secret-token-123", connettore.getHttpHeaderValue());
    }

    @Test
    void testGetAllAbilitati_WorksWithCache() {
        var connettori = connettoreService.getAllAbilitati();

        assertNotNull(connettori);
        assertFalse(connettori.isEmpty());

        // TEST_DISABLED should not be in list
        assertTrue(connettori.stream()
                .noneMatch(c -> "TEST_DISABLED".equals(c.getIdConnettore())));
    }

    @Test
    void testMultipleConnectors_IndependentCaching() {
        // Load multiple connectors
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_BASIC");
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_APIKEY");
        RestTemplate rt3 = connettoreService.getRestTemplate("TEST_AZURE");

        assertNotNull(rt1);
        assertNotNull(rt2);
        assertNotNull(rt3);

        // All should be different instances
        assertNotSame(rt1, rt2);
        assertNotSame(rt2, rt3);
        assertNotSame(rt1, rt3);

        // All should be in cache
        assertTrue(connettoreService.isInCache("TEST_BASIC"));
        assertTrue(connettoreService.isInCache("TEST_APIKEY"));
        assertTrue(connettoreService.isInCache("TEST_AZURE"));

        // Invalidate one
        connettoreService.invalidateCache("TEST_APIKEY");

        // Only that one should be removed
        assertTrue(connettoreService.isInCache("TEST_BASIC"));
        assertFalse(connettoreService.isInCache("TEST_APIKEY"));
        assertTrue(connettoreService.isInCache("TEST_AZURE"));
    }

    @Test
    void testCachePersistence_AcrossMultipleCalls() {
        // Load connector multiple times
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_OAUTH2");
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_OAUTH2");
        RestTemplate rt3 = connettoreService.getRestTemplate("TEST_OAUTH2");

        // All should be the same cached instance
        assertSame(rt1, rt2);
        assertSame(rt2, rt3);
        assertSame(rt1, rt3);
    }

    @Test
    void testInvalidateCache_NonExistentConnector() {
        // Should not throw exception for non-existent connector
        assertDoesNotThrow(() ->
            connettoreService.invalidateCache("NON_EXISTENT")
        );
    }

    @Test
    void testReloadConnettore_NonExistentConnector() {
        // Should throw exception when trying to reload non-existent connector
        assertThrows(RuntimeException.class, () ->
            connettoreService.reloadConnettore("NON_EXISTENT")
        );
    }

    @Test
    void testCacheInitialization_LoadsAbilitati() {
        // After service initialization, cache should contain all enabled connectors
        int cacheSize = connettoreService.getCacheSize();

        // Should have at least some connectors loaded (all ABILITATO=true from data.sql)
        assertTrue(cacheSize > 0);

        // TEST_DISABLED should not be loaded (ABILITATO=false)
        assertFalse(connettoreService.isInCache("TEST_DISABLED"));

        // TEST_BASIC should be loaded (ABILITATO=true)
        assertTrue(connettoreService.isInCache("TEST_BASIC"));
    }

    @Test
    void testGetRestTemplate_AfterInvalidate_LoadsFromDB() {
        // Initial load
        RestTemplate rt1 = connettoreService.getRestTemplate("TEST_NONE");
        assertTrue(connettoreService.isInCache("TEST_NONE"));

        // Invalidate
        connettoreService.invalidateCache("TEST_NONE");
        assertFalse(connettoreService.isInCache("TEST_NONE"));

        // Load again - should work and reload from DB
        RestTemplate rt2 = connettoreService.getRestTemplate("TEST_NONE");
        assertNotNull(rt2);
        assertTrue(connettoreService.isInCache("TEST_NONE"));

        // Should be different instances
        assertNotSame(rt1, rt2);
    }
}
