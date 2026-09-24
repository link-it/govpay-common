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
package it.govpay.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

/**
 * Verifica la propagazione downstream del correlation id (BP-LOG-3): il
 * transaction id resta interno, gli header gia' valorizzati non vengono
 * sovrascritti e in assenza di contesto la richiesta non viene alterata.
 */
class CorrelationIdClientInterceptorTest {

    private final CorrelationIdClientInterceptor interceptor = new CorrelationIdClientInterceptor();

    @AfterEach
    void pulisciMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Propaga il correlation id sugli header configurati")
    void propagaIlCorrelationId() throws IOException {
        TransactionContext.setCorrelationId("corr-downstream");
        MockClientHttpRequest request = nuovaRichiesta();

        interceptor.intercept(request, new byte[0], esecuzioneFittizia());

        assertEquals("corr-downstream", request.getHeaders().getFirst(LoggingCostanti.HEADER_CORRELATION_ID));
        assertEquals("corr-downstream", request.getHeaders().getFirst(LoggingCostanti.HEADER_REQUEST_ID));
    }

    @Test
    @DisplayName("Non propaga il transaction id, che resta interno al componente")
    void nonPropagaIlTransactionId() throws IOException {
        TransactionContext.setTransactionId("tx-interno");
        TransactionContext.setCorrelationId("corr-downstream");
        MockClientHttpRequest request = nuovaRichiesta();

        interceptor.intercept(request, new byte[0], esecuzioneFittizia());

        assertFalse(request.getHeaders().containsHeader(LoggingCostanti.HEADER_TRANSACTION_ID));
        assertFalse(request.getHeaders().containsHeader(LoggingCostanti.HEADER_TRANSACTION_ID_LEGACY));
    }

    @Test
    @DisplayName("Non sovrascrive un header gia' valorizzato dal connettore")
    void nonSovrascriveHeaderEsistenti() throws IOException {
        TransactionContext.setCorrelationId("corr-downstream");
        MockClientHttpRequest request = nuovaRichiesta();
        request.getHeaders().set(LoggingCostanti.HEADER_CORRELATION_ID, "impostato-dal-connettore");

        interceptor.intercept(request, new byte[0], esecuzioneFittizia());

        assertEquals("impostato-dal-connettore",
                request.getHeaders().getFirst(LoggingCostanti.HEADER_CORRELATION_ID));
        assertEquals("corr-downstream", request.getHeaders().getFirst(LoggingCostanti.HEADER_REQUEST_ID));
    }

    @Test
    @DisplayName("Senza contesto non aggiunge alcun header")
    void senzaContestoNonAggiungeNulla() throws IOException {
        MockClientHttpRequest request = nuovaRichiesta();

        interceptor.intercept(request, new byte[0], esecuzioneFittizia());

        assertNull(request.getHeaders().getFirst(LoggingCostanti.HEADER_CORRELATION_ID));
        assertNull(request.getHeaders().getFirst(LoggingCostanti.HEADER_REQUEST_ID));
    }

    private static MockClientHttpRequest nuovaRichiesta() {
        return new MockClientHttpRequest(HttpMethod.POST, URI.create("https://example.org/eventi"));
    }

    private static ClientHttpRequestExecution esecuzioneFittizia() {
        return new ClientHttpRequestExecution() {
            @Override
            public ClientHttpResponse execute(HttpRequest request, byte[] body) {
                return new MockClientHttpResponse(new byte[0], 200);
            }
        };
    }
}
