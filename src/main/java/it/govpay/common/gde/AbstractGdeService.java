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
package it.govpay.common.gde;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.ObjectMapper;

import it.govpay.common.client.gde.HttpDataHolder;
import it.govpay.common.configurazione.ConfigurazioneKeys;
import it.govpay.common.configurazione.model.GdeEvento;
import it.govpay.common.configurazione.model.GdeInterfaccia;
import it.govpay.common.configurazione.model.Giornale;
import it.govpay.common.configurazione.service.ConfigurazioneService;
import it.govpay.gde.client.beans.CategoriaEvento;
import it.govpay.gde.client.beans.ComponenteEvento;
import it.govpay.gde.client.beans.EsitoEvento;
import it.govpay.gde.client.beans.NuovoEvento;
import it.govpay.gde.client.beans.RuoloEvento;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe base astratta per l'invio di eventi al GDE (Giornale degli Eventi).
 * <p>
 * Fornisce funzionalita' comuni per:
 * <ul>
 *   <li>Invio asincrono di eventi tramite CompletableFuture</li>
 *   <li>Gestione degli errori con retry configurabile</li>
 *   <li>Costruzione di eventi OK/KO</li>
 *   <li>Serializzazione payload con GdeUtils</li>
 * </ul>
 * <p>
 * Le sottoclassi devono implementare:
 * <ul>
 *   <li>{@link #convertToGdeEvent(GdeEventInfo)} - conversione in formato GDE specifico</li>
 *   <li>{@link #getGdeEndpoint()} - URL dell'endpoint GDE</li>
 * </ul>
 * <p>
 * Esempio d'uso:
 * <pre>{@code
 * public class MyGdeService extends AbstractGdeService {
 *
 *     @Override
 *     protected NuovoEvento convertToGdeEvent(GdeEventInfo eventInfo) {
 *         NuovoEvento evento = new NuovoEvento();
 *         evento.setComponente(ComponenteEvento.GOVPAY);
 *         evento.setEsito(EsitoEvento.OK);
 *         // ... altri campi
 *         return evento;
 *     }
 *
 *     @Override
 *     protected String getGdeEndpoint() {
 *         return gdeProperties.getEndpoint();
 *     }
 * }
 * }</pre>
 */
@Slf4j
public abstract class AbstractGdeService {

    /** Codice connettore per il servizio GDE */
    public static final String COD_CONNETTORE_GDE = ConfigurazioneKeys.COD_CONNETTORE_GDE;

    /** Esito OK */
    public static final EsitoEvento ESITO_OK = EsitoEvento.OK;

    /** Esito KO */
    public static final EsitoEvento ESITO_KO = EsitoEvento.KO;

    /** Ruolo client */
    public static final RuoloEvento RUOLO_CLIENT = RuoloEvento.CLIENT;

    /** Categoria interfaccia */
    public static final CategoriaEvento CATEGORIA_INTERFACCIA = CategoriaEvento.INTERFACCIA;

    /** Tipo evento interfaccia */
    public static final String TIPO_EVENTO_INTERFACCIA = "interfaccia";

    protected final ObjectMapper objectMapper;
    protected final Executor asyncExecutor;
    private final ConfigurazioneService configurazioneService;

    /**
     * Costruttore.
     *
     * @param objectMapper          ObjectMapper per serializzazione JSON
     * @param asyncExecutor         Executor per esecuzione asincrona
     * @param configurazioneService ConfigurazioneService per ottenere il connettore GDE
     */
    protected AbstractGdeService(ObjectMapper objectMapper, Executor asyncExecutor,
            ConfigurazioneService configurazioneService) {
        this.objectMapper = objectMapper;
        this.asyncExecutor = asyncExecutor;
        this.configurazioneService = configurazioneService;
    }

    // ==================== Abstract Methods ====================

    /**
     * Converte un GdeEventInfo nel formato NuovoEvento del GDE.
     *
     * @param eventInfo informazioni dell'evento
     * @return NuovoEvento pronto per essere inviato
     */
    protected abstract NuovoEvento convertToGdeEvent(GdeEventInfo eventInfo);

    /**
     * Restituisce l'URL dell'endpoint GDE.
     *
     * @return URL endpoint
     */
    protected abstract String getGdeEndpoint();

    /**
     * Determina se l'operazione descritta dall'evento e' una lettura.
     * <p>
     * L'implementazione di default delega a
     * {@link GdeUtils#isRequestLettura(String, ComponenteEvento, String)}
     * che gestisce API_PAGOPA (operazione SOAP), tracciati notifica pagamenti e
     * il fallback sul metodo HTTP.
     * Le sottoclassi possono sovrascrivere per logiche specifiche.
     *
     * @param eventInfo informazioni dell'evento
     * @return true se l'operazione e' una lettura
     */
    protected boolean isRequestLettura(GdeEventInfo eventInfo) {
        return GdeUtils.isRequestLettura(eventInfo.getMetodoHttp(), eventInfo.getComponente(), eventInfo.getTipoEvento());
    }

    /**
     * Determina se l'operazione descritta dall'evento e' una scrittura.
     * <p>
     * L'implementazione di default delega a
     * {@link GdeUtils#isRequestScrittura(String, ComponenteEvento, String)}
     * che gestisce API_PAGOPA (operazione SOAP), tracciati notifica pagamenti e
     * il fallback sul metodo HTTP.
     * Le sottoclassi possono sovrascrivere per logiche specifiche.
     *
     * @param eventInfo informazioni dell'evento
     * @return true se l'operazione e' una scrittura
     */
    protected boolean isRequestScrittura(GdeEventInfo eventInfo) {
        return GdeUtils.isRequestScrittura(eventInfo.getMetodoHttp(), eventInfo.getComponente(), eventInfo.getTipoEvento());
    }

    /**
     * Restituisce la configurazione GDE per la componente specificata.
     * <p>
     * Ogni progetto implementa il proprio mapping; per i casi comuni e' possibile
     * delegare a {@link GdeUtils#getConfigurazioneComponente(ComponenteEvento, Giornale)}.
     *
     * @param componente componente che genera l'evento
     * @param giornale   configurazione completa del giornale degli eventi
     * @return la configurazione dell'interfaccia, o null se la componente non e' gestita
     */
    protected abstract GdeInterfaccia getConfigurazioneComponente(ComponenteEvento componente, Giornale giornale);

    /**
     * Restituisce il RestTemplate configurato per il GDE,
     * caricato dal connettore {@link #COD_CONNETTORE_GDE}.
     *
     * @return RestTemplate
     */
    protected RestTemplate getGdeRestTemplate() {
        return configurazioneService.getRestTemplateGDE();
    }

    // ==================== Public API ====================

    /**
     * Verifica se il servizio GDE e' abilitato controllando
     * l'esistenza e lo stato del connettore {@link #COD_CONNETTORE_GDE}.
     *
     * @return true se il connettore GDE esiste ed e' abilitato
     */
    public boolean isAbilitato() {
        return configurazioneService.isServizioGDEAbilitato();
    }

    /**
     * Invia un evento al GDE in modo asincrono.
     * <p>
     * L'invio avviene in background e non blocca il thread chiamante.
     * Gli errori vengono loggati ma non propagati.
     *
     * @param eventInfo informazioni dell'evento da inviare
     * @return CompletableFuture che completa quando l'invio termina
     */
    public CompletableFuture<Void> inviaEventoAsync(GdeEventInfo eventInfo) {
        return CompletableFuture.runAsync(() -> {
            try {
                inviaEvento(eventInfo);
            } catch (Exception e) {
                log.error("Errore invio evento GDE: {}", e.getMessage(), e);
            }
        }, asyncExecutor);
    }

    /**
     * Invia un evento al GDE in modo sincrono.
     * <p>
     * Prima di inviare, consulta la configurazione {@link Giornale} per verificare
     * la policy di log e dump per la componente e il tipo di operazione (lettura/scrittura).
     * Se la policy {@code log} indica che l'evento non deve essere registrato, l'invio viene saltato.
     * Se la policy {@code dump} indica che il payload non deve essere incluso, i campi
     * payload vengono rimossi dall'evento prima dell'invio.
     *
     * @param eventInfo informazioni dell'evento da inviare
     * @throws RestClientException se l'invio fallisce
     */
    public void inviaEvento(GdeEventInfo eventInfo) {
        if (!isAbilitato()) {
            log.debug("Servizio GDE non abilitato/configurato: evento non inviato (componente={}, tipoEvento={})",
                    eventInfo.getComponente(), eventInfo.getTipoEvento());
            return;
        }

        // Valuta la policy di log/dump dalla configurazione Giornale
        GdeEvento gdeEventoPolicy = resolveGdeEventoPolicy(eventInfo);
        EsitoEvento esito = eventInfo.getEsito();

        // Se la policy e' presente, valuta log e dump; se assente, invia comunque
        if (gdeEventoPolicy != null) {
            boolean logEvento = GdeUtils.logEvento(gdeEventoPolicy, esito);
            if (!logEvento) {
                log.debug("Evento GDE non registrato per policy: componente={}, esito={}, lettura={}",
                        eventInfo.getComponente(), esito, isRequestLettura(eventInfo));
                return;
            }

            boolean dumpEvento = GdeUtils.dumpEvento(gdeEventoPolicy, esito);
            if (!dumpEvento) {
                log.debug("Dump disabilitato per policy: rimozione payload dall'evento");
                eventInfo.setPayloadRichiesta(null);
                eventInfo.setPayloadRisposta(null);
            }
        }

        NuovoEvento gdeEvent = convertToGdeEvent(eventInfo);
        String endpoint = getGdeEndpoint();
        RestTemplate restTemplate = getGdeRestTemplate();

        log.debug("Invio evento GDE a {}: componente={}, esito={}",
                endpoint, eventInfo.getComponente(), esito);

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(endpoint, gdeEvent, Void.class);
            log.debug("Evento GDE inviato con successo: status={}", response.getStatusCode());
        } finally {
            HttpDataHolder.clear();
        }
    }

    /**
     * Risolve la configurazione {@link GdeEvento} (policy log/dump) applicabile all'evento.
     * <p>
     * Il metodo consulta la configurazione {@link Giornale} dal database, determina la
     * {@link GdeInterfaccia} per la componente dell'evento, e seleziona la sezione
     * {@code letture} o {@code scritture} in base alla classificazione dell'operazione.
     * Se l'operazione non e' classificabile come lettura ne' come scrittura, restituisce null
     * e l'evento non verra' registrato.
     *
     * @param eventInfo informazioni dell'evento
     * @return la policy GdeEvento applicabile, o null se la configurazione non e' disponibile
     *         o l'operazione non e' classificabile
     */
    protected GdeEvento resolveGdeEventoPolicy(GdeEventInfo eventInfo) {
        Optional<Giornale> giornaleOpt = configurazioneService.getGiornale();
        if (giornaleOpt.isEmpty()) {
            log.debug("Configurazione Giornale non presente, policy non applicabile");
            return null;
        }

        GdeInterfaccia interfaccia = getConfigurazioneComponente(eventInfo.getComponente(), giornaleOpt.get());
        if (interfaccia == null) {
            log.warn("Configurazione GDE non trovata per componente {}", eventInfo.getComponente());
            return null;
        }

        if (isRequestLettura(eventInfo)) {
            log.debug("Tipo operazione: lettura");
            return interfaccia.getLetture();
        } else if (isRequestScrittura(eventInfo)) {
            log.debug("Tipo operazione: scrittura");
            return interfaccia.getScritture();
        } else {
            log.debug("Tipo operazione non riconosciuta, l'evento non verra' registrato");
            return null;
        }
    }

    /**
     * Invia un evento di successo al GDE.
     *
     * @param eventInfo informazioni dell'evento (l'esito verra' impostato a OK)
     * @return CompletableFuture che completa quando l'invio termina
     */
    public CompletableFuture<Void> inviaEventoOk(GdeEventInfo eventInfo) {
        eventInfo.setEsito(ESITO_OK);
        return inviaEventoAsync(eventInfo);
    }

    /**
     * Invia un evento di errore al GDE.
     *
     * @param eventInfo informazioni dell'evento (l'esito verra' impostato a KO)
     * @return CompletableFuture che completa quando l'invio termina
     */
    public CompletableFuture<Void> inviaEventoKo(GdeEventInfo eventInfo) {
        eventInfo.setEsito(ESITO_KO);
        return inviaEventoAsync(eventInfo);
    }

    // ==================== Utility Methods ====================

    /**
     * Estrae e codifica il payload della risposta usando GdeUtils.
     *
     * @param response  risposta HTTP (puo' essere null)
     * @param exception eccezione (puo' essere null)
     * @return payload codificato in Base64
     */
    protected String extractResponsePayload(ResponseEntity<?> response, RestClientException exception) {
        return GdeUtils.extractResponsePayload(objectMapper, response, exception);
    }

    /**
     * Estrae e codifica il payload della richiesta usando GdeUtils.
     *
     * @param request oggetto richiesta
     * @return payload codificato in Base64
     */
    protected String extractRequestPayload(Object request) {
        return GdeUtils.extractRequestPayload(objectMapper, request);
    }

    /**
     * Costruisce un URL completo usando GdeUtils.
     *
     * @param baseUrl       URL base
     * @param operationPath path dell'operazione
     * @return URL completo
     */
    protected String buildUrl(String baseUrl, String operationPath) {
        return GdeUtils.buildUrl(baseUrl, operationPath, null, null);
    }

    /**
     * Determina l'esito in base alla risposta o eccezione.
     *
     * @param response  risposta HTTP (puo' essere null)
     * @param exception eccezione (puo' essere null)
     * @return EsitoEvento.OK o EsitoEvento.KO
     */
    protected EsitoEvento determineEsito(ResponseEntity<?> response, RestClientException exception) {
        if (exception != null) {
            return ESITO_KO;
        }
        if (response != null && response.getStatusCode().is2xxSuccessful()) {
            return ESITO_OK;
        }
        return ESITO_KO;
    }

    /**
     * Estrae lo status code dalla risposta o eccezione.
     *
     * @param response  risposta HTTP (puo' essere null)
     * @param exception eccezione (puo' essere null)
     * @return status code HTTP o null
     */
    protected Integer extractStatusCode(ResponseEntity<?> response, RestClientException exception) {
        if (exception instanceof HttpStatusCodeException httpException) {
            return httpException.getStatusCode().value();
        }
        if (response != null) {
            HttpStatusCode statusCode = response.getStatusCode();
            return statusCode.value();
        }
        return null;
    }

    /**
     * Costruisce la descrizione dell'esito.
     *
     * @param response  risposta HTTP (puo' essere null)
     * @param exception eccezione (puo' essere null)
     * @return descrizione dell'esito
     */
    protected String buildDescrizioneEsito(ResponseEntity<?> response, RestClientException exception) {
        if (exception != null) {
            if (exception instanceof HttpStatusCodeException httpException) {
                return String.format("HTTP %d: %s",
                        httpException.getStatusCode().value(),
                        httpException.getStatusText());
            }
            return exception.getMessage();
        }
        if (response != null) {
            return "HTTP " + response.getStatusCode().value();
        }
        return null;
    }

    /**
     * Restituisce l'ObjectMapper utilizzato.
     *
     * @return ObjectMapper
     */
    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
