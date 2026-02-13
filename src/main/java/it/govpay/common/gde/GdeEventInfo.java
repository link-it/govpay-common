/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2025 Link.it srl (http://www.link.it).
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
package it.govpay.common.gde;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import it.govpay.gde.client.beans.CategoriaEvento;
import it.govpay.gde.client.beans.ComponenteEvento;
import it.govpay.gde.client.beans.EsitoEvento;
import it.govpay.gde.client.beans.Header;
import it.govpay.gde.client.beans.RuoloEvento;
import lombok.Builder;
import lombok.Data;

/**
 * DTO contenente le informazioni necessarie per creare un evento GDE.
 * <p>
 * Questa classe raccoglie tutti i dati che un evento GDE puo' contenere,
 * inclusi i dati della richiesta, della risposta e i metadati dell'operazione.
 */
@Data
@Builder
public class GdeEventInfo {

    // ==================== Metadati Operazione ====================

    /** Componente che genera l'evento (es. API_GOVPAY) */
    private ComponenteEvento componente;

    /** Categoria del componente (es. INTERFACCIA, INTERNO) */
    private CategoriaEvento categoriaEvento;

    /** Tipo di evento (es. "interfaccia", "interno") */
    private String tipoEvento;

    /** Ruolo del componente (es. CLIENT, SERVER) */
    private RuoloEvento ruolo;

    /** Sottotipo dell'evento */
    private String sottotipoEvento;

    /** Timestamp dell'evento */
    private OffsetDateTime dataEvento;

    /** Esito dell'operazione (OK, KO, FAIL) */
    private EsitoEvento esito;

    /** Descrizione dell'esito */
    private String descrizioneEsito;

    /** Identificativo univoco della richiesta (X-Request-Id) */
    private String idTransazione;

    // ==================== Dati Dominio ====================

    /** Codice del dominio/EC */
    private String idDominio;

    /** Codice della stazione */
    private String stazione;

    // ==================== Dati Richiesta ====================

    /** URL completo della richiesta */
    private String urlRichiesta;

    /** Metodo HTTP (GET, POST, PUT, DELETE) */
    private String metodoHttp;

    /** Payload della richiesta codificato in Base64 */
    private String payloadRichiesta;

    /** Headers della richiesta */
    private List<Header> headersRichiesta;

    // ==================== Dati Risposta ====================

    /** Status code HTTP della risposta */
    private Integer statusCodeRisposta;

    /** Payload della risposta codificato in Base64 */
    private String payloadRisposta;

    /** Headers della risposta */
    private List<Header> headersRisposta;

    // ==================== Dati Runtime (non persistiti) ====================

    /** Risposta HTTP originale (per estrazione dati) */
    @Builder.Default
    private ResponseEntity<?> response = null;

    /** Eccezione verificatasi (per estrazione dati errore) */
    @Builder.Default
    private RestClientException exception = null;

    /** Oggetto richiesta originale (per serializzazione) */
    @Builder.Default
    private Object requestObject = null;

    // ==================== Metodi di utilita' ====================

    /**
     * Verifica se l'evento rappresenta un successo.
     *
     * @return true se l'esito e' OK
     */
    public boolean isSuccess() {
        return EsitoEvento.OK.equals(esito);
    }

    /**
     * Verifica se l'evento rappresenta un errore.
     *
     * @return true se l'esito e' KO o FAIL
     */
    public boolean isError() {
        return EsitoEvento.KO.equals(esito) || EsitoEvento.FAIL.equals(esito);
    }
}
