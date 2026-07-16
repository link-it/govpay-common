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
package it.govpay.common.metrics;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.client.ResourceAccessException;

/**
 * Classifica le eccezioni delle chiamate verso servizi esterni in esiti
 * a bassa cardinalita' per il tag {@code outcome}:
 * {@code circuit_open}, {@code timeout}, {@code io_error}, {@code error}.
 *
 * <p>Le liste di classi riconosciute partono da una baseline:
 * <ul>
 *   <li>{@code circuit_open}: vuota — senza un circuit breaker nel progetto
 *       l'esito non puo' verificarsi;</li>
 *   <li>{@code timeout}: {@link SocketTimeoutException}, {@link HttpTimeoutException};</li>
 *   <li>{@code io_error}: {@link ResourceAccessException}.</li>
 * </ul>
 *
 * <p><b>Contratto del registry</b>: i metodi {@code register*} sono
 * <b>additivi</b> — aggiungono classi alla baseline, che resta sempre attiva
 * e non e' sostituibile ne' rimuovibile. Le registrazioni vanno eseguite allo
 * startup dell'applicazione (es. in una {@code @Configuration} o in un
 * {@code @PostConstruct}), prima che il traffico produca classificazioni.
 * Esempio per un progetto con Resilience4j:
 *
 * <pre>
 * ExternalCallOutcomeRegistry.registerCircuitOpen(CallNotPermittedException.class);
 * </pre>
 *
 * <p>Il match avviene su tutta la catena delle cause con semantica
 * {@code isInstance} (le sottoclassi contano). La priorita' di valutazione e'
 * {@code circuit_open} &gt; {@code timeout} &gt; {@code io_error} &gt;
 * {@code error}: non e' cosmetica, una {@link ResourceAccessException} che
 * avvolge un {@link SocketTimeoutException} deve classificare {@code timeout}.
 */
public final class ExternalCallOutcomeRegistry {

    public static final String OUTCOME_CIRCUIT_OPEN = "circuit_open";
    public static final String OUTCOME_TIMEOUT = "timeout";
    public static final String OUTCOME_IO_ERROR = "io_error";
    public static final String OUTCOME_ERROR = "error";

    private static final Set<Class<? extends Throwable>> CIRCUIT_OPEN = ConcurrentHashMap.newKeySet();
    private static final Set<Class<? extends Throwable>> TIMEOUT = ConcurrentHashMap.newKeySet();
    private static final Set<Class<? extends Throwable>> IO_ERROR = ConcurrentHashMap.newKeySet();

    static {
        TIMEOUT.add(SocketTimeoutException.class);
        TIMEOUT.add(HttpTimeoutException.class);
        IO_ERROR.add(ResourceAccessException.class);
    }

    private ExternalCallOutcomeRegistry() {
    }

    /** Aggiunge una classe di eccezione classificata come {@code circuit_open}. */
    public static void registerCircuitOpen(Class<? extends Throwable> type) {
        CIRCUIT_OPEN.add(Objects.requireNonNull(type, "type"));
    }

    /** Aggiunge una classe di eccezione classificata come {@code timeout}. */
    public static void registerTimeout(Class<? extends Throwable> type) {
        TIMEOUT.add(Objects.requireNonNull(type, "type"));
    }

    /** Aggiunge una classe di eccezione classificata come {@code io_error}. */
    public static void registerIoError(Class<? extends Throwable> type) {
        IO_ERROR.add(Objects.requireNonNull(type, "type"));
    }

    public static String classify(Throwable error) {
        if (containsCause(error, CIRCUIT_OPEN)) {
            return OUTCOME_CIRCUIT_OPEN;
        }
        if (containsCause(error, TIMEOUT)) {
            return OUTCOME_TIMEOUT;
        }
        if (containsCause(error, IO_ERROR)) {
            return OUTCOME_IO_ERROR;
        }
        return OUTCOME_ERROR;
    }

    private static boolean containsCause(Throwable error, Set<Class<? extends Throwable>> types) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            for (Class<? extends Throwable> type : types) {
                if (type.isInstance(current)) {
                    return true;
                }
            }
        }
        return false;
    }
}
