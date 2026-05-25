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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpDataHolderTest {

    @BeforeEach
    @AfterEach
    void cleanup() {
        HttpDataHolder.clear();
    }

    @Test
    void testRequestUri() {
        URI uri = URI.create("https://api.example.com/test");
        HttpDataHolder.setRequestUri(uri);

        assertEquals(uri, HttpDataHolder.getRequestUri());
    }

    @Test
    void testRequestMethod() {
        HttpDataHolder.setRequestMethod(HttpMethod.POST);

        assertEquals(HttpMethod.POST, HttpDataHolder.getRequestMethod());
    }

    @Test
    void testRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer token123");
        headers.add("Content-Type", "application/json");

        HttpDataHolder.setRequestHeaders(headers);

        HttpHeaders retrieved = HttpDataHolder.getRequestHeaders();
        assertNotNull(retrieved);
        assertEquals("Bearer token123", retrieved.getFirst("Authorization"));
        assertEquals("application/json", retrieved.getFirst("Content-Type"));
    }

    @Test
    void testRequestBody() {
        byte[] body = "{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        HttpDataHolder.setRequestBody(body);

        byte[] retrieved = HttpDataHolder.getRequestBody();
        assertNotNull(retrieved);
        assertArrayEquals(body, retrieved);

        // Verify it's a copy (modifications don't affect stored value)
        retrieved[0] = 0;
        assertNotEquals(retrieved[0], HttpDataHolder.getRequestBody()[0]);
    }

    @Test
    void testRequestBodyAsString() {
        String bodyStr = "{\"message\":\"hello\"}";
        HttpDataHolder.setRequestBody(bodyStr.getBytes(StandardCharsets.UTF_8));

        assertEquals(bodyStr, HttpDataHolder.getRequestBodyAsString());
    }

    @Test
    void testRequestTimestamp() {
        long timestamp = System.currentTimeMillis();
        HttpDataHolder.setRequestTimestamp(timestamp);

        assertEquals(timestamp, HttpDataHolder.getRequestTimestamp());
    }

    @Test
    void testResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id", "req-123");
        headers.add("Content-Length", "1024");

        HttpDataHolder.setResponseHeaders(headers);

        HttpHeaders retrieved = HttpDataHolder.getResponseHeaders();
        assertNotNull(retrieved);
        assertEquals("req-123", retrieved.getFirst("X-Request-Id"));
        assertEquals("1024", retrieved.getFirst("Content-Length"));
    }

    @Test
    void testResponseBody() {
        byte[] body = "{\"result\":\"success\"}".getBytes(StandardCharsets.UTF_8);
        HttpDataHolder.setResponseBody(body);

        byte[] retrieved = HttpDataHolder.getResponseBody();
        assertNotNull(retrieved);
        assertArrayEquals(body, retrieved);
    }

    @Test
    void testResponseBodyAsString() {
        String bodyStr = "{\"status\":\"ok\"}";
        HttpDataHolder.setResponseBody(bodyStr.getBytes(StandardCharsets.UTF_8));

        assertEquals(bodyStr, HttpDataHolder.getResponseBodyAsString());
    }

    @Test
    void testResponseStatusCode() {
        HttpDataHolder.setResponseStatusCode(HttpStatus.OK);

        assertEquals(HttpStatus.OK, HttpDataHolder.getResponseStatusCode());
    }

    @Test
    void testResponseStatusText() {
        HttpDataHolder.setResponseStatusText("OK");

        assertEquals("OK", HttpDataHolder.getResponseStatusText());
    }

    @Test
    void testResponseTimestamp() {
        long timestamp = System.currentTimeMillis();
        HttpDataHolder.setResponseTimestamp(timestamp);

        assertEquals(timestamp, HttpDataHolder.getResponseTimestamp());
    }

    @Test
    void testElapsedTimeMillis() {
        long requestTime = 1000L;
        long responseTime = 1500L;

        HttpDataHolder.setRequestTimestamp(requestTime);
        HttpDataHolder.setResponseTimestamp(responseTime);

        assertEquals(500L, HttpDataHolder.getElapsedTimeMillis());
    }

    @Test
    void testElapsedTimeMillis_NullTimestamps() {
        assertNull(HttpDataHolder.getElapsedTimeMillis());

        HttpDataHolder.setRequestTimestamp(1000L);
        assertNull(HttpDataHolder.getElapsedTimeMillis());
    }

    @Test
    void testHasResponseData() {
        assertFalse(HttpDataHolder.hasResponseData());

        HttpDataHolder.setResponseBody("test".getBytes());
        assertTrue(HttpDataHolder.hasResponseData());
    }

    @Test
    void testHasResponseData_HeadersOnly() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Test", "value");
        HttpDataHolder.setResponseHeaders(headers);

        assertTrue(HttpDataHolder.hasResponseData());
    }

    @Test
    void testHasRequestData() {
        assertFalse(HttpDataHolder.hasRequestData());

        HttpDataHolder.setRequestUri(URI.create("https://test.com"));
        assertTrue(HttpDataHolder.hasRequestData());
    }

    @Test
    void testHasRequestData_HeadersOnly() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Test", "value");
        HttpDataHolder.setRequestHeaders(headers);

        assertTrue(HttpDataHolder.hasRequestData());
    }

    @Test
    void testClear() {
        // Set all values
        HttpDataHolder.setRequestUri(URI.create("https://test.com"));
        HttpDataHolder.setRequestMethod(HttpMethod.GET);
        HttpDataHolder.setRequestHeaders(new HttpHeaders());
        HttpDataHolder.setRequestBody("test".getBytes());
        HttpDataHolder.setRequestTimestamp(1000L);
        HttpDataHolder.setResponseHeaders(new HttpHeaders());
        HttpDataHolder.setResponseBody("response".getBytes());
        HttpDataHolder.setResponseStatusCode(HttpStatus.OK);
        HttpDataHolder.setResponseStatusText("OK");
        HttpDataHolder.setResponseTimestamp(2000L);

        // Clear all
        HttpDataHolder.clear();

        // Verify all are null
        assertNull(HttpDataHolder.getRequestUri());
        assertNull(HttpDataHolder.getRequestMethod());
        assertNull(HttpDataHolder.getRequestHeaders());
        assertNull(HttpDataHolder.getRequestBody());
        assertNull(HttpDataHolder.getRequestTimestamp());
        assertNull(HttpDataHolder.getResponseHeaders());
        assertNull(HttpDataHolder.getResponseBody());
        assertNull(HttpDataHolder.getResponseStatusCode());
        assertNull(HttpDataHolder.getResponseStatusText());
        assertNull(HttpDataHolder.getResponseTimestamp());
    }

    @Test
    void testNullBodyHandling() {
        HttpDataHolder.setRequestBody(null);
        HttpDataHolder.setResponseBody(null);

        assertNull(HttpDataHolder.getRequestBody());
        assertNull(HttpDataHolder.getRequestBodyAsString());
        assertNull(HttpDataHolder.getResponseBody());
        assertNull(HttpDataHolder.getResponseBodyAsString());
    }

    @Test
    void testNullHeadersHandling() {
        HttpDataHolder.setRequestHeaders(null);
        HttpDataHolder.setResponseHeaders(null);

        assertNull(HttpDataHolder.getRequestHeaders());
        assertNull(HttpDataHolder.getResponseHeaders());
    }

    @Test
    void testThreadIsolation() throws InterruptedException {
        // Set value in main thread
        HttpDataHolder.setRequestUri(URI.create("https://main-thread.com"));

        // Create another thread and verify it doesn't see main thread's data
        Thread otherThread = new Thread(() -> {
            assertNull(HttpDataHolder.getRequestUri());
            HttpDataHolder.setRequestUri(URI.create("https://other-thread.com"));
            assertEquals(URI.create("https://other-thread.com"), HttpDataHolder.getRequestUri());
        });

        otherThread.start();
        otherThread.join();

        // Main thread should still have its original value
        assertEquals(URI.create("https://main-thread.com"), HttpDataHolder.getRequestUri());
    }

    @Test
    void testFullHttpTransaction() {
        // Simulate a complete HTTP transaction capture
        long startTime = System.currentTimeMillis();

        // Request phase
        HttpDataHolder.setRequestTimestamp(startTime);
        HttpDataHolder.setRequestUri(URI.create("https://api.example.com/users/123"));
        HttpDataHolder.setRequestMethod(HttpMethod.PUT);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization", "Bearer token");
        requestHeaders.add("Content-Type", "application/json");
        HttpDataHolder.setRequestHeaders(requestHeaders);

        String requestBody = "{\"name\":\"Updated Name\"}";
        HttpDataHolder.setRequestBody(requestBody.getBytes(StandardCharsets.UTF_8));

        // Response phase
        long endTime = startTime + 150; // 150ms elapsed
        HttpDataHolder.setResponseTimestamp(endTime);
        HttpDataHolder.setResponseStatusCode(HttpStatus.OK);
        HttpDataHolder.setResponseStatusText("OK");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("X-Request-Id", "abc-123");
        HttpDataHolder.setResponseHeaders(responseHeaders);

        String responseBody = "{\"id\":123,\"name\":\"Updated Name\"}";
        HttpDataHolder.setResponseBody(responseBody.getBytes(StandardCharsets.UTF_8));

        // Verify all data is captured correctly
        assertEquals(URI.create("https://api.example.com/users/123"), HttpDataHolder.getRequestUri());
        assertEquals(HttpMethod.PUT, HttpDataHolder.getRequestMethod());
        assertEquals("Bearer token", HttpDataHolder.getRequestHeaders().getFirst("Authorization"));
        assertEquals(requestBody, HttpDataHolder.getRequestBodyAsString());

        assertEquals(HttpStatus.OK, HttpDataHolder.getResponseStatusCode());
        assertEquals("OK", HttpDataHolder.getResponseStatusText());
        assertEquals("abc-123", HttpDataHolder.getResponseHeaders().getFirst("X-Request-Id"));
        assertEquals(responseBody, HttpDataHolder.getResponseBodyAsString());

        assertEquals(150L, HttpDataHolder.getElapsedTimeMillis());
        assertTrue(HttpDataHolder.hasRequestData());
        assertTrue(HttpDataHolder.hasResponseData());
    }
}
