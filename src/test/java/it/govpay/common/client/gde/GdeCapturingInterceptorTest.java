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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GdeCapturingInterceptorTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new GdeCapturingInterceptor());
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @AfterEach
    void cleanup() {
        HttpDataHolder.clear();
    }

    @Test
    void testCapturesGetRequest() {
        String responseBody = "{\"id\":1,\"name\":\"Test\"}";

        mockServer.expect(requestTo("/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        restTemplate.getForEntity("/api/test", String.class);

        // Verify request data captured
        assertNotNull(HttpDataHolder.getRequestUri());
        assertTrue(HttpDataHolder.getRequestUri().toString().contains("/api/test"));
        assertEquals(HttpMethod.GET, HttpDataHolder.getRequestMethod());
        assertNotNull(HttpDataHolder.getRequestHeaders());
        assertNotNull(HttpDataHolder.getRequestTimestamp());

        // Verify response data captured
        assertNotNull(HttpDataHolder.getResponseHeaders());
        assertEquals(responseBody, HttpDataHolder.getResponseBodyAsString());
        assertEquals(HttpStatus.OK, HttpDataHolder.getResponseStatusCode());
        assertNotNull(HttpDataHolder.getResponseTimestamp());

        mockServer.verify();
    }

    @Test
    void testCapturesPostRequest() {
        String requestBody = "{\"name\":\"New Item\"}";
        String responseBody = "{\"id\":123,\"name\":\"New Item\"}";

        mockServer.expect(requestTo("/api/items"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(requestBody))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        restTemplate.postForEntity("/api/items", requestBody, String.class);

        // Verify request body captured
        assertEquals(requestBody, HttpDataHolder.getRequestBodyAsString());
        assertEquals(HttpMethod.POST, HttpDataHolder.getRequestMethod());

        // Verify response captured
        assertEquals(responseBody, HttpDataHolder.getResponseBodyAsString());

        mockServer.verify();
    }

    @Test
    void testCapturesPutRequest() {
        String requestBody = "{\"name\":\"Updated\"}";

        mockServer.expect(requestTo("/api/items/1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess());

        restTemplate.put("/api/items/1", requestBody);

        assertEquals(HttpMethod.PUT, HttpDataHolder.getRequestMethod());
        assertNotNull(HttpDataHolder.getRequestBody());
        assertTrue(HttpDataHolder.hasRequestData());

        mockServer.verify();
    }

    @Test
    void testCapturesDeleteRequest() {
        mockServer.expect(requestTo("/api/items/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        restTemplate.delete("/api/items/1");

        assertEquals(HttpMethod.DELETE, HttpDataHolder.getRequestMethod());
        assertTrue(HttpDataHolder.getRequestUri().toString().contains("/api/items/1"));

        mockServer.verify();
    }

    @Test
    void testCapturesErrorResponse() {
        String errorBody = "{\"error\":\"Not Found\",\"message\":\"Item not found\"}";

        mockServer.expect(requestTo("/api/items/999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        try {
            restTemplate.getForEntity("/api/items/999", String.class);
        } catch (Exception e) {
            // Expected exception for 404
        }

        // Response data should still be captured even on error
        assertEquals(errorBody, HttpDataHolder.getResponseBodyAsString());
        assertEquals(HttpStatus.NOT_FOUND, HttpDataHolder.getResponseStatusCode());

        mockServer.verify();
    }

    @Test
    void testCapturesServerError() {
        String errorBody = "{\"error\":\"Internal Server Error\"}";

        mockServer.expect(requestTo("/api/error"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorBody));

        try {
            restTemplate.getForEntity("/api/error", String.class);
        } catch (Exception e) {
            // Expected exception for 500
        }

        assertEquals(errorBody, HttpDataHolder.getResponseBodyAsString());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpDataHolder.getResponseStatusCode());

        mockServer.verify();
    }

    @Test
    void testCapturesResponseHeaders() {
        mockServer.expect(requestTo("/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON)
                        .headers(new org.springframework.http.HttpHeaders() {{
                            add("X-Custom-Header", "custom-value");
                            add("X-Request-Id", "req-123");
                        }}));

        restTemplate.getForEntity("/api/test", String.class);

        assertNotNull(HttpDataHolder.getResponseHeaders());
        assertEquals("custom-value", HttpDataHolder.getResponseHeaders().getFirst("X-Custom-Header"));
        assertEquals("req-123", HttpDataHolder.getResponseHeaders().getFirst("X-Request-Id"));

        mockServer.verify();
    }

    @Test
    void testElapsedTimeCalculation() {
        mockServer.expect(requestTo("/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("OK", MediaType.TEXT_PLAIN));

        restTemplate.getForEntity("/api/test", String.class);

        Long elapsed = HttpDataHolder.getElapsedTimeMillis();
        assertNotNull(elapsed);
        assertTrue(elapsed >= 0, "Elapsed time should be non-negative");

        mockServer.verify();
    }

    @Test
    void testClearsDataBetweenRequests() {
        // Set up both expectations before making requests
        mockServer.expect(requestTo("/api/first"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("First Response", MediaType.TEXT_PLAIN));

        mockServer.expect(requestTo("/api/second"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("Second Response", MediaType.TEXT_PLAIN));

        // First request
        restTemplate.getForEntity("/api/first", String.class);
        assertEquals("First Response", HttpDataHolder.getResponseBodyAsString());

        // Second request - should clear previous data
        restTemplate.getForEntity("/api/second", String.class);
        assertEquals("Second Response", HttpDataHolder.getResponseBodyAsString());
        assertTrue(HttpDataHolder.getRequestUri().toString().contains("/api/second"));

        mockServer.verify();
    }

    @Test
    void testLargeResponseBody() {
        // Create a large response body
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeBody.append("{\"item\":").append(i).append("},");
        }
        String responseBody = "[" + largeBody.substring(0, largeBody.length() - 1) + "]";

        mockServer.expect(requestTo("/api/large"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        restTemplate.getForEntity("/api/large", String.class);

        assertEquals(responseBody, HttpDataHolder.getResponseBodyAsString());

        mockServer.verify();
    }

    @Test
    void testEmptyResponseBody() {
        mockServer.expect(requestTo("/api/empty"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withNoContent());

        restTemplate.getForEntity("/api/empty", Void.class);

        assertNotNull(HttpDataHolder.getResponseBody());
        assertEquals(0, HttpDataHolder.getResponseBody().length);

        mockServer.verify();
    }

    @Test
    void testResponseCanBeReadMultipleTimes() {
        String responseBody = "{\"data\":\"test\"}";

        mockServer.expect(requestTo("/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // The RestTemplate should be able to read and deserialize the response
        String result = restTemplate.getForObject("/api/test", String.class);

        // And the captured body should still be available
        assertEquals(responseBody, HttpDataHolder.getResponseBodyAsString());
        assertEquals(responseBody, result);

        mockServer.verify();
    }

    @Test
    void testCapturesResponseWithoutBody() {
        // 204 No Content: il body e' assente. La cattura non deve sollevare eccezioni
        // (l'interceptor non deve mai far fallire la chiamata di business) e il body
        // catturato deve essere un array vuoto, non null.
        mockServer.expect(requestTo("/api/items/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> restTemplate.delete("/api/items/1"));

        assertEquals(HttpStatus.NO_CONTENT, HttpDataHolder.getResponseStatusCode());
        assertNotNull(HttpDataHolder.getResponseBody());
        assertEquals(0, HttpDataHolder.getResponseBody().length);
        assertEquals("", HttpDataHolder.getResponseBodyAsString());

        mockServer.verify();
    }

    @Test
    void testCapturesErrorResponseWithoutBody() {
        // Servizio remoto in errore senza payload: e' lo scenario "di bordo" in cui
        // l'interceptor si attiva mentre il sistema e' gia' in difficolta'. Deve
        // limitarsi a non registrare nulla di piu', senza aggiungere un secondo guasto.
        mockServer.expect(requestTo("/api/error"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(HttpServerErrorException.class,
                () -> restTemplate.getForEntity("/api/error", String.class));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, HttpDataHolder.getResponseStatusCode());
        assertNotNull(HttpDataHolder.getResponseBody());
        assertEquals(0, HttpDataHolder.getResponseBody().length);

        mockServer.verify();
    }
}
