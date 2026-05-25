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
package it.govpay.common.client.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Wrapper asincrono per RestTemplate che utilizza CompletableFuture.
 *
 * <p>Fornisce un'API simile a RestTemplate ma con esecuzione asincrona non-blocking
 * tramite un ExecutorService configurabile.
 *
 * <p>Esempio d'uso:
 * <pre>{@code
 * AsyncRestTemplateWrapper asyncClient = connettoreService.getAsyncRestTemplate("COD_CONNETTORE");
 *
 * // GET asincrono
 * CompletableFuture<ResponseEntity<String>> future = asyncClient.getForEntityAsync("/api/data", String.class);
 * future.thenAccept(response -> log.info("Ricevuto: {}", response.getBody()));
 *
 * // POST asincrono
 * CompletableFuture<ResponseEntity<RispostaDTO>> postFuture =
 *     asyncClient.postForEntityAsync("/api/create", request, RispostaDTO.class);
 * }</pre>
 *
 * @see RestTemplate
 * @see CompletableFuture
 */
@Slf4j
public class AsyncRestTemplateWrapper {

    private final RestTemplate restTemplate;
    private final Executor executor;

    public AsyncRestTemplateWrapper(RestTemplate restTemplate, Executor executor) {
        this.restTemplate = restTemplate;
        this.executor = executor;
    }

    /**
     * Esegue una richiesta GET asincrona.
     *
     * @param url URL della richiesta
     * @param responseType Tipo della risposta
     * @param uriVariables Variabili URI opzionali
     * @return CompletableFuture con la ResponseEntity
     */
    public <T> CompletableFuture<ResponseEntity<T>> getForEntityAsync(
            String url, Class<T> responseType, Object... uriVariables) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Executing async GET: {}", url);
            return restTemplate.getForEntity(url, responseType, uriVariables);
        }, executor);
    }

    /**
     * Esegue una richiesta GET asincrona e restituisce solo il body.
     *
     * @param url URL della richiesta
     * @param responseType Tipo della risposta
     * @param uriVariables Variabili URI opzionali
     * @return CompletableFuture con il body della risposta
     */
    public <T> CompletableFuture<T> getForObjectAsync(
            String url, Class<T> responseType, Object... uriVariables) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Executing async GET for object: {}", url);
            return restTemplate.getForObject(url, responseType, uriVariables);
        }, executor);
    }

    /**
     * Esegue una richiesta POST asincrona.
     *
     * @param url URL della richiesta
     * @param request Body della richiesta
     * @param responseType Tipo della risposta
     * @param uriVariables Variabili URI opzionali
     * @return CompletableFuture con la ResponseEntity
     */
    public <T> CompletableFuture<ResponseEntity<T>> postForEntityAsync(
            String url, Object request, Class<T> responseType, Object... uriVariables) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Executing async POST: {}", url);
            return restTemplate.postForEntity(url, request, responseType, uriVariables);
        }, executor);
    }

    /**
     * Esegue una richiesta POST asincrona e restituisce solo il body.
     *
     * @param url URL della richiesta
     * @param request Body della richiesta
     * @param responseType Tipo della risposta
     * @param uriVariables Variabili URI opzionali
     * @return CompletableFuture con il body della risposta
     */
    public <T> CompletableFuture<T> postForObjectAsync(
            String url, Object request, Class<T> responseType, Object... uriVariables) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Executing async POST for object: {}", url);
            return restTemplate.postForObject(url, request, responseType, uriVariables);
        }, executor);
    }

    /**
     * Esegue una richiesta PUT asincrona.
     *
     * @param url URL della richiesta
     * @param request Body della richiesta
     * @param uriVariables Variabili URI opzionali
     * @return {@code CompletableFuture<Void>} che completa quando la richiesta è terminata
     */
    public CompletableFuture<Void> putAsync(String url, Object request, Object... uriVariables) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Executing async PUT: {}", url);
            restTemplate.put(url, request, uriVariables);
        }, executor);
    }

    /**
     * Esegue una richiesta DELETE asincrona.
     *
     * @param url URL della richiesta
     * @param uriVariables Variabili URI opzionali
     * @return {@code CompletableFuture<Void>} che completa quando la richiesta è terminata
     */
    public CompletableFuture<Void> deleteAsync(String url, Object... uriVariables) {
        return CompletableFuture.runAsync(() -> {
            log.debug("Executing async DELETE: {}", url);
            restTemplate.delete(url, uriVariables);
        }, executor);
    }

    /**
     * Esegue una richiesta PATCH asincrona.
     *
     * @param url URL della richiesta
     * @param request Body della richiesta
     * @param responseType Tipo della risposta
     * @param uriVariables Variabili URI opzionali
     * @return CompletableFuture con la ResponseEntity
     */
    public <T> CompletableFuture<ResponseEntity<T>> patchForEntityAsync(
            String url, Object request, Class<T> responseType, Object... uriVariables) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Executing async PATCH: {}", url);
            return restTemplate.exchange(url, HttpMethod.PATCH,
                    new HttpEntity<>(request), responseType, uriVariables);
        }, executor);
    }

    /**
     * Esegue una richiesta generica con metodo HTTP specificato in modo asincrono.
     *
     * @param url URL della richiesta
     * @param method Metodo HTTP (GET, POST, PUT, DELETE, ecc.)
     * @param requestEntity HttpEntity con body e headers
     * @param responseType Tipo della risposta
     * @param uriVariables Variabili URI opzionali
     * @return CompletableFuture con la ResponseEntity
     */
    public <T> CompletableFuture<ResponseEntity<T>> exchangeAsync(
            String url, HttpMethod method, HttpEntity<?> requestEntity,
            Class<T> responseType, Object... uriVariables) {

        return CompletableFuture.supplyAsync(() -> {
            log.debug("Executing async {}: {}", method, url);
            return restTemplate.exchange(url, method, requestEntity, responseType, uriVariables);
        }, executor);
    }

    /**
     * Restituisce il RestTemplate sottostante per operazioni avanzate.
     *
     * @return il RestTemplate configurato
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * Restituisce l'Executor utilizzato per l'esecuzione asincrona.
     *
     * @return l'Executor configurato
     */
    public Executor getExecutor() {
        return executor;
    }
}
