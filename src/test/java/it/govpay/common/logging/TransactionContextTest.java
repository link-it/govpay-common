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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Verifica la gestione del contesto di tracciatura nelle elaborazioni non HTTP e
 * la sua propagazione ai thread di un pool.
 */
class TransactionContextTest {

    @AfterEach
    void pulisciMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("apri() genera entrambi gli identificativi e ripristina il contesto alla chiusura")
    void scopeGeneraERipristina() {
        assertNull(TransactionContext.getTransactionId());

        try (TransactionContext.Scope scope = TransactionContext.apri()) {
            assertNotNull(UUID.fromString(TransactionContext.getTransactionId()));
            assertNotNull(UUID.fromString(TransactionContext.getCorrelationId()));
        }

        assertNull(TransactionContext.getTransactionId());
        assertNull(TransactionContext.getCorrelationId());
    }

    @Test
    @DisplayName("apri(correlationId) eredita il correlation id del chiamante e genera un nuovo transaction id")
    void scopeEreditaCorrelationId() {
        try (TransactionContext.Scope scope = TransactionContext.apri("correlazione-esterna")) {
            assertEquals("correlazione-esterna", TransactionContext.getCorrelationId());
            assertNotNull(UUID.fromString(TransactionContext.getTransactionId()));
        }
    }

    @Test
    @DisplayName("apri() annidato ripristina il contesto esterno alla chiusura")
    void scopeAnnidatoRipristinaIlPrecedente() {
        try (TransactionContext.Scope esterno = TransactionContext.apri("correlazione")) {
            String transactionEsterno = TransactionContext.getTransactionId();

            try (TransactionContext.Scope interno = TransactionContext.apri("altra-correlazione")) {
                assertEquals("altra-correlazione", TransactionContext.getCorrelationId());
            }

            assertEquals(transactionEsterno, TransactionContext.getTransactionId());
            assertEquals("correlazione", TransactionContext.getCorrelationId());
        }
    }

    @Test
    @DisplayName("wrap() porta il contesto sul thread esecutore e lo ripulisce al termine")
    void wrapPropagaIlContesto() throws Exception {
        TransactionContext.setTransactionId("tx-1");
        TransactionContext.setCorrelationId("corr-1");

        AtomicReference<String> transactionNelTask = new AtomicReference<>();
        AtomicReference<String> correlationNelTask = new AtomicReference<>();
        AtomicReference<String> transactionDopoIlTask = new AtomicReference<>();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(TransactionContext.wrap(() -> {
                transactionNelTask.set(TransactionContext.getTransactionId());
                correlationNelTask.set(TransactionContext.getCorrelationId());
            })).get(5, TimeUnit.SECONDS);

            // Il thread del pool non deve trascinare il contesto sul task successivo
            executor.submit(() -> transactionDopoIlTask.set(TransactionContext.getTransactionId()))
                    .get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals("tx-1", transactionNelTask.get());
        assertEquals("corr-1", correlationNelTask.get());
        assertNull(transactionDopoIlTask.get());
    }

    @Test
    @DisplayName("MdcTaskDecorator decora i task con la stessa semantica di wrap()")
    void taskDecoratorPropagaIlContesto() throws Exception {
        TransactionContext.setCorrelationId("corr-decorator");
        AtomicReference<String> visto = new AtomicReference<>();

        Runnable decorato = new MdcTaskDecorator()
                .decorate(() -> visto.set(TransactionContext.getCorrelationId()));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(decorato).get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals("corr-decorator", visto.get());
    }
}
