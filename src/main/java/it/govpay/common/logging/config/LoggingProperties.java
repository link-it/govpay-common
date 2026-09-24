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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;

import it.govpay.common.logging.LoggingCostanti;
import lombok.Getter;
import lombok.Setter;

/**
 * Proprieta' di configurazione della tracciatura Transaction ID / Correlation ID
 * (prefisso {@code govpay.logging}).
 *
 * <pre>
 * govpay:
 *   logging:
 *     tracciatura:
 *       enabled: true
 *       ordine: -2147483548           # Ordered.HIGHEST_PRECEDENCE + 100
 *       lunghezza-massima-id: 128
 *       pattern-id-valido: "[A-Za-z0-9._:\\-]+"
 *       correlation-inbound-headers: [ "X-Correlation-ID", "X-Request-ID" ]
 *       correlation-response-headers: [ "X-Correlation-ID", "X-Request-ID" ]
 *       correlation-downstream-headers: [ "X-Correlation-ID", "X-Request-ID" ]
 *       transaction-response-headers: [ "X-Transaction-ID", "X-Govpay-IdTransazione" ]
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "govpay.logging")
public class LoggingProperties {

    /** Configurazione della tracciatura degli identificativi. */
    private Tracciatura tracciatura = new Tracciatura();

    /**
     * Impostazioni di generazione, validazione e propagazione degli identificativi.
     */
    @Getter
    @Setter
    public static class Tracciatura {

        /** Abilita il filtro che popola MDC e header di risposta. */
        private boolean enabled = true;

        /**
         * Ordine del filtro nella catena servlet. Va eseguito prima di qualsiasi
         * altro filtro che produca log, quindi il default e' molto anticipato.
         */
        private int ordine = Integer.MIN_VALUE + 100;

        /** Lunghezza massima accettata per un identificativo ricevuto dall'esterno. */
        private int lunghezzaMassimaId = 128;

        /**
         * Espressione regolare che un identificativo ricevuto dall'esterno deve
         * soddisfare per essere riusato. Un valore non conforme viene scartato e
         * sostituito da un identificativo generato: evita che caratteri di
         * controllo o di a capo finiscano nei log o negli header di risposta.
         */
        private String patternIdValido = "[A-Za-z0-9._:\\-]+";

        /** Header letti in ingresso per il correlation id, in ordine di precedenza. */
        private List<String> correlationInboundHeaders =
                new ArrayList<>(LoggingCostanti.DEFAULT_CORRELATION_INBOUND_HEADERS);

        /** Header valorizzati in risposta con il correlation id. */
        private List<String> correlationResponseHeaders =
                new ArrayList<>(LoggingCostanti.DEFAULT_CORRELATION_RESPONSE_HEADERS);

        /** Header valorizzati con il correlation id sulle chiamate downstream. */
        private List<String> correlationDownstreamHeaders =
                new ArrayList<>(LoggingCostanti.DEFAULT_CORRELATION_DOWNSTREAM_HEADERS);

        /** Header valorizzati in risposta con il transaction id. */
        private List<String> transactionResponseHeaders =
                new ArrayList<>(LoggingCostanti.DEFAULT_TRANSACTION_RESPONSE_HEADERS);

        /**
         * Compila il pattern di validazione degli identificativi ricevuti dall'esterno.
         *
         * @return il pattern compilato, oppure {@code null} se la validazione e' disattivata
         */
        public Pattern compilaPatternIdValido() {
            if (patternIdValido == null || patternIdValido.isBlank()) {
                return null;
            }
            return Pattern.compile(patternIdValido);
        }
    }
}
