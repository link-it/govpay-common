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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class ExternalCallOutcomeRegistryTest {

    /** Eccezioni fittizie usate SOLO da questa classe di test. */
    private static class RegistryStubCircuitOpenException extends RuntimeException {
    }

    private static class RegistryStubTimeoutException extends RuntimeException {
    }

    private static class RegistryStubIoException extends RuntimeException {
    }

    @Test
    void baselineTimeoutClassesClassifyAsTimeout() {
        assertEquals("timeout", ExternalCallOutcomeRegistry.classify(new SocketTimeoutException("read")));
        assertEquals("timeout", ExternalCallOutcomeRegistry.classify(
                new RuntimeException(new HttpTimeoutException("deadline"))));
    }

    @Test
    void baselineIoErrorClassifiesAsIoError() {
        assertEquals("io_error", ExternalCallOutcomeRegistry.classify(
                new ResourceAccessException("refused", new ConnectException("refused"))));
    }

    @Test
    void timeoutWinsOverIoErrorWhenWrapped() {
        // ResourceAccessException (io_error) che avvolge un SocketTimeoutException:
        // la priorita' deve dare timeout
        assertEquals("timeout", ExternalCallOutcomeRegistry.classify(
                new ResourceAccessException("read timed out", new SocketTimeoutException("read timed out"))));
    }

    @Test
    void unknownExceptionClassifiesAsError() {
        assertEquals("error", ExternalCallOutcomeRegistry.classify(new IllegalStateException("boom")));
    }

    @Test
    void circuitOpenStartsEmptyAndIsPopulatedByRegistration() {
        assertEquals("error", ExternalCallOutcomeRegistry.classify(new RegistryStubCircuitOpenException()));

        ExternalCallOutcomeRegistry.registerCircuitOpen(RegistryStubCircuitOpenException.class);

        assertEquals("circuit_open",
                ExternalCallOutcomeRegistry.classify(new RegistryStubCircuitOpenException()));
        // priorita': circuit_open vince anche se nella catena c'e' un timeout
        RuntimeException wrapped = new RegistryStubCircuitOpenException();
        wrapped.initCause(new SocketTimeoutException("late"));
        assertEquals("circuit_open", ExternalCallOutcomeRegistry.classify(wrapped));
    }

    @Test
    void registrationIsAdditiveAndPreservesBaseline() {
        ExternalCallOutcomeRegistry.registerTimeout(RegistryStubTimeoutException.class);
        ExternalCallOutcomeRegistry.registerIoError(RegistryStubIoException.class);

        // le classi registrate classificano
        assertEquals("timeout", ExternalCallOutcomeRegistry.classify(new RegistryStubTimeoutException()));
        assertEquals("io_error", ExternalCallOutcomeRegistry.classify(new RegistryStubIoException()));

        // la baseline resta attiva: register* aggiunge, non sostituisce
        assertEquals("timeout", ExternalCallOutcomeRegistry.classify(new SocketTimeoutException("read")));
        assertEquals("io_error", ExternalCallOutcomeRegistry.classify(
                new ResourceAccessException("refused", new ConnectException("refused"))));
    }

    @Test
    void repeatedRegistrationIsIdempotent() {
        ExternalCallOutcomeRegistry.registerTimeout(RegistryStubTimeoutException.class);
        ExternalCallOutcomeRegistry.registerTimeout(RegistryStubTimeoutException.class);

        assertEquals("timeout", ExternalCallOutcomeRegistry.classify(new RegistryStubTimeoutException()));
    }
}
