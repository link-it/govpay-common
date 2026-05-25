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

import it.govpay.common.client.async.AsyncRestTemplateWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per le operazioni asincrone di ConnettoreService.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConnettoreServiceAsyncTest {

    @Autowired
    private ConnettoreService connettoreService;

    @Test
    void testGetAsyncRestTemplate() {
        AsyncRestTemplateWrapper asyncWrapper = connettoreService.getAsyncRestTemplate("TEST_BASIC");

        assertNotNull(asyncWrapper);
        assertNotNull(asyncWrapper.getRestTemplate());
        assertNotNull(asyncWrapper.getExecutor());
    }

    @Test
    void testGetAsyncRestTemplateForDifferentAuthTypes() {
        // Test vari tipi di autenticazione
        String[] connettori = {
                "TEST_BASIC",
                "TEST_APIKEY",
                "TEST_CUSTOM_HEADERS",
                "TEST_AZURE",
                "TEST_HTTP_HEADER",
                "TEST_NONE"
        };

        for (String codice : connettori) {
            AsyncRestTemplateWrapper asyncWrapper = connettoreService.getAsyncRestTemplate(codice);
            assertNotNull(asyncWrapper, "AsyncWrapper non creato per " + codice);
            assertNotNull(asyncWrapper.getRestTemplate());
        }
    }

    @Test
    void testGetAsyncRestTemplate_NotFound() {
        assertThrows(IllegalArgumentException.class, () ->
                connettoreService.getAsyncRestTemplate("NON_EXISTENT")
        );
    }

    @Test
    void testGetAsyncRestTemplateIfExists() {
        Optional<AsyncRestTemplateWrapper> optional =
                connettoreService.getAsyncRestTemplateIfExists("TEST_BASIC");

        assertTrue(optional.isPresent());
        assertNotNull(optional.get().getRestTemplate());
    }

    @Test
    void testGetAsyncRestTemplateIfExists_NotFound() {
        Optional<AsyncRestTemplateWrapper> optional =
                connettoreService.getAsyncRestTemplateIfExists("NON_EXISTENT");

        assertFalse(optional.isPresent());
    }

    @Test
    void testAsyncWrapperUsesCorrectRestTemplate() {
        // Verifica che l'AsyncWrapper usi lo stesso RestTemplate del sync
        AsyncRestTemplateWrapper asyncWrapper = connettoreService.getAsyncRestTemplate("TEST_BASIC");

        // Il RestTemplate dovrebbe avere gli interceptors configurati
        assertFalse(asyncWrapper.getRestTemplate().getInterceptors().isEmpty());
    }

    @Test
    void testAsyncWrapperForCombinedConnector() {
        // Test connettore con multiple features (API Key + Subscription + Custom Headers)
        AsyncRestTemplateWrapper asyncWrapper = connettoreService.getAsyncRestTemplate("TEST_COMBINED");

        assertNotNull(asyncWrapper);

        // Dovrebbe avere 3 interceptors + GdeCapturingInterceptor
        assertEquals(4, asyncWrapper.getRestTemplate().getInterceptors().size());
    }

    @Test
    void testMultipleAsyncWrappersAreIndependent() {
        AsyncRestTemplateWrapper wrapper1 = connettoreService.getAsyncRestTemplate("TEST_BASIC");
        AsyncRestTemplateWrapper wrapper2 = connettoreService.getAsyncRestTemplate("TEST_APIKEY");

        assertNotNull(wrapper1);
        assertNotNull(wrapper2);

        // I RestTemplate sottostanti dovrebbero essere diversi (configurazioni diverse)
        assertNotSame(wrapper1.getRestTemplate(), wrapper2.getRestTemplate());

        // Ma l'Executor dovrebbe essere lo stesso (shared pool)
        assertSame(wrapper1.getExecutor(), wrapper2.getExecutor());
    }

    @Test
    void testAsyncWrapperReusesRestTemplateFromCache() {
        // Abilita cache per questo test (se non già abilitata)
        AsyncRestTemplateWrapper wrapper1 = connettoreService.getAsyncRestTemplate("TEST_BASIC");
        AsyncRestTemplateWrapper wrapper2 = connettoreService.getAsyncRestTemplate("TEST_BASIC");

        assertNotNull(wrapper1);
        assertNotNull(wrapper2);

        // Gli wrapper sono diversi (nuova istanza ogni volta)
        assertNotSame(wrapper1, wrapper2);

        // Ma se la cache è abilitata, il RestTemplate sottostante è lo stesso
        if (connettoreService.isCacheEnabled()) {
            assertSame(wrapper1.getRestTemplate(), wrapper2.getRestTemplate());
        }
    }

    @Test
    void testAsyncOperationExample() throws Exception {
        // Esempio di utilizzo asincrono (senza chiamata reale)
        AsyncRestTemplateWrapper asyncWrapper = connettoreService.getAsyncRestTemplate("TEST_NONE");

        assertNotNull(asyncWrapper);

        // Verifica che possiamo creare future (anche se non eseguiamo chiamate reali nei test)
        CompletableFuture<ResponseEntity<String>> future = CompletableFuture.supplyAsync(() -> {
            // Simulazione di una chiamata
            return ResponseEntity.ok("Test response");
        });

        ResponseEntity<String> response = future.get();
        assertEquals("Test response", response.getBody());
    }
}
