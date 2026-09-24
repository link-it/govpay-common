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

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Interceptor che propaga il correlation id sulle chiamate HTTP uscenti (BP-LOG-3).
 *
 * <p>Il valore e' letto dall'MDC del thread che esegue la chiamata: e' quindi
 * quello della richiesta in ingresso quando la chiamata e' sincrona, oppure quello
 * propagato dal {@link MdcTaskDecorator} quando la chiamata e' asincrona.
 *
 * <p>Il <b>transaction id non viene propagato</b>: e' l'identificativo interno del
 * componente. Al componente chiamato compete generare il proprio.
 *
 * <p>Se il correlation id non e' disponibile, oppure un header e' gia' valorizzato
 * dal chiamante (ad esempio da un custom header di connettore), l'interceptor non
 * modifica nulla.
 */
public class CorrelationIdClientInterceptor implements ClientHttpRequestInterceptor {

    private final List<String> headerDaValorizzare;

    /**
     * Costruisce l'interceptor con gli header di propagazione di default.
     */
    public CorrelationIdClientInterceptor() {
        this(LoggingCostanti.DEFAULT_CORRELATION_DOWNSTREAM_HEADERS);
    }

    /**
     * Costruisce l'interceptor con gli header di propagazione indicati.
     *
     * @param headerDaValorizzare i nomi degli header su cui scrivere il correlation id
     */
    public CorrelationIdClientInterceptor(List<String> headerDaValorizzare) {
        this.headerDaValorizzare = List.copyOf(headerDaValorizzare);
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        String correlationId = TransactionContext.getCorrelationId();
        if (correlationId != null && !correlationId.isBlank()) {
            for (String header : headerDaValorizzare) {
                if (!request.getHeaders().containsHeader(header)) {
                    request.getHeaders().set(header, correlationId);
                }
            }
        }
        return execution.execute(request, body);
    }
}
