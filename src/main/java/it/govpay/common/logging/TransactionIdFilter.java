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
import java.util.regex.Pattern;

import org.springframework.web.filter.OncePerRequestFilter;

import it.govpay.common.logging.config.LoggingProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro che apre il contesto di tracciatura di ogni richiesta HTTP (BP-LOG-3).
 *
 * <p>Per ogni richiesta:
 * <ol>
 *   <li>genera un <b>transaction id</b> nuovo (UUID v4): non viene mai accettato
 *       dall'esterno, e' l'identificativo interno del componente;</li>
 *   <li>determina il <b>correlation id</b> leggendo il primo header configurato
 *       che contenga un valore accettabile, altrimenti ne genera uno nuovo;</li>
 *   <li>pubblica entrambi nell'MDC, cosi' che finiscano in tutti i messaggi di
 *       log della richiesta;</li>
 *   <li>valorizza gli header di risposta configurati, cosi' che il client possa
 *       riconciliare la propria chiamata con i log del componente;</li>
 *   <li>ripulisce l'MDC al termine, anche in caso di eccezione.</li>
 * </ol>
 *
 * <p>Un correlation id ricevuto dall'esterno viene riusato solo se rispetta
 * lunghezza massima e pattern configurati: un valore non conforme viene scartato
 * (e sostituito da uno generato) per non riversare caratteri di controllo nei log
 * o negli header di risposta.
 *
 * <p>Gli header di risposta sono impostati <i>prima</i> di proseguire la catena:
 * una volta scritto il primo byte del corpo gli header non sono piu' modificabili.
 */
public class TransactionIdFilter extends OncePerRequestFilter {

    private final List<String> headerIngressoCorrelation;
    private final List<String> headerRispostaCorrelation;
    private final List<String> headerRispostaTransaction;
    private final int lunghezzaMassimaId;
    private final Pattern patternIdValido;

    /**
     * Costruisce il filtro a partire dalle proprieta' di tracciatura.
     *
     * @param tracciatura la configurazione di header, lunghezze e pattern
     */
    public TransactionIdFilter(LoggingProperties.Tracciatura tracciatura) {
        this.headerIngressoCorrelation = List.copyOf(tracciatura.getCorrelationInboundHeaders());
        this.headerRispostaCorrelation = List.copyOf(tracciatura.getCorrelationResponseHeaders());
        this.headerRispostaTransaction = List.copyOf(tracciatura.getTransactionResponseHeaders());
        this.lunghezzaMassimaId = tracciatura.getLunghezzaMassimaId();
        this.patternIdValido = tracciatura.compilaPatternIdValido();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String transactionId = TransactionContext.nuovoId();
        String correlationId = estraiCorrelationId(request);

        TransactionContext.setTransactionId(transactionId);
        TransactionContext.setCorrelationId(correlationId);

        for (String header : headerRispostaTransaction) {
            response.setHeader(header, transactionId);
        }
        for (String header : headerRispostaCorrelation) {
            response.setHeader(header, correlationId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TransactionContext.clear();
        }
    }

    private String estraiCorrelationId(HttpServletRequest request) {
        for (String header : headerIngressoCorrelation) {
            String valore = request.getHeader(header);
            if (isAccettabile(valore)) {
                return valore;
            }
        }
        return TransactionContext.nuovoId();
    }

    private boolean isAccettabile(String valore) {
        if (valore == null || valore.isBlank() || valore.length() > lunghezzaMassimaId) {
            return false;
        }
        return patternIdValido == null || patternIdValido.matcher(valore).matches();
    }
}
