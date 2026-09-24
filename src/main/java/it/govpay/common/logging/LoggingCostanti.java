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

import java.util.List;

/**
 * Costanti per la gestione di Transaction ID e Correlation ID (BP-LOG-3).
 *
 * <p>I due identificativi sono distinti e complementari:
 * <ul>
 *   <li><b>Transaction ID</b>: generato internamente dal componente all'inizio di
 *       ogni elaborazione. Non viene mai accettato dall'esterno. Raggruppa tutti i
 *       log prodotti dal componente per quella specifica gestione ed e' restituito
 *       al client nella risposta.</li>
 *   <li><b>Correlation ID</b>: accettato dall'esterno se presente negli header di
 *       richiesta, altrimenti generato. E' restituito al client <i>e</i> propagato
 *       alle chiamate downstream, cosi' che tutti i componenti del flusso lo
 *       riportino nei propri log.</li>
 * </ul>
 *
 * <p>Le chiavi MDC sono quelle attese dai pattern di log: la chiave
 * {@value #MDC_TRANSACTION_ID} coincide con quella usata da GovPay 3
 * ({@code org.openspcoop2.utils.service.context.MD5Constants.TRANSACTION_ID}),
 * cosi' che un pattern {@code %X{transactionId}} resti valido nella migrazione.
 */
public final class LoggingCostanti {

    private LoggingCostanti() {
        // costanti
    }

    /** Chiave MDC dell'identificativo di transazione (compatibile con GovPay 3). */
    public static final String MDC_TRANSACTION_ID = "transactionId";

    /** Chiave MDC dell'identificativo di correlazione. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    /** Header standard di correlazione, prima scelta in lettura. */
    public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

    /** Header di correlazione alternativo, diffuso su reverse proxy e gateway. */
    public static final String HEADER_REQUEST_ID = "X-Request-ID";

    /** Header con cui il transaction id viene restituito al client. */
    public static final String HEADER_TRANSACTION_ID = "X-Transaction-ID";

    /** Header legacy GovPay 3 con cui il transaction id veniva restituito al client. */
    public static final String HEADER_TRANSACTION_ID_LEGACY = "X-Govpay-IdTransazione";

    /** Header letti in ingresso per il correlation id, in ordine di precedenza. */
    public static final List<String> DEFAULT_CORRELATION_INBOUND_HEADERS =
            List.of(HEADER_CORRELATION_ID, HEADER_REQUEST_ID);

    /** Header valorizzati in risposta con il correlation id. */
    public static final List<String> DEFAULT_CORRELATION_RESPONSE_HEADERS =
            List.of(HEADER_CORRELATION_ID, HEADER_REQUEST_ID);

    /** Header valorizzati con il correlation id sulle chiamate downstream. */
    public static final List<String> DEFAULT_CORRELATION_DOWNSTREAM_HEADERS =
            List.of(HEADER_CORRELATION_ID, HEADER_REQUEST_ID);

    /** Header valorizzati in risposta con il transaction id. */
    public static final List<String> DEFAULT_TRANSACTION_RESPONSE_HEADERS =
            List.of(HEADER_TRANSACTION_ID, HEADER_TRANSACTION_ID_LEGACY);
}
