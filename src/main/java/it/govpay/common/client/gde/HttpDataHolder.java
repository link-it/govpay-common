package it.govpay.common.client.gde;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;

import java.net.URI;

/**
 * Thread-local holder for storing HTTP request/response data for GDE (Giornale degli Eventi) logging.
 * <p>
 * This class captures comprehensive HTTP transaction data that can be used for:
 * - Event journaling and auditing
 * - Debugging failed requests
 * - Performance monitoring
 * - Compliance logging
 * <p>
 * Usage pattern:
 * <ol>
 *   <li>GdeCapturingInterceptor captures request data before execution</li>
 *   <li>GdeCapturingInterceptor captures response data after execution</li>
 *   <li>Application retrieves data for event logging via getXxx() methods</li>
 *   <li>Application calls clear() to prevent memory leaks (always in finally block)</li>
 * </ol>
 * <p>
 * Example:
 * <pre>{@code
 * try {
 *     ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
 *     // Process response...
 *
 *     // Log event with captured data
 *     eventLogger.logSuccess(
 *         HttpDataHolder.getRequestUri(),
 *         HttpDataHolder.getRequestMethod(),
 *         HttpDataHolder.getRequestHeaders(),
 *         HttpDataHolder.getRequestBody(),
 *         HttpDataHolder.getResponseHeaders(),
 *         HttpDataHolder.getResponseBody(),
 *         HttpDataHolder.getResponseStatusCode()
 *     );
 * } catch (Exception e) {
 *     // Log error event with captured data (response body available even on deserialization errors)
 *     eventLogger.logError(
 *         HttpDataHolder.getRequestUri(),
 *         HttpDataHolder.getResponseBody(),
 *         e
 *     );
 * } finally {
 *     HttpDataHolder.clear();
 * }
 * }</pre>
 *
 * @see GdeCapturingInterceptor
 */
public final class HttpDataHolder {

    // Request data
    private static final ThreadLocal<URI> REQUEST_URI = new ThreadLocal<>();
    private static final ThreadLocal<HttpMethod> REQUEST_METHOD = new ThreadLocal<>();
    private static final ThreadLocal<HttpHeaders> REQUEST_HEADERS = new ThreadLocal<>();
    private static final ThreadLocal<byte[]> REQUEST_BODY = new ThreadLocal<>();

    // Response data
    private static final ThreadLocal<HttpHeaders> RESPONSE_HEADERS = new ThreadLocal<>();
    private static final ThreadLocal<byte[]> RESPONSE_BODY = new ThreadLocal<>();
    private static final ThreadLocal<HttpStatusCode> RESPONSE_STATUS_CODE = new ThreadLocal<>();
    private static final ThreadLocal<String> RESPONSE_STATUS_TEXT = new ThreadLocal<>();

    // Timing data
    private static final ThreadLocal<Long> REQUEST_TIMESTAMP = new ThreadLocal<>();
    private static final ThreadLocal<Long> RESPONSE_TIMESTAMP = new ThreadLocal<>();

    private HttpDataHolder() {
        // Utility class - prevent instantiation
    }

    // ==================== Request Setters ====================

    /**
     * Stores the request URI for the current thread.
     *
     * @param uri the request URI
     */
    public static void setRequestUri(URI uri) {
        REQUEST_URI.set(uri);
    }

    /**
     * Stores the HTTP method for the current thread.
     *
     * @param method the HTTP method (GET, POST, PUT, DELETE, etc.)
     */
    public static void setRequestMethod(HttpMethod method) {
        REQUEST_METHOD.set(method);
    }

    /**
     * Stores the request headers for the current thread.
     *
     * @param headers the HTTP request headers
     */
    public static void setRequestHeaders(HttpHeaders headers) {
        REQUEST_HEADERS.set(headers != null ? HttpHeaders.readOnlyHttpHeaders(headers) : null);
    }

    /**
     * Stores the raw request body for the current thread.
     *
     * @param body the raw request body bytes
     */
    public static void setRequestBody(byte[] body) {
        REQUEST_BODY.set(body != null ? body.clone() : null);
    }

    /**
     * Stores the request timestamp for the current thread.
     *
     * @param timestamp the request start time in milliseconds
     */
    public static void setRequestTimestamp(long timestamp) {
        REQUEST_TIMESTAMP.set(timestamp);
    }

    // ==================== Response Setters ====================

    /**
     * Stores the response headers for the current thread.
     *
     * @param headers the HTTP response headers
     */
    public static void setResponseHeaders(HttpHeaders headers) {
        RESPONSE_HEADERS.set(headers != null ? HttpHeaders.readOnlyHttpHeaders(headers) : null);
    }

    /**
     * Stores the raw response body for the current thread.
     *
     * @param body the raw response body bytes
     */
    public static void setResponseBody(byte[] body) {
        RESPONSE_BODY.set(body != null ? body.clone() : null);
    }

    /**
     * Stores the response status code for the current thread.
     *
     * @param statusCode the HTTP status code
     */
    public static void setResponseStatusCode(HttpStatusCode statusCode) {
        RESPONSE_STATUS_CODE.set(statusCode);
    }

    /**
     * Stores the response status text for the current thread.
     *
     * @param statusText the HTTP status text (e.g., "OK", "Not Found")
     */
    public static void setResponseStatusText(String statusText) {
        RESPONSE_STATUS_TEXT.set(statusText);
    }

    /**
     * Stores the response timestamp for the current thread.
     *
     * @param timestamp the response received time in milliseconds
     */
    public static void setResponseTimestamp(long timestamp) {
        RESPONSE_TIMESTAMP.set(timestamp);
    }

    // ==================== Request Getters ====================

    /**
     * Retrieves the stored request URI for the current thread.
     *
     * @return the request URI, or null if not set
     */
    public static URI getRequestUri() {
        return REQUEST_URI.get();
    }

    /**
     * Retrieves the stored HTTP method for the current thread.
     *
     * @return the HTTP method, or null if not set
     */
    public static HttpMethod getRequestMethod() {
        return REQUEST_METHOD.get();
    }

    /**
     * Retrieves the stored request headers for the current thread.
     *
     * @return the HTTP request headers, or null if not set
     */
    public static HttpHeaders getRequestHeaders() {
        return REQUEST_HEADERS.get();
    }

    /**
     * Retrieves the stored request body for the current thread.
     *
     * @return a copy of the raw request body bytes, or null if not set
     */
    public static byte[] getRequestBody() {
        byte[] body = REQUEST_BODY.get();
        return body != null ? body.clone() : null;
    }

    /**
     * Retrieves the stored request body as a String for the current thread.
     *
     * @return the request body as UTF-8 string, or null if not set
     */
    public static String getRequestBodyAsString() {
        byte[] body = REQUEST_BODY.get();
        return body != null ? new String(body, java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    /**
     * Retrieves the stored request timestamp for the current thread.
     *
     * @return the request start time in milliseconds, or null if not set
     */
    public static Long getRequestTimestamp() {
        return REQUEST_TIMESTAMP.get();
    }

    // ==================== Response Getters ====================

    /**
     * Retrieves the stored response headers for the current thread.
     *
     * @return the HTTP response headers, or null if not set
     */
    public static HttpHeaders getResponseHeaders() {
        return RESPONSE_HEADERS.get();
    }

    /**
     * Retrieves the stored response body for the current thread.
     *
     * @return a copy of the raw response body bytes, or null if not set
     */
    public static byte[] getResponseBody() {
        byte[] body = RESPONSE_BODY.get();
        return body != null ? body.clone() : null;
    }

    /**
     * Retrieves the stored response body as a String for the current thread.
     *
     * @return the response body as UTF-8 string, or null if not set
     */
    public static String getResponseBodyAsString() {
        byte[] body = RESPONSE_BODY.get();
        return body != null ? new String(body, java.nio.charset.StandardCharsets.UTF_8) : null;
    }

    /**
     * Retrieves the stored response status code for the current thread.
     *
     * @return the HTTP status code, or null if not set
     */
    public static HttpStatusCode getResponseStatusCode() {
        return RESPONSE_STATUS_CODE.get();
    }

    /**
     * Retrieves the stored response status text for the current thread.
     *
     * @return the HTTP status text, or null if not set
     */
    public static String getResponseStatusText() {
        return RESPONSE_STATUS_TEXT.get();
    }

    /**
     * Retrieves the stored response timestamp for the current thread.
     *
     * @return the response received time in milliseconds, or null if not set
     */
    public static Long getResponseTimestamp() {
        return RESPONSE_TIMESTAMP.get();
    }

    // ==================== Utility Methods ====================

    /**
     * Calculates the elapsed time between request and response.
     *
     * @return elapsed time in milliseconds, or null if timestamps are not available
     */
    public static Long getElapsedTimeMillis() {
        Long requestTs = REQUEST_TIMESTAMP.get();
        Long responseTs = RESPONSE_TIMESTAMP.get();
        if (requestTs != null && responseTs != null) {
            return responseTs - requestTs;
        }
        return null;
    }

    /**
     * Checks if response data is available.
     *
     * @return true if response body or headers are set
     */
    public static boolean hasResponseData() {
        return RESPONSE_BODY.get() != null || RESPONSE_HEADERS.get() != null;
    }

    /**
     * Checks if request data is available.
     *
     * @return true if request URI or headers are set
     */
    public static boolean hasRequestData() {
        return REQUEST_URI.get() != null || REQUEST_HEADERS.get() != null;
    }

    /**
     * Clears all stored data for the current thread.
     * <p>
     * <strong>IMPORTANT:</strong> This method should always be called in a finally block
     * to prevent memory leaks in thread-pooled environments.
     */
    public static void clear() {
        REQUEST_URI.remove();
        REQUEST_METHOD.remove();
        REQUEST_HEADERS.remove();
        REQUEST_BODY.remove();
        REQUEST_TIMESTAMP.remove();
        RESPONSE_HEADERS.remove();
        RESPONSE_BODY.remove();
        RESPONSE_STATUS_CODE.remove();
        RESPONSE_STATUS_TEXT.remove();
        RESPONSE_TIMESTAMP.remove();
    }
}
