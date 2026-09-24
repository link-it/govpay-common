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
package it.govpay.common.logging.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

import it.govpay.common.logging.CorrelationIdClientInterceptor;
import it.govpay.common.logging.MdcTaskDecorator;
import it.govpay.common.logging.TransactionIdFilter;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto-configurazione della tracciatura Transaction ID / Correlation ID (BP-LOG-3).
 *
 * <p>Registra:
 * <ul>
 *   <li>{@link TransactionIdFilter} — solo nelle applicazioni web servlet, con
 *       ordine molto anticipato cosi' che ogni altro filtro che produce log trovi
 *       gia' popolato l'MDC;</li>
 *   <li>{@link CorrelationIdClientInterceptor} — usato da
 *       {@code RestTemplateFactory} per propagare il correlation id downstream;</li>
 *   <li>{@link MdcTaskDecorator} — usato dagli executor per non perdere il
 *       contesto sui thread di pool.</li>
 * </ul>
 *
 * <p>Attiva di default; si disattiva con
 * {@code govpay.logging.tracciatura.enabled=false}. I due bean non web sono
 * registrati anche nelle applicazioni batch, dove il contesto viene aperto
 * esplicitamente con {@code TransactionContext.apri()}.
 *
 * <p>Perche' gli identificativi compaiano nei log va aggiunto il pattern, ad
 * esempio in {@code application.properties}:
 * <pre>
 * logging.pattern.level=%5p [%X{transactionId:-},%X{correlationId:-}]
 * </pre>
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(LoggingProperties.class)
@ConditionalOnProperty(prefix = "govpay.logging.tracciatura", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(TransactionIdFilter.class)
    public FilterRegistrationBean<TransactionIdFilter> transactionIdFilterRegistration(
            LoggingProperties properties) {
        LoggingProperties.Tracciatura tracciatura = properties.getTracciatura();
        log.info("Tracciatura richieste attiva: correlation id letto da {}, transaction id restituito su {}",
                tracciatura.getCorrelationInboundHeaders(), tracciatura.getTransactionResponseHeaders());
        FilterRegistrationBean<TransactionIdFilter> registration =
                new FilterRegistrationBean<>(new TransactionIdFilter(tracciatura));
        registration.setOrder(tracciatura.getOrdine());
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(CorrelationIdClientInterceptor.class)
    public CorrelationIdClientInterceptor correlationIdClientInterceptor(LoggingProperties properties) {
        return new CorrelationIdClientInterceptor(
                properties.getTracciatura().getCorrelationDownstreamHeaders());
    }

    @Bean
    @ConditionalOnMissingBean(MdcTaskDecorator.class)
    public MdcTaskDecorator mdcTaskDecorator() {
        return new MdcTaskDecorator();
    }
}
