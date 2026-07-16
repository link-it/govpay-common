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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingFilterBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.filter.RequestContextFilter;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Auto-configurazione delle metriche custom GovPay per applicazioni web
 * servlet con Micrometer attivo (tipicamente via actuator):
 *
 * <ul>
 *   <li>{@link ExternalCallMetricsRecorder} — timer
 *       {@code govpay.external.service.duration} per le chiamate a servizi
 *       esterni, con esito classificato da {@link ExternalCallOutcomeRegistry};</li>
 *   <li>{@link ApiTimingMetricsFilter} — breakdown per request
 *       {@code govpay.api.internal/external.duration};</li>
 *   <li>{@link ExternalCallMetricsContext} — accumulatore request-scoped
 *       che collega i due.</li>
 * </ul>
 *
 * <p>Disattivabile con {@code govpay.metrics.enabled=false}. Nelle
 * applicazioni non web (batch senza server, tool) o senza MeterRegistry non
 * si attiva nulla.
 */
@AutoConfiguration(
        afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
        before = WebMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "govpay.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class GovpayMetricsAutoConfiguration {

    @Bean
    @RequestScope
    public ExternalCallMetricsContext externalCallMetricsContext() {
        return new ExternalCallMetricsContext();
    }

    @Bean
    public ExternalCallMetricsRecorder externalCallMetricsRecorder(
            ObjectProvider<ExternalCallMetricsContext> contextProvider, MeterRegistry meterRegistry) {
        return new ExternalCallMetricsRecorder(contextProvider, meterRegistry);
    }

    @Bean
    public FilterRegistrationBean<ApiTimingMetricsFilter> apiTimingMetricsFilterRegistration(
            ObjectProvider<ExternalCallMetricsContext> contextProvider, MeterRegistry meterRegistry) {
        FilterRegistrationBean<ApiTimingMetricsFilter> registration = new FilterRegistrationBean<>(
                new ApiTimingMetricsFilter(contextProvider, meterRegistry));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

    /**
     * Lega la request al thread corrente cosi' che il bean
     * {@code @RequestScope} sia risolvibile dai filtri e dal recorder.
     * Registrato subito prima di {@link ApiTimingMetricsFilter}.
     * <p>
     * {@code before = WebMvcAutoConfiguration.class} sulla classe e'
     * load-bearing: il {@code RequestContextFilter} di default di Spring Boot
     * (dentro {@code WebMvcAutoConfiguration}, {@code @ConditionalOnMissingBean})
     * va registrato con ordine molto anticipato (circa -105) se questa
     * autoconfigurazione viene valutata dopo — a quell'ordine il filtro non
     * risulta risolvibile dai filtri applicativi che girano su ordini piu'
     * alti. Se questa autoconfigurazione gira prima, il proprio bean vince
     * la corsa e quello di default di Boot si ritira correttamente.
     */
    @Bean
    @ConditionalOnMissingFilterBean(RequestContextFilter.class)
    public FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>(
                new RequestContextFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 15);
        return registration;
    }
}
