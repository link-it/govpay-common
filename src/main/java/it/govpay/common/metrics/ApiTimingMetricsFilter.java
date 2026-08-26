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

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Pubblica il breakdown della request API in tempo esterno
 * ({@code govpay.api.external.duration}, accumulato dal
 * {@link ExternalCallMetricsContext}) e interno
 * ({@code govpay.api.internal.duration}, il resto), con tag allineati a
 * {@code http.server.requests}.
 */
public class ApiTimingMetricsFilter extends OncePerRequestFilter {

    private static final String UNKNOWN = "UNKNOWN";
    private static final String OTHER = "OTHER";
    private static final Set<String> KNOWN_HTTP_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE");

    private final ObjectProvider<ExternalCallMetricsContext> contextProvider;
    private final MeterRegistry meterRegistry;

    public ApiTimingMetricsFilter(ObjectProvider<ExternalCallMetricsContext> contextProvider,
                                  MeterRegistry meterRegistry) {
        this.contextProvider = contextProvider;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (DispatcherType.ERROR.equals(request.getDispatcherType())) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith("/actuator");
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        // Uscita anomala rilevata senza catturare l'eccezione: catturare Error terrebbe in vita
        // una JVM in stato irrecuperabile (SonarCloud java:S1181). Il flag copre qualunque
        // Throwable, quindi anche gli Error vengono contati come fallimento invece che come
        // successo, e la propagazione resta inalterata.
        boolean failed = true;
        try {
            filterChain.doFilter(request, response);
            failed = false;
        } finally {
            long total = System.nanoTime() - start;
            ExternalCallMetricsContext context = contextProvider.getIfAvailable();
            long external = context != null ? context.externalNanos() : 0L;
            long internal = Math.max(0L, total - external);
            int status = status(response, failed);

            Tags tags = Tags.of(
                    "method", method(request),
                    "uri", bestMatchingPattern(request),
                    "status", Integer.toString(status),
                    "outcome", outcome(status));

            meterRegistry.timer("govpay.api.external.duration", tags)
                    .record(external, TimeUnit.NANOSECONDS);
            meterRegistry.timer("govpay.api.internal.duration", tags)
                    .record(internal, TimeUnit.NANOSECONDS);
        }
    }

    private static String bestMatchingPattern(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return pattern.toString();
        }
        return UNKNOWN;
    }

    private static String method(HttpServletRequest request) {
        String method = request.getMethod();
        if (method != null && KNOWN_HTTP_METHODS.contains(method)) {
            return method;
        }
        return OTHER;
    }

    private static int status(HttpServletResponse response, boolean failed) {
        int status = response.getStatus();
        if (failed && status < 400) {
            return 500;
        }
        return status;
    }

    private static String outcome(int status) {
        if (status >= 100 && status < 200) {
            return "INFORMATIONAL";
        }
        if (status >= 200 && status < 300) {
            return "SUCCESS";
        }
        if (status >= 300 && status < 400) {
            return "REDIRECTION";
        }
        if (status >= 400 && status < 500) {
            return "CLIENT_ERROR";
        }
        if (status >= 500 && status < 600) {
            return "SERVER_ERROR";
        }
        return UNKNOWN;
    }
}
