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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import it.govpay.common.client.gde.HttpDataHolder;
import it.govpay.common.configurazione.model.GdeEvento;
import it.govpay.common.configurazione.model.GdeInterfaccia;
import it.govpay.common.configurazione.model.Giornale;
import it.govpay.gde.client.beans.ComponenteEvento;
import it.govpay.gde.client.beans.EsitoEvento;
import it.govpay.gde.client.beans.Header;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class per la gestione degli eventi GDE (Giornale degli Eventi).
 * <p>
 * Fornisce metodi comuni per:
 * <ul>
 *   <li>Serializzazione payload in Base64</li>
 *   <li>Costruzione URL con path e query parameters</li>
 *   <li>Gestione headers HTTP</li>
 *   <li>Recupero dati catturati da HttpDataHolder</li>
 * </ul>
 */
@Slf4j
public final class GdeUtils {

    /** Header standard per Accept */
    public static final String GDE_HEADER_ACCEPT = MediaType.APPLICATION_JSON_VALUE;

    /** Header standard per Content-Type */
    public static final String GDE_HEADER_CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;

    /** Header X-Request-Id */
    public static final String HEADER_X_REQUEST_ID = "X-Request-Id";

    /** Messaggio per payload non serializzabile */
    public static final String MSG_PAYLOAD_NON_SERIALIZZABILE = "Payload non serializzabile";

    private GdeUtils() {
        // Utility class
    }

    // ==================== Serializzazione JSON ====================

    /**
     * Serializza un oggetto in JSON string.
     * <p>
     * In caso di errore di serializzazione, restituisce un messaggio di fallback
     * invece di lanciare un'eccezione.
     *
     * @param objectMapper ObjectMapper per la serializzazione
     * @param obj          oggetto da serializzare
     * @return stringa JSON o messaggio di fallback
     */
    public static String writeValueAsString(ObjectMapper objectMapper, Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            log.warn("Errore serializzazione JSON: {}", e.getMessage());
            return MSG_PAYLOAD_NON_SERIALIZZABILE;
        }
    }

    /**
     * Codifica un byte array in Base64.
     *
     * @param data dati da codificare
     * @return stringa Base64 o null se data e' null
     */
    public static String encodeBase64(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Codifica una stringa in Base64.
     *
     * @param data stringa da codificare
     * @return stringa Base64 o null se data e' null
     */
    public static String encodeBase64(String data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    // ==================== Payload Serialization ====================

    /**
     * Estrae e codifica in Base64 il payload della risposta.
     * <p>
     * Ordine di priorita':
     * <ol>
     *   <li>HttpStatusCodeException.getResponseBodyAsByteArray() - per errori HTTP (4xx, 5xx)</li>
     *   <li>HttpDataHolder.getResponseBody() - body catturato dall'interceptor</li>
     *   <li>response.getBody() serializzato - per risposte OK</li>
     *   <li>exception.getMessage() - fallback per altri errori</li>
     * </ol>
     *
     * @param objectMapper ObjectMapper per serializzazione
     * @param response     risposta HTTP (puo' essere null)
     * @param exception    eccezione (puo' essere null)
     * @return payload codificato in Base64 o null
     */
    public static String extractResponsePayload(ObjectMapper objectMapper, ResponseEntity<?> response, RestClientException exception) {
        try {
            if (exception != null) {
                // Caso 1: errore HTTP con body disponibile
                if (exception instanceof HttpStatusCodeException httpStatusCodeException) {
                    byte[] body = httpStatusCodeException.getResponseBodyAsByteArray();
                    if (body != null && body.length > 0) {
                        return encodeBase64(body);
                    }
                }

                // Caso 2: prova a recuperare il body catturato dall'interceptor
                byte[] capturedBody = HttpDataHolder.getResponseBody();
                if (capturedBody != null && capturedBody.length > 0) {
                    log.debug("Usando body catturato dall'interceptor: {} bytes", capturedBody.length);
                    return encodeBase64(capturedBody);
                }

                // Fallback: usa il messaggio dell'eccezione
                log.debug("Body non disponibile, uso messaggio eccezione");
                return encodeBase64(exception.getMessage());
            }

            // Caso 3: risposta OK
            if (response != null && response.getBody() != null) {
                return encodeBase64(writeValueAsString(objectMapper, response.getBody()));
            }

            return null;
        } finally {
            // Pulisci sempre il ThreadLocal per evitare memory leak
            HttpDataHolder.clear();
        }
    }

    /**
     * Estrae e codifica in Base64 il payload della richiesta.
     *
     * @param objectMapper ObjectMapper per serializzazione
     * @param request      oggetto richiesta
     * @return payload codificato in Base64 o null
     */
    public static String extractRequestPayload(ObjectMapper objectMapper, Object request) {
        if (request == null) {
            return null;
        }
        return encodeBase64(writeValueAsString(objectMapper, request));
    }

    // ==================== URL Building ====================

    /**
     * Costruisce un URL completo con path parameters e query parameters.
     *
     * @param baseUrl       URL base
     * @param operationPath path dell'operazione (puo' essere null)
     * @param pathParams    mappa di path parameters (placeholder -> valore)
     * @param queryParams   mappa di query parameters (nome -> valore)
     * @return URL completo
     */
    public static String buildUrl(String baseUrl, String operationPath, Map<String, String> pathParams, Map<String, String> queryParams) {
        StringBuilder url = new StringBuilder(baseUrl != null ? baseUrl : "");

        // Aggiungi operation path
        if (operationPath != null) {
            if (!url.isEmpty() && url.charAt(url.length() - 1) == '/') {
                url.append(operationPath.startsWith("/") ? operationPath.substring(1) : operationPath);
            } else {
                url.append(operationPath.startsWith("/") ? operationPath : "/" + operationPath);
            }
        }

        // Sostituisci path parameters
        String result = url.toString();
        if (pathParams != null) {
            for (Entry<String, String> param : pathParams.entrySet()) {
                result = result.replace(param.getKey(), param.getValue());
            }
        }

        // Aggiungi query parameters
        result = appendQueryString(result, queryParams);

        return result;
    }

    /**
     * Aggiunge query parameters all'URL.
     *
     * @param url         URL base
     * @param queryParams mappa di query parameters
     * @return URL con query string
     */
    public static String appendQueryString(String url, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }

        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> param : queryParams.entrySet()) {
            if (param.getValue() != null) {
                if (!sb.isEmpty()) {
                    sb.append("&");
                }
                sb.append(param.getKey()).append("=").append(param.getValue());
            }
        }

        if (sb.isEmpty()) {
            return url;
        }

        if (url.contains("?")) {
            return url + "&" + sb;
        } else {
            return url + "?" + sb;
        }
    }

    // ==================== Header Management ====================

    /**
     * Recupera gli headers della richiesta catturati da HttpDataHolder.
     *
     * @param headerConverter funzione per convertire HttpHeaders in formato GDE
     * @param <T>             tipo dell'header GDE
     * @return lista di headers convertiti
     */
    public static <T> List<T> getCapturedRequestHeaders(HeaderConverter<T> headerConverter) {
        List<T> headers = new ArrayList<>();
        HttpHeaders httpHeaders = HttpDataHolder.getRequestHeaders();

        if (httpHeaders != null) {
            httpHeaders.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    String value = String.join(", ", values);
                    headers.add(headerConverter.convert(key, value));
                }
            });
            log.trace("Recuperati {} headers della richiesta per GDE", headers.size());
        }

        return headers;
    }

    /**
     * Recupera gli headers della richiesta catturati usando i beans GDE.
     *
     * @return lista di headers GDE
     */
    public static List<Header> getCapturedRequestHeadersAsGdeHeaders() {
        return getCapturedRequestHeaders(GdeUtils::createGdeHeader);
    }

    /**
     * Crea una lista di headers standard per una richiesta.
     *
     * @param headerConverter funzione per convertire in formato GDE
     * @param isGet           true se e' una richiesta GET (senza Content-Type)
     * @param <T>             tipo dell'header GDE
     * @return lista di headers
     */
    public static <T> List<T> createStandardRequestHeaders(HeaderConverter<T> headerConverter, boolean isGet) {
        List<T> headers = new ArrayList<>();

        headers.add(headerConverter.convert(HttpHeaders.ACCEPT, GDE_HEADER_ACCEPT));

        if (!isGet) {
            headers.add(headerConverter.convert(HttpHeaders.CONTENT_TYPE, GDE_HEADER_CONTENT_TYPE));
        }

        return headers;
    }

    /**
     * Crea una lista di headers standard usando i beans GDE.
     *
     * @param isGet true se e' una richiesta GET (senza Content-Type)
     * @return lista di headers GDE
     */
    public static List<Header> createStandardRequestGdeHeaders(boolean isGet) {
        return createStandardRequestHeaders(GdeUtils::createGdeHeader, isGet);
    }

    /**
     * Aggiunge l'header X-Request-Id alla lista.
     *
     * @param headers         lista di headers
     * @param headerConverter funzione per convertire in formato GDE
     * @param xRequestId      valore dell'header
     * @param <T>             tipo dell'header GDE
     */
    public static <T> void addXRequestIdHeader(List<T> headers, HeaderConverter<T> headerConverter, String xRequestId) {
        if (xRequestId != null) {
            headers.add(headerConverter.convert(HEADER_X_REQUEST_ID, xRequestId));
        }
    }

    /**
     * Aggiunge l'header X-Request-Id alla lista usando i beans GDE.
     *
     * @param headers    lista di headers GDE
     * @param xRequestId valore dell'header
     */
    public static void addXRequestIdGdeHeader(List<Header> headers, String xRequestId) {
        addXRequestIdHeader(headers, GdeUtils::createGdeHeader, xRequestId);
    }

    /**
     * Crea un Header GDE a partire da nome e valore.
     *
     * @param name  nome dell'header
     * @param value valore dell'header
     * @return Header GDE
     */
    public static Header createGdeHeader(String name, String value) {
        Header header = new Header();
        header.setNome(name);
        header.setValore(value);
        return header;
    }

    // ==================== Configurazione Componente ====================

    /**
     * Mapping di default tra {@link ComponenteEvento} e la configurazione {@link GdeInterfaccia}
     * presente nel {@link Giornale}.
     * <p>
     * Copre i casi comuni; le sottoclassi di {@link AbstractGdeService} che necessitano di
     * mapping aggiuntivi (es. API_MYPIVOT, API_SECIM, GOVPAY) devono sovrascrivere
     * {@code getConfigurazioneComponente} e gestire i casi specifici prima di delegare qui.
     *
     * @param componente componente che genera l'evento
     * @param giornale   configurazione completa del giornale degli eventi
     * @return la configurazione dell'interfaccia, o null se la componente non e' gestita
     */
    public static GdeInterfaccia getConfigurazioneComponente(ComponenteEvento componente, Giornale giornale) {
        if (componente == null) return null;
        return switch (componente) {
            case API_BACKOFFICE -> giornale.getApiBackoffice();
            case API_ENTE -> giornale.getApiEnte();
            case API_PAGAMENTO -> giornale.getApiPagamento();
            case API_PAGOPA -> giornale.getApiPagoPA();
            case API_PENDENZE -> giornale.getApiPendenze();
            case API_RAGIONERIA -> giornale.getApiRagioneria();
            case API_BACKEND_IO -> giornale.getApiBackendIO();
            case API_MAGGIOLI_JPPA -> giornale.getApiMaggioliJPPA();
            default -> null;
        };
    }

    // ==================== Policy Evaluation ====================

    /**
     * Valuta se l'evento deve essere registrato (log) in base alla policy configurata e all'esito.
     *
     * @param evento configurazione policy per l'evento (letture o scritture)
     * @param esito  esito dell'operazione
     * @return true se l'evento deve essere registrato
     */
    public static boolean logEvento(GdeEvento evento, EsitoEvento esito) {
        if (evento == null || evento.getLog() == null) {
            return false;
        }
        return switch (evento.getLog()) {
            case MAI -> false;
            case SEMPRE -> true;
            case SOLO_ERRORE -> !EsitoEvento.OK.equals(esito);
        };
    }

    /**
     * Valuta se il payload (dump) deve essere incluso nell'evento in base alla policy configurata e all'esito.
     *
     * @param evento configurazione policy per l'evento (letture o scritture)
     * @param esito  esito dell'operazione
     * @return true se il payload deve essere incluso
     */
    public static boolean dumpEvento(GdeEvento evento, EsitoEvento esito) {
        if (evento == null || evento.getDump() == null) {
            return false;
        }
        return switch (evento.getDump()) {
            case MAI -> false;
            case SEMPRE -> true;
            case SOLO_ERRORE -> !EsitoEvento.OK.equals(esito);
        };
    }

    /**
     * Valuta se l'evento deve essere registrato (log) in base alla policy configurata e al codice HTTP di risposta.
     *
     * @param evento       configurazione policy per l'evento (letture o scritture)
     * @param responseCode codice HTTP della risposta
     * @return true se l'evento deve essere registrato
     */
    public static boolean logEvento(GdeEvento evento, Integer responseCode) {
        if (evento == null || evento.getLog() == null) {
            return false;
        }
        return switch (evento.getLog()) {
            case MAI -> false;
            case SEMPRE -> true;
            case SOLO_ERRORE -> responseCode > 399;
        };
    }

    /**
     * Valuta se il payload (dump) deve essere incluso nell'evento in base alla policy configurata e al codice HTTP di risposta.
     *
     * @param evento       configurazione policy per l'evento (letture o scritture)
     * @param responseCode codice HTTP della risposta
     * @return true se il payload deve essere incluso
     */
    public static boolean dumpEvento(GdeEvento evento, Integer responseCode) {
        if (evento == null || evento.getDump() == null) {
            return false;
        }
        return switch (evento.getDump()) {
            case MAI -> false;
            case SEMPRE -> true;
            case SOLO_ERRORE -> responseCode > 399;
        };
    }

    // ==================== Classificazione Operazioni ====================

    /**
     * Verifica se l'operazione API pagoPA e' una scrittura.
     *
     * @param operazione nome dell'operazione (tipo evento)
     * @return true se l'operazione e' una scrittura
     */
    public static boolean isOperazioneScrittura(String operazione) {
        return GdeCostanti.APIPAGOPA_TIPOEVENTO_NODOCHIEDICOPIART.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_NODOCHIEDISTATORPT.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_NODOINVIARPT.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_NODOINVIACARRELLORPT.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_NODOINVIARICHIESTASTORNO.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_NODOINVIARISPOSTAREVOCA.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PAAVERIFICARPT.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PAAATTIVARPT.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PAAINVIAESITOSTORNO.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PAAINVIARICHIESTAREVOCA.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PAAINVIART.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PAVERIFYPAYMENTNOTICE.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PAGETPAYMENT.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_PASENDRT.equals(operazione);
    }

    /**
     * Verifica se l'operazione e' una scrittura relativa ai tracciati di notifica pagamenti.
     *
     * @param operazione nome dell'operazione (tipo evento)
     * @return true se l'operazione e' una scrittura di tracciati notifica pagamenti
     */
    public static boolean isOperazioneScritturaTracciatiNotificaPagamenti(String operazione) {
        return GdeCostanti.APIMYPIVOT_TIPOEVENTO_MYPIVOTINVIATRACCIATOEMAIL.equals(operazione)
                || GdeCostanti.APIMYPIVOT_TIPOEVENTO_MYPIVOTINVIATRACCIATOFILESYSTEM.equals(operazione)
                || GdeCostanti.APIMYPIVOT_TIPOEVENTO_PIVOTSILINVIAFLUSSO.equals(operazione)
                || GdeCostanti.APISECIM_TIPOEVENTO_SECIMINVIATRACCIATOEMAIL.equals(operazione)
                || GdeCostanti.APISECIM_TIPOEVENTO_SECIMINVIATRACCIATOFILESYSTEM.equals(operazione)
                || GdeCostanti.APIGOVPAY_TIPOEVENTO_GOVPAYINVIATRACCIATOEMAIL.equals(operazione)
                || GdeCostanti.APIGOVPAY_TIPOEVENTO_GOVPAYINVIATRACCIATOFILESYSTEM.equals(operazione)
                || GdeCostanti.APIGOVPAY_TIPOEVENTO_GOVPAYINVIATRACCIATOREST.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_INVIAFLUSSORENDICONTAZIONE.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_INVIARPP.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_INVIASINTESIFLUSSIRENDICONTAZIONE.equals(operazione)
                || GdeCostanti.APIPAGOPA_TIPOEVENTO_INVIASINTESIPAGAMENTI.equals(operazione)
                || GdeCostanti.APIHYPERSICAPKAPPA_TIPOEVENTO_HYPERSIC_APKINVIATRACCIATOEMAIL.equals(operazione)
                || GdeCostanti.APIHYPERSICAPKAPPA_TIPOEVENTO_HYPERSIC_APKINVIATRACCIATOFILESYSTEM.equals(operazione);
    }

    /**
     * Verifica se la richiesta e' una lettura in base al metodo HTTP.
     *
     * @param httpMethod metodo HTTP (GET, POST, ecc.)
     * @return true se il metodo HTTP corrisponde a una lettura
     */
    public static boolean isRequestLettura(String httpMethod) {
        if (httpMethod == null) return false;
        return switch (httpMethod.toUpperCase()) {
            case "GET", "OPTIONS", "HEAD", "TRACE" -> true;
            default -> false;
        };
    }

    /**
     * Verifica se la richiesta e' una scrittura in base al metodo HTTP.
     *
     * @param httpMethod metodo HTTP (GET, POST, ecc.)
     * @return true se il metodo HTTP corrisponde a una scrittura
     */
    public static boolean isRequestScrittura(String httpMethod) {
        if (httpMethod == null) return false;
        return switch (httpMethod.toUpperCase()) {
            case "PUT", "POST", "DELETE", "PATCH", "LINK", "UNLINK" -> true;
            default -> false;
        };
    }

    /**
     * Verifica se la richiesta e' una lettura, tenendo conto della componente e dell'operazione.
     * <p>
     * Per API_PAGOPA la classificazione si basa sull'operazione SOAP, non sul metodo HTTP.
     * Per le componenti di tracciati notifica pagamenti (API_SECIM, API_MYPIVOT, API_GOVPAY,
     * API_HYPERSIC_APK) si usa la classificazione specifica per tracciati.
     * Per tutte le altre componenti si usa il metodo HTTP.
     *
     * @param httpMethod metodo HTTP
     * @param componente componente che genera l'evento
     * @param operazione nome dell'operazione (tipo evento)
     * @return true se l'operazione e' una lettura
     */
    public static boolean isRequestLettura(String httpMethod, ComponenteEvento componente, String operazione) {
        if (ComponenteEvento.API_PAGOPA.equals(componente)) {
            return operazione != null && !isOperazioneScrittura(operazione);
        }

        if (ComponenteEvento.API_SECIM.equals(componente)
                || ComponenteEvento.API_MYPIVOT.equals(componente)
                || ComponenteEvento.API_GOVPAY.equals(componente)
                || ComponenteEvento.API_HYPERSIC_APK.equals(componente)) {
            return operazione != null && !isOperazioneScritturaTracciatiNotificaPagamenti(operazione);
        }

        return isRequestLettura(httpMethod);
    }

    /**
     * Verifica se la richiesta e' una scrittura, tenendo conto della componente e dell'operazione.
     *
     * @param httpMethod metodo HTTP
     * @param componente componente che genera l'evento
     * @param operazione nome dell'operazione (tipo evento)
     * @return true se l'operazione e' una scrittura
     */
    public static boolean isRequestScrittura(String httpMethod, ComponenteEvento componente, String operazione) {
        if (ComponenteEvento.API_PAGOPA.equals(componente)) {
            return isOperazioneScrittura(operazione);
        }

        if (ComponenteEvento.API_SECIM.equals(componente)
                || ComponenteEvento.API_MYPIVOT.equals(componente)
                || ComponenteEvento.API_GOVPAY.equals(componente)
                || ComponenteEvento.API_HYPERSIC_APK.equals(componente)) {
            return isOperazioneScritturaTracciatiNotificaPagamenti(operazione);
        }

        return isRequestScrittura(httpMethod);
    }

    // ==================== Functional Interfaces ====================

    /**
     * Interfaccia funzionale per convertire header name/value in formato GDE.
     *
     * @param <T> tipo dell'header GDE
     */
    @FunctionalInterface
    public interface HeaderConverter<T> {
        /**
         * Converte un header in formato GDE.
         *
         * @param name  nome dell'header
         * @param value valore dell'header
         * @return header in formato GDE
         */
        T convert(String name, String value);
    }
}
