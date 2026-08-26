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
package it.govpay.common.client.gde;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interceptor that captures HTTP request/response data for GDE (Giornale degli Eventi) logging.
 * <p>
 * This interceptor captures:
 * <ul>
 *   <li>Request: URI, method, headers, body, timestamp</li>
 *   <li>Response: headers, body, status code, status text, timestamp</li>
 * </ul>
 * <p>
 * All captured data is stored in {@link HttpDataHolder} and can be retrieved
 * after the HTTP call completes for event logging purposes.
 * <p>
 * The interceptor buffers the response body to allow it to be read multiple times:
 * once for capturing and once by the RestTemplate message converters.
 * <p>
 * <strong>Usage:</strong>
 * <pre>{@code
 * // The interceptor is automatically added to RestTemplate by RestTemplateFactory
 * RestTemplate restTemplate = connettoreService.getRestTemplate("MY_CONNECTOR");
 *
 * try {
 *     ResponseEntity<MyDto> response = restTemplate.getForEntity("/api/data", MyDto.class);
 *     // Process response...
 *
 *     // Access captured data for GDE logging
 *     String requestBody = HttpDataHolder.getRequestBodyAsString();
 *     String responseBody = HttpDataHolder.getResponseBodyAsString();
 *     HttpHeaders requestHeaders = HttpDataHolder.getRequestHeaders();
 *     HttpHeaders responseHeaders = HttpDataHolder.getResponseHeaders();
 *     Long elapsedTime = HttpDataHolder.getElapsedTimeMillis();
 *
 * } finally {
 *     // Always clear to prevent memory leaks
 *     HttpDataHolder.clear();
 * }
 * }</pre>
 *
 * @see HttpDataHolder
 */
@Slf4j
public class GdeCapturingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {

        // Clear any previous data from thread-local storage
        HttpDataHolder.clear();

        // Capture request data before execution
        captureRequestData(request, body);

        // Execute the request
        ClientHttpResponse response = execution.execute(request, body);

        // Capture response data after execution
        byte[] responseBody = captureResponseData(request, response);

        // Return a buffered response that allows the body to be read again
        return new BufferedClientHttpResponse(response, responseBody);
    }

    /**
     * Captures request data and stores it in HttpDataHolder.
     *
     * @param request the HTTP request
     * @param body    the request body bytes
     */
    private void captureRequestData(HttpRequest request, byte[] body) {
        try {
            HttpDataHolder.setRequestTimestamp(System.currentTimeMillis());
            HttpDataHolder.setRequestUri(request.getURI());
            HttpDataHolder.setRequestMethod(request.getMethod());
            HttpDataHolder.setRequestHeaders(request.getHeaders());

            if (body != null && body.length > 0) {
                HttpDataHolder.setRequestBody(body);
                log.trace("Captured request body: {} bytes for {} {}",
                        body.length, request.getMethod(), request.getURI());
            }

            log.trace("Captured request data for {} {}: headers={}",
                    request.getMethod(), request.getURI(), request.getHeaders().headerNames());

        } catch (Exception e) {
            log.warn("Failed to capture request data for {}: {}",
                    request.getURI(), e.getMessage());
        }
    }

    /**
     * Captures response data and stores it in HttpDataHolder.
     *
     * @param request  the original HTTP request (for logging)
     * @param response the HTTP response
     * @return the response body bytes for buffering
     */
    private byte[] captureResponseData(HttpRequest request, ClientHttpResponse response) {
        byte[] responseBody = new byte[0];

        try {
            HttpDataHolder.setResponseTimestamp(System.currentTimeMillis());
            HttpDataHolder.setResponseHeaders(response.getHeaders());
            HttpDataHolder.setResponseStatusCode(response.getStatusCode());
            HttpDataHolder.setResponseStatusText(response.getStatusText());

            // Read and capture the response body. Il risultato viene normalizzato a un array
            // vuoto: il body assente e' una condizione attesa (risposte 204/304, errori remoti)
            // e non deve mai propagarsi come null al resto del metodo o al chiamante.
            byte[] capturedBody = StreamUtils.copyToByteArray(response.getBody());
            responseBody = capturedBody != null ? capturedBody : new byte[0];

            // La dimensione viene letta prima di consegnare l'array all'HttpDataHolder: quel
            // metodo ammette null e l'analisi simbolica, esplorando il ramo null del callee,
            // considererebbe nullo anche il nostro array in ogni accesso successivo alla
            // chiamata (SonarCloud javabugs:S2259).
            int responseBodyLength = responseBody.length;

            HttpDataHolder.setResponseBody(responseBody);

            log.trace("Captured response data for {} {}: status={}, body={} bytes, headers={}",
                    request.getMethod(), request.getURI(),
                    response.getStatusCode().value(),
                    responseBodyLength,
                    response.getHeaders().headerNames());

        } catch (IOException e) {
            log.warn("Failed to capture response body for {} {}: {}",
                    request.getMethod(), request.getURI(), e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to capture response data for {} {}: {}",
                    request.getMethod(), request.getURI(), e.getMessage());
        }

        return responseBody;
    }

    /**
     * Wrapper for ClientHttpResponse that provides the buffered body.
     * This allows the response body to be read multiple times.
     */
    private static class BufferedClientHttpResponse implements ClientHttpResponse {

        private final ClientHttpResponse original;
        private final byte[] body;

        BufferedClientHttpResponse(ClientHttpResponse original, byte[] body) {
            this.original = original;
            this.body = body;
        }

        @Override
        public InputStream getBody() throws IOException {
            // Return a new ByteArrayInputStream each time, allowing multiple reads
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return original.getHeaders();
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return original.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return original.getStatusText();
        }

        @Override
        public void close() {
            original.close();
        }
    }
}
