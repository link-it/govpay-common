package it.govpay.common.client.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AsyncRestTemplateWrapperTest {

    private RestTemplate restTemplate;
    private AsyncRestTemplateWrapper asyncWrapper;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        asyncWrapper = new AsyncRestTemplateWrapper(restTemplate, Executors.newFixedThreadPool(5));
        mockServer = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
    }

    @Test
    void testGetForEntityAsync() throws ExecutionException, InterruptedException {
        // Setup mock
        mockServer.expect(requestTo("/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"message\":\"Hello\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        // Execute async
        CompletableFuture<ResponseEntity<String>> future =
                asyncWrapper.getForEntityAsync("/api/test", String.class);

        // Verify
        ResponseEntity<String> response = future.get();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Hello"));

        mockServer.verify();
    }

    @Test
    void testGetForObjectAsync() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("/api/data"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("test data", org.springframework.http.MediaType.TEXT_PLAIN));

        CompletableFuture<String> future =
                asyncWrapper.getForObjectAsync("/api/data", String.class);

        String result = future.get();
        assertNotNull(result);
        assertEquals("test data", result);

        mockServer.verify();
    }

    @Test
    void testPostForEntityAsync() throws ExecutionException, InterruptedException {
        String requestBody = "{\"name\":\"test\"}";

        mockServer.expect(requestTo("/api/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(requestBody))
                .andRespond(withSuccess("{\"id\":123}", org.springframework.http.MediaType.APPLICATION_JSON));

        CompletableFuture<ResponseEntity<String>> future =
                asyncWrapper.postForEntityAsync("/api/create", requestBody, String.class);

        ResponseEntity<String> response = future.get();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("123"));

        mockServer.verify();
    }

    @Test
    void testPostForObjectAsync() throws ExecutionException, InterruptedException {
        String requestBody = "{\"name\":\"test\"}";

        mockServer.expect(requestTo("/api/create"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("Created", org.springframework.http.MediaType.TEXT_PLAIN));

        CompletableFuture<String> future =
                asyncWrapper.postForObjectAsync("/api/create", requestBody, String.class);

        String result = future.get();
        assertEquals("Created", result);

        mockServer.verify();
    }

    @Test
    void testPutAsync() throws ExecutionException, InterruptedException {
        String requestBody = "{\"name\":\"updated\"}";

        mockServer.expect(requestTo("/api/update/1"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json(requestBody))
                .andRespond(withSuccess());

        CompletableFuture<Void> future =
                asyncWrapper.putAsync("/api/update/1", requestBody);

        future.get(); // Should complete without exception

        mockServer.verify();
    }

    @Test
    void testDeleteAsync() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("/api/delete/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        CompletableFuture<Void> future =
                asyncWrapper.deleteAsync("/api/delete/1");

        future.get(); // Should complete without exception

        mockServer.verify();
    }

    @Test
    void testMultipleAsyncCalls() throws ExecutionException, InterruptedException {
        // Setup multiple endpoints (unordered for parallel execution)
        mockServer.expect(ExpectedCount.once(), requestTo("/api/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("Response 1", org.springframework.http.MediaType.TEXT_PLAIN));

        mockServer.expect(ExpectedCount.once(), requestTo("/api/2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("Response 2", org.springframework.http.MediaType.TEXT_PLAIN));

        mockServer.expect(ExpectedCount.once(), requestTo("/api/3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("Response 3", org.springframework.http.MediaType.TEXT_PLAIN));

        // Execute multiple async calls
        CompletableFuture<String> future1 = asyncWrapper.getForObjectAsync("/api/1", String.class);
        CompletableFuture<String> future2 = asyncWrapper.getForObjectAsync("/api/2", String.class);
        CompletableFuture<String> future3 = asyncWrapper.getForObjectAsync("/api/3", String.class);

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);
        allFutures.get();

        // Verify all results
        assertEquals("Response 1", future1.get());
        assertEquals("Response 2", future2.get());
        assertEquals("Response 3", future3.get());

        mockServer.verify();
    }

    @Test
    void testAsyncWithThenAccept() throws InterruptedException {
        mockServer.expect(requestTo("/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("Success", org.springframework.http.MediaType.TEXT_PLAIN));

        final String[] result = new String[1];
        CompletableFuture<String> future = asyncWrapper.getForObjectAsync("/api/test", String.class)
                .thenApply(response -> {
                    result[0] = response;
                    return response;
                });

        // Wait for completion
        future.join();

        assertEquals("Success", result[0]);
        mockServer.verify();
    }

    @Test
    void testAsyncErrorHandling() {
        mockServer.expect(requestTo("/api/error"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        CompletableFuture<String> future = asyncWrapper.getForObjectAsync("/api/error", String.class);

        assertThrows(ExecutionException.class, future::get);
        mockServer.verify();
    }

    @Test
    void testGetRestTemplate() {
        assertNotNull(asyncWrapper.getRestTemplate());
        assertSame(restTemplate, asyncWrapper.getRestTemplate());
    }

    @Test
    void testGetExecutor() {
        assertNotNull(asyncWrapper.getExecutor());
    }

    @Test
    void testPatchForEntityAsync() throws ExecutionException, InterruptedException {
        String requestBody = "{\"name\":\"patched\"}";

        mockServer.expect(requestTo("/api/patch/1"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess("{\"status\":\"patched\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        CompletableFuture<ResponseEntity<String>> future =
                asyncWrapper.patchForEntityAsync("/api/patch/1", requestBody, String.class);

        ResponseEntity<String> response = future.get();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("patched"));

        mockServer.verify();
    }

    @Test
    void testExchangeAsync_GET() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("/api/exchange"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("Exchange Response", org.springframework.http.MediaType.TEXT_PLAIN));

        org.springframework.http.HttpEntity<?> requestEntity = new org.springframework.http.HttpEntity<>((Object) null);

        CompletableFuture<ResponseEntity<String>> future =
                asyncWrapper.exchangeAsync("/api/exchange", HttpMethod.GET, requestEntity, String.class);

        ResponseEntity<String> response = future.get();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Exchange Response", response.getBody());

        mockServer.verify();
    }

    @Test
    void testExchangeAsync_POST() throws ExecutionException, InterruptedException {
        String requestBody = "{\"data\":\"test\"}";

        mockServer.expect(requestTo("/api/exchange"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"result\":\"ok\"}", org.springframework.http.MediaType.APPLICATION_JSON));

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<String> requestEntity = new org.springframework.http.HttpEntity<>(requestBody, headers);

        CompletableFuture<ResponseEntity<String>> future =
                asyncWrapper.exchangeAsync("/api/exchange", HttpMethod.POST, requestEntity, String.class);

        ResponseEntity<String> response = future.get();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("ok"));

        mockServer.verify();
    }

    @Test
    void testGetForEntityAsync_WithUriVariables() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("/api/items/123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":123}", org.springframework.http.MediaType.APPLICATION_JSON));

        CompletableFuture<ResponseEntity<String>> future =
                asyncWrapper.getForEntityAsync("/api/items/{id}", String.class, "123");

        ResponseEntity<String> response = future.get();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("123"));

        mockServer.verify();
    }

    @Test
    void testPostForEntityAsync_WithUriVariables() throws ExecutionException, InterruptedException {
        String requestBody = "{\"name\":\"test\"}";

        mockServer.expect(requestTo("/api/items/123/sub"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"created\":true}", org.springframework.http.MediaType.APPLICATION_JSON));

        CompletableFuture<ResponseEntity<String>> future =
                asyncWrapper.postForEntityAsync("/api/items/{id}/sub", requestBody, String.class, "123");

        ResponseEntity<String> response = future.get();
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        mockServer.verify();
    }

    @Test
    void testPutAsync_WithUriVariables() throws ExecutionException, InterruptedException {
        String requestBody = "{\"name\":\"updated\"}";

        mockServer.expect(requestTo("/api/items/456"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess());

        CompletableFuture<Void> future =
                asyncWrapper.putAsync("/api/items/{id}", requestBody, "456");

        future.get();

        mockServer.verify();
    }

    @Test
    void testDeleteAsync_WithUriVariables() throws ExecutionException, InterruptedException {
        mockServer.expect(requestTo("/api/items/789"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        CompletableFuture<Void> future =
                asyncWrapper.deleteAsync("/api/items/{id}", "789");

        future.get();

        mockServer.verify();
    }
}
