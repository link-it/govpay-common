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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.slf4j.MDC;

/**
 * Facciata sull'MDC per Transaction ID e Correlation ID (BP-LOG-3).
 *
 * <p>Lo stato e' mantenuto nell'MDC di SLF4J, quindi e' per-thread e viene
 * automaticamente incluso nei messaggi di log dal pattern configurato. La classe
 * non introduce un ThreadLocal aggiuntivo: chi deve propagare il contesto su un
 * altro thread usa {@link #wrap(Runnable)}, {@link #wrap(Callable)} oppure la
 * coppia {@link #snapshot()} / {@link #restore(Map)}.
 *
 * <p>Nelle elaborazioni non HTTP (job batch, thread di spedizione, task
 * schedulati) il contesto va aperto esplicitamente:
 * <pre>
 * try (TransactionContext.Scope scope = TransactionContext.apri()) {
 *     // tutti i log qui dentro riportano transactionId e correlationId
 * }
 * </pre>
 */
public final class TransactionContext {

    private TransactionContext() {
        // utility
    }

    /**
     * Genera un nuovo identificativo univoco (UUID v4).
     *
     * @return il nuovo identificativo
     */
    public static String nuovoId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Restituisce il transaction id corrente.
     *
     * @return il transaction id, oppure {@code null} se non impostato
     */
    public static String getTransactionId() {
        return MDC.get(LoggingCostanti.MDC_TRANSACTION_ID);
    }

    /**
     * Restituisce il correlation id corrente.
     *
     * @return il correlation id, oppure {@code null} se non impostato
     */
    public static String getCorrelationId() {
        return MDC.get(LoggingCostanti.MDC_CORRELATION_ID);
    }

    /**
     * Imposta il transaction id nel contesto corrente.
     *
     * @param transactionId l'identificativo di transazione; se {@code null} la chiave viene rimossa
     */
    public static void setTransactionId(String transactionId) {
        put(LoggingCostanti.MDC_TRANSACTION_ID, transactionId);
    }

    /**
     * Imposta il correlation id nel contesto corrente.
     *
     * @param correlationId l'identificativo di correlazione; se {@code null} la chiave viene rimossa
     */
    public static void setCorrelationId(String correlationId) {
        put(LoggingCostanti.MDC_CORRELATION_ID, correlationId);
    }

    private static void put(String chiave, String valore) {
        if (valore == null) {
            MDC.remove(chiave);
        } else {
            MDC.put(chiave, valore);
        }
    }

    /**
     * Rimuove dal contesto corrente entrambi gli identificativi.
     */
    public static void clear() {
        MDC.remove(LoggingCostanti.MDC_TRANSACTION_ID);
        MDC.remove(LoggingCostanti.MDC_CORRELATION_ID);
    }

    /**
     * Apre un nuovo contesto generando sia il transaction id sia il correlation id.
     * Da usare nelle elaborazioni senza richiesta HTTP in ingresso (batch, thread interni).
     *
     * @return lo scope da chiudere al termine dell'elaborazione
     */
    public static Scope apri() {
        return apri(null);
    }

    /**
     * Apre un nuovo contesto generando il transaction id e riusando il correlation id indicato.
     *
     * @param correlationId il correlation id ereditato dal chiamante; se {@code null} ne viene generato uno nuovo
     * @return lo scope da chiudere al termine dell'elaborazione
     */
    public static Scope apri(String correlationId) {
        Map<String, String> precedente = snapshot();
        setTransactionId(nuovoId());
        setCorrelationId(correlationId != null ? correlationId : nuovoId());
        return () -> restore(precedente);
    }

    /**
     * Fotografa l'intera mappa MDC del thread corrente.
     *
     * @return la copia della mappa MDC, eventualmente {@code null} se vuota
     */
    public static Map<String, String> snapshot() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * Ripristina la mappa MDC del thread corrente a partire da uno snapshot.
     *
     * @param contesto lo snapshot da ripristinare; {@code null} azzera l'MDC
     */
    public static void restore(Map<String, String> contesto) {
        if (contesto == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(contesto);
        }
    }

    /**
     * Decora un {@link Runnable} in modo che venga eseguito con il contesto MDC
     * del thread chiamante, ripristinando poi quello del thread esecutore.
     *
     * @param delegate il task da decorare
     * @return il task decorato
     */
    public static Runnable wrap(Runnable delegate) {
        Map<String, String> chiamante = snapshot();
        return () -> {
            Map<String, String> precedente = snapshot();
            restore(chiamante);
            try {
                delegate.run();
            } finally {
                restore(precedente);
            }
        };
    }

    /**
     * Decora un {@link Callable} in modo che venga eseguito con il contesto MDC
     * del thread chiamante, ripristinando poi quello del thread esecutore.
     *
     * @param delegate il task da decorare
     * @param <T> il tipo di ritorno del task
     * @return il task decorato
     */
    public static <T> Callable<T> wrap(Callable<T> delegate) {
        Map<String, String> chiamante = snapshot();
        return () -> {
            Map<String, String> precedente = snapshot();
            restore(chiamante);
            try {
                return delegate.call();
            } finally {
                restore(precedente);
            }
        };
    }

    /**
     * Scope richiudibile che ripristina il contesto MDC precedente.
     */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
