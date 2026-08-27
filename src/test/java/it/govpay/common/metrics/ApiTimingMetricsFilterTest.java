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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.DispatcherType;

class ApiTimingMetricsFilterTest {

    @Test
    void recordsApiTimingOnRequestDispatch() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ExternalCallMetricsContext context = new ExternalCallMetricsContext();
        context.addExternalNanos(1_000L);
        ApiTimingMetricsFilter filter = new ApiTimingMetricsFilter(provider(context), registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.setDispatcherType(DispatcherType.REQUEST);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/test");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(registry.find("govpay.api.external.duration").timer()).isNotNull();
        assertThat(registry.find("govpay.api.internal.duration").timer()).isNotNull();
    }

    @Test
    void skipsErrorDispatchToAvoidDoubleCountingSameRequest() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ExternalCallMetricsContext context = new ExternalCallMetricsContext();
        context.addExternalNanos(1_000L);
        ApiTimingMetricsFilter filter = new ApiTimingMetricsFilter(provider(context), registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setDispatcherType(DispatcherType.ERROR);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(registry.find("govpay.api.external.duration").timer()).isNull();
        assertThat(registry.find("govpay.api.internal.duration").timer()).isNull();
    }

    @Test
    void normalizesUnknownHttpMethodsToBoundCardinality() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiTimingMetricsFilter filter = new ApiTimingMetricsFilter(provider(new ExternalCallMetricsContext()), registry);

        MockHttpServletRequest request = new MockHttpServletRequest("BOGUS-METHOD", "/test");
        request.setDispatcherType(DispatcherType.REQUEST);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/test");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(registry.find("govpay.api.internal.duration")
                .tag("method", "OTHER")
                .tag("uri", "/test")
                .timer()).isNotNull();
        assertThat(registry.find("govpay.api.internal.duration")
                .tag("method", "BOGUS-METHOD")
                .timer()).isNull();
    }

    @Test
    void recordsServerErrorWhenUnhandledErrorLeavesResponseStatusAtDefault200() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApiTimingMetricsFilter filter = new ApiTimingMetricsFilter(provider(new ExternalCallMetricsContext()), registry);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
        request.setDispatcherType(DispatcherType.REQUEST);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/test");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> {
                    throw new AssertionError("boom");
                })).isInstanceOf(AssertionError.class);

        assertThat(registry.find("govpay.api.internal.duration")
                .tag("status", "500")
                .tag("outcome", "SERVER_ERROR")
                .timer()).isNotNull();
    }

    private static ObjectProvider<ExternalCallMetricsContext> provider(ExternalCallMetricsContext context) {
        @SuppressWarnings("unchecked")
        ObjectProvider<ExternalCallMetricsContext> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(context);
        return provider;
    }
}
