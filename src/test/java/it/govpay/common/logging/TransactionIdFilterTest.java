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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import it.govpay.common.logging.config.LoggingProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Verifica la semantica di BP-LOG-3 sul filtro di tracciatura: il transaction id
 * e' sempre generato e mai accettato dall'esterno, il correlation id e' riusato se
 * valido, entrambi sono pubblicati nell'MDC e negli header di risposta e l'MDC
 * viene ripulito al termine della richiesta.
 */
class TransactionIdFilterTest {

    private final TransactionIdFilter filter =
            new TransactionIdFilter(new LoggingProperties().getTracciatura());

    @AfterEach
    void pulisciMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("Senza header in ingresso genera transaction id e correlation id distinti")
    void generaEntrambiGliIdentificativi() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        CatturaMdc chain = new CatturaMdc();

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertNotNull(UUID.fromString(chain.transactionId));
        assertNotNull(UUID.fromString(chain.correlationId));
        assertNotEquals(chain.transactionId, chain.correlationId);

        assertEquals(chain.transactionId, response.getHeader(LoggingCostanti.HEADER_TRANSACTION_ID));
        assertEquals(chain.transactionId, response.getHeader(LoggingCostanti.HEADER_TRANSACTION_ID_LEGACY));
        assertEquals(chain.correlationId, response.getHeader(LoggingCostanti.HEADER_CORRELATION_ID));
        assertEquals(chain.correlationId, response.getHeader(LoggingCostanti.HEADER_REQUEST_ID));
    }

    @Test
    @DisplayName("Riusa il correlation id ricevuto su X-Correlation-ID")
    void riusaCorrelationIdDaHeaderStandard() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LoggingCostanti.HEADER_CORRELATION_ID, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CatturaMdc chain = new CatturaMdc();

        filter.doFilter(request, response, chain);

        assertEquals("abc-123", chain.correlationId);
        assertEquals("abc-123", response.getHeader(LoggingCostanti.HEADER_CORRELATION_ID));
    }

    @Test
    @DisplayName("X-Correlation-ID ha la precedenza su X-Request-ID")
    void precedenzaTraHeaderDiIngresso() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LoggingCostanti.HEADER_CORRELATION_ID, "da-correlation");
        request.addHeader(LoggingCostanti.HEADER_REQUEST_ID, "da-request");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CatturaMdc chain = new CatturaMdc();

        filter.doFilter(request, response, chain);

        assertEquals("da-correlation", chain.correlationId);
    }

    @Test
    @DisplayName("Il transaction id non e' mai accettato dall'esterno")
    void transactionIdNonAccettatoDallEsterno() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LoggingCostanti.HEADER_TRANSACTION_ID, "imposto-dal-client");
        request.addHeader(LoggingCostanti.HEADER_TRANSACTION_ID_LEGACY, "imposto-dal-client-legacy");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CatturaMdc chain = new CatturaMdc();

        filter.doFilter(request, response, chain);

        assertNotEquals("imposto-dal-client", chain.transactionId);
        assertNotEquals("imposto-dal-client-legacy", chain.transactionId);
        assertNotNull(UUID.fromString(chain.transactionId));
    }

    @Test
    @DisplayName("Un correlation id con caratteri non ammessi viene scartato")
    void correlationIdNonConformeScartato() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LoggingCostanti.HEADER_CORRELATION_ID, "riga1\nINFO iniezione nei log");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CatturaMdc chain = new CatturaMdc();

        filter.doFilter(request, response, chain);

        assertNotNull(UUID.fromString(chain.correlationId));
        assertEquals(chain.correlationId, response.getHeader(LoggingCostanti.HEADER_CORRELATION_ID));
    }

    @Test
    @DisplayName("Un correlation id piu' lungo del limite viene scartato")
    void correlationIdTroppoLungoScartato() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(LoggingCostanti.HEADER_CORRELATION_ID, "a".repeat(129));
        MockHttpServletResponse response = new MockHttpServletResponse();
        CatturaMdc chain = new CatturaMdc();

        filter.doFilter(request, response, chain);

        assertNotNull(UUID.fromString(chain.correlationId));
    }

    @Test
    @DisplayName("L'MDC viene ripulito anche quando la catena solleva un'eccezione")
    void mdcRipulitoInCasoDiEccezione() {
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) {
                throw new IllegalStateException("errore applicativo");
            }
        };

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
        }

        assertNull(MDC.get(LoggingCostanti.MDC_TRANSACTION_ID));
        assertNull(MDC.get(LoggingCostanti.MDC_CORRELATION_ID));
    }

    private static class CatturaMdc extends MockFilterChain {
        String transactionId;
        String correlationId;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            this.transactionId = MDC.get(LoggingCostanti.MDC_TRANSACTION_ID);
            this.correlationId = MDC.get(LoggingCostanti.MDC_CORRELATION_ID);
        }
    }
}
