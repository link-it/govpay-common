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

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.ScopeNotActiveException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * Misura operazioni logiche verso servizi esterni e pubblica il timer
 * {@code govpay.external.service.duration} con tag {@code client},
 * {@code operation} e {@code outcome} (classificato da
 * {@link ExternalCallOutcomeRegistry}).
 *
 * <p>Se e' attiva una request HTTP, la durata viene accumulata anche nel
 * {@link ExternalCallMetricsContext} per il breakdown interno/esterno di
 * {@link ApiTimingMetricsFilter}; fuori da una request (es. job batch) resta
 * la sola metrica del servizio esterno. Il timer va usato fuori dai metodi
 * annotati {@code @Retry}, cosi' la durata include tutti i tentativi.
 */
public class ExternalCallMetricsRecorder {

    private final ObjectProvider<ExternalCallMetricsContext> contextProvider;
    private final MeterRegistry meterRegistry;

    public ExternalCallMetricsRecorder(ObjectProvider<ExternalCallMetricsContext> contextProvider,
                                       MeterRegistry meterRegistry) {
        this.contextProvider = contextProvider;
        this.meterRegistry = meterRegistry;
    }

    public void record(String client, String operation, ExternalCall call) {
        long start = System.nanoTime();
        String outcome = "success";
        try {
            call.run();
        } catch (RuntimeException | Error e) {
            outcome = ExternalCallOutcomeRegistry.classify(e);
            throw e;
        } finally {
            long elapsed = System.nanoTime() - start;
            recordDuration(client, operation, outcome, elapsed);
        }
    }

    public void recordDuration(String client, String operation, String outcome, long elapsed) {
        recordInRequestContext(elapsed);
        Tags tags = Tags.of("client", client, "operation", operation, "outcome", outcome);
        meterRegistry.timer("govpay.external.service.duration", tags)
                .record(elapsed, TimeUnit.NANOSECONDS);
    }

    private void recordInRequestContext(long elapsed) {
        try {
            ExternalCallMetricsContext context = contextProvider.getIfAvailable();
            if (context != null) {
                context.record(elapsed);
            }
        } catch (ScopeNotActiveException e) {
            // Chiamata misurata fuori da una request HTTP: resta la metrica del client esterno,
            // ma non c'e' un breakdown API a cui sommare il tempo.
        }
    }

    @FunctionalInterface
    public interface ExternalCall {
        void run();
    }
}
