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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.govpay.common.client.gde.HttpDataHolder;

class GdeUtilsTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        HttpDataHolder.clear();
    }

    @Nested
    @DisplayName("Test encodeBase64")
    class EncodeBase64Tests {

        @Test
        @DisplayName("encodeBase64 - byte array")
        void encodeBase64_byteArray() {
            byte[] data = "test data".getBytes(StandardCharsets.UTF_8);
            String result = GdeUtils.encodeBase64(data);

            assertNotNull(result);
            assertEquals("test data", new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("encodeBase64 - byte array null")
        void encodeBase64_byteArrayNull() {
            assertNull(GdeUtils.encodeBase64((byte[]) null));
        }

        @Test
        @DisplayName("encodeBase64 - stringa")
        void encodeBase64_string() {
            String result = GdeUtils.encodeBase64("hello world");

            assertNotNull(result);
            assertEquals("hello world", new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("encodeBase64 - stringa null")
        void encodeBase64_stringNull() {
            assertNull(GdeUtils.encodeBase64((String) null));
        }
    }

    @Nested
    @DisplayName("Test writeValueAsString")
    class WriteValueAsStringTests {

        @Test
        @DisplayName("writeValueAsString - oggetto valido")
        void writeValueAsString_validObject() {
            Map<String, String> obj = Map.of("key", "value");
            String result = GdeUtils.writeValueAsString(objectMapper, obj);

            assertNotNull(result);
            assertTrue(result.contains("key"));
            assertTrue(result.contains("value"));
        }

        @Test
        @DisplayName("writeValueAsString - null")
        void writeValueAsString_null() {
            assertNull(GdeUtils.writeValueAsString(objectMapper, null));
        }

        @Test
        @DisplayName("writeValueAsString - oggetto non serializzabile")
        void writeValueAsString_nonSerializable() {
            // Un oggetto che causa errore di serializzazione
            Object badObject = new Object() {
                @SuppressWarnings("unused")
                public Object getSelf() {
                    return this; // riferimento circolare - ma Jackson lo gestisce
                }
            };

            // Jackson serializza come {}
            String result = GdeUtils.writeValueAsString(objectMapper, badObject);
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Test buildUrl")
    class BuildUrlTests {

        @Test
        @DisplayName("buildUrl - base e path")
        void buildUrl_baseAndPath() {
            String result = GdeUtils.buildUrl("http://localhost:8080", "/api/test", null, null);
            assertEquals("http://localhost:8080/api/test", result);
        }

        @Test
        @DisplayName("buildUrl - base con slash finale")
        void buildUrl_baseWithTrailingSlash() {
            String result = GdeUtils.buildUrl("http://localhost:8080/", "/api/test", null, null);
            assertEquals("http://localhost:8080/api/test", result);
        }

        @Test
        @DisplayName("buildUrl - path senza slash iniziale")
        void buildUrl_pathWithoutLeadingSlash() {
            String result = GdeUtils.buildUrl("http://localhost:8080", "api/test", null, null);
            assertEquals("http://localhost:8080/api/test", result);
        }

        @Test
        @DisplayName("buildUrl - con path parameters")
        void buildUrl_withPathParams() {
            Map<String, String> pathParams = new LinkedHashMap<>();
            pathParams.put("{id}", "123");
            pathParams.put("{type}", "user");

            String result = GdeUtils.buildUrl("http://localhost:8080", "/api/{type}/{id}", pathParams, null);
            assertEquals("http://localhost:8080/api/user/123", result);
        }

        @Test
        @DisplayName("buildUrl - con query parameters")
        void buildUrl_withQueryParams() {
            Map<String, String> queryParams = new LinkedHashMap<>();
            queryParams.put("page", "1");
            queryParams.put("size", "10");

            String result = GdeUtils.buildUrl("http://localhost:8080", "/api/test", null, queryParams);
            assertTrue(result.contains("page=1"));
            assertTrue(result.contains("size=10"));
            assertTrue(result.contains("?"));
        }

        @Test
        @DisplayName("buildUrl - URL gia' con query string")
        void buildUrl_urlWithExistingQueryString() {
            Map<String, String> queryParams = new LinkedHashMap<>();
            queryParams.put("extra", "value");

            String result = GdeUtils.buildUrl("http://localhost:8080/api?existing=param", null, null, queryParams);
            assertTrue(result.contains("existing=param"));
            assertTrue(result.contains("extra=value"));
            assertTrue(result.contains("&"));
        }

        @Test
        @DisplayName("buildUrl - base null")
        void buildUrl_baseNull() {
            String result = GdeUtils.buildUrl(null, "/api/test", null, null);
            assertEquals("/api/test", result);
        }

        @Test
        @DisplayName("buildUrl - path null")
        void buildUrl_pathNull() {
            String result = GdeUtils.buildUrl("http://localhost:8080", null, null, null);
            assertEquals("http://localhost:8080", result);
        }
    }

    @Nested
    @DisplayName("Test appendQueryString")
    class AppendQueryStringTests {

        @Test
        @DisplayName("appendQueryString - parametri vuoti")
        void appendQueryString_emptyParams() {
            String result = GdeUtils.appendQueryString("http://localhost", Map.of());
            assertEquals("http://localhost", result);
        }

        @Test
        @DisplayName("appendQueryString - parametri null")
        void appendQueryString_nullParams() {
            String result = GdeUtils.appendQueryString("http://localhost", null);
            assertEquals("http://localhost", result);
        }

        @Test
        @DisplayName("appendQueryString - parametri con valore null")
        void appendQueryString_paramsWithNullValue() {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("key1", "value1");
            params.put("key2", null);

            String result = GdeUtils.appendQueryString("http://localhost", params);
            assertTrue(result.contains("key1=value1"));
            assertFalse(result.contains("key2"));
        }
    }

    @Nested
    @DisplayName("Test extractResponsePayload")
    class ExtractResponsePayloadTests {

        @Test
        @DisplayName("extractResponsePayload - risposta OK")
        void extractResponsePayload_successResponse() {
            Map<String, String> body = Map.of("status", "success");
            ResponseEntity<Map<String, String>> response = ResponseEntity.ok(body);

            String result = GdeUtils.extractResponsePayload(objectMapper, response, null);

            assertNotNull(result);
            String decoded = new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("success"));
        }

        @Test
        @DisplayName("extractResponsePayload - risposta con body null")
        void extractResponsePayload_nullBody() {
            ResponseEntity<Object> response = ResponseEntity.ok().build();

            String result = GdeUtils.extractResponsePayload(objectMapper, response, null);
            assertNull(result);
        }

        @Test
        @DisplayName("extractResponsePayload - HttpStatusCodeException")
        void extractResponsePayload_httpStatusCodeException() {
            byte[] errorBody = "{\"error\":\"Not Found\"}".getBytes(StandardCharsets.UTF_8);
            HttpClientErrorException exception = HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, errorBody, StandardCharsets.UTF_8);

            String result = GdeUtils.extractResponsePayload(objectMapper, null, exception);

            assertNotNull(result);
            String decoded = new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("Not Found"));
        }

        @Test
        @DisplayName("extractResponsePayload - RestClientException generica")
        void extractResponsePayload_genericException() {
            RestClientException exception = new RestClientException("Connection refused");

            String result = GdeUtils.extractResponsePayload(objectMapper, null, exception);

            assertNotNull(result);
            String decoded = new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("Connection refused"));
        }
    }

    @Nested
    @DisplayName("Test extractRequestPayload")
    class ExtractRequestPayloadTests {

        @Test
        @DisplayName("extractRequestPayload - oggetto valido")
        void extractRequestPayload_validObject() {
            Map<String, String> request = Map.of("field", "value");

            String result = GdeUtils.extractRequestPayload(objectMapper, request);

            assertNotNull(result);
            String decoded = new String(Base64.getDecoder().decode(result), StandardCharsets.UTF_8);
            assertTrue(decoded.contains("field"));
            assertTrue(decoded.contains("value"));
        }

        @Test
        @DisplayName("extractRequestPayload - null")
        void extractRequestPayload_null() {
            assertNull(GdeUtils.extractRequestPayload(objectMapper, null));
        }
    }

    @Nested
    @DisplayName("Test createStandardRequestHeaders")
    class CreateStandardRequestHeadersTests {

        @Test
        @DisplayName("createStandardRequestHeaders - GET request")
        void createStandardRequestHeaders_getRequest() {
            List<String[]> headers = GdeUtils.createStandardRequestHeaders(
                    (name, value) -> new String[]{name, value}, true);

            assertEquals(1, headers.size());
            assertEquals(HttpHeaders.ACCEPT, headers.get(0)[0]);
        }

        @Test
        @DisplayName("createStandardRequestHeaders - POST request")
        void createStandardRequestHeaders_postRequest() {
            List<String[]> headers = GdeUtils.createStandardRequestHeaders(
                    (name, value) -> new String[]{name, value}, false);

            assertEquals(2, headers.size());
            assertTrue(headers.stream().anyMatch(h -> h[0].equals(HttpHeaders.ACCEPT)));
            assertTrue(headers.stream().anyMatch(h -> h[0].equals(HttpHeaders.CONTENT_TYPE)));
        }
    }

    @Nested
    @DisplayName("Test addXRequestIdHeader")
    class AddXRequestIdHeaderTests {

        @Test
        @DisplayName("addXRequestIdHeader - con valore")
        void addXRequestIdHeader_withValue() {
            List<String[]> headers = new ArrayList<>();

            GdeUtils.addXRequestIdHeader(headers, (name, value) -> new String[]{name, value}, "req-123");

            assertEquals(1, headers.size());
            assertEquals(GdeUtils.HEADER_X_REQUEST_ID, headers.get(0)[0]);
            assertEquals("req-123", headers.get(0)[1]);
        }

        @Test
        @DisplayName("addXRequestIdHeader - senza valore")
        void addXRequestIdHeader_withoutValue() {
            List<String[]> headers = new ArrayList<>();

            GdeUtils.addXRequestIdHeader(headers, (name, value) -> new String[]{name, value}, null);

            assertTrue(headers.isEmpty());
        }
    }

    @Nested
    @DisplayName("Test getCapturedRequestHeaders")
    class GetCapturedRequestHeadersTests {

        @Test
        @DisplayName("getCapturedRequestHeaders - con headers catturati")
        void getCapturedRequestHeaders_withCapturedHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Custom", "value1");
            headers.add(HttpHeaders.ACCEPT, "application/json");
            HttpDataHolder.setRequestHeaders(headers);

            List<String[]> result = GdeUtils.getCapturedRequestHeaders(
                    (name, value) -> new String[]{name, value});

            assertFalse(result.isEmpty());
            assertTrue(result.stream().anyMatch(h -> h[0].equals("X-Custom")));
        }

        @Test
        @DisplayName("getCapturedRequestHeaders - senza headers")
        void getCapturedRequestHeaders_withoutHeaders() {
            List<String[]> result = GdeUtils.getCapturedRequestHeaders(
                    (name, value) -> new String[]{name, value});

            assertTrue(result.isEmpty());
        }
    }
}
