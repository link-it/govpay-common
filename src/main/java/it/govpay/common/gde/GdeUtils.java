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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.govpay.common.client.gde.HttpDataHolder;
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
        } catch (JsonProcessingException e) {
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
