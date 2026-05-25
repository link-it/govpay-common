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
package it.govpay.common.client.model;

import it.govpay.common.entity.TipoAutenticazione;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConnettoreTest {

    @Test
    void testDefaultValues() {
        Connettore connettore = new Connettore();

        assertTrue(connettore.isAbilitato());
        assertEquals(5000, connettore.getConnectionTimeout());
        assertEquals(30000, connettore.getReadTimeout());
    }

    @Test
    void testBuilderWithDefaults() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST")
                .url("https://api.test.com")
                .build();

        assertEquals("TEST", connettore.getIdConnettore());
        assertEquals("https://api.test.com", connettore.getUrl());
        assertTrue(connettore.isAbilitato());
        assertEquals(5000, connettore.getConnectionTimeout());
        assertEquals(30000, connettore.getReadTimeout());
    }

    @Test
    void testBuilderAllFields() {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Custom", "value");

        Connettore connettore = Connettore.builder()
                .idConnettore("FULL_TEST")
                .url("https://api.full.com")
                .tipoAutenticazione(TipoAutenticazione.HTTP_BASIC)
                .httpUser("user")
                .httpPassw("pass")
                .tipoSsl(Connettore.EnumSslType.CLIENT)
                .sslKsType("PKCS12")
                .sslKsLocation("/path/keystore.p12")
                .sslKsPasswd("kspass")
                .sslPKeyPasswd("pkeypass")
                .sslTsType("JKS")
                .sslTsLocation("/path/truststore.jks")
                .sslTsPasswd("tspass")
                .sslType("TLSv1.2")
                .httpHeaderName("X-Auth")
                .httpHeaderValue("secret")
                .apiKey("api-key-123")
                .apiId("X-API-Key")
                .oauth2ClientCredentialsClientId("client-id")
                .oauth2ClientCredentialsClientSecret("client-secret")
                .oauth2ClientCredentialsUrlTokenEndpoint("https://auth.com/token")
                .oauth2ClientCredentialsScope("read write")
                .subscriptionKeyValue("sub-key")
                .customHeaders(customHeaders)
                .abilitato(false)
                .connectionTimeout(10000)
                .readTimeout(60000)
                .build();

        assertEquals("FULL_TEST", connettore.getIdConnettore());
        assertEquals("https://api.full.com", connettore.getUrl());
        assertEquals(TipoAutenticazione.HTTP_BASIC, connettore.getTipoAutenticazione());
        assertEquals("user", connettore.getHttpUser());
        assertEquals("pass", connettore.getHttpPassw());
        assertEquals(Connettore.EnumSslType.CLIENT, connettore.getTipoSsl());
        assertEquals("PKCS12", connettore.getSslKsType());
        assertEquals("/path/keystore.p12", connettore.getSslKsLocation());
        assertEquals("kspass", connettore.getSslKsPasswd());
        assertEquals("pkeypass", connettore.getSslPKeyPasswd());
        assertEquals("JKS", connettore.getSslTsType());
        assertEquals("/path/truststore.jks", connettore.getSslTsLocation());
        assertEquals("tspass", connettore.getSslTsPasswd());
        assertEquals("TLSv1.2", connettore.getSslType());
        assertEquals("X-Auth", connettore.getHttpHeaderName());
        assertEquals("secret", connettore.getHttpHeaderValue());
        assertEquals("api-key-123", connettore.getApiKey());
        assertEquals("X-API-Key", connettore.getApiId());
        assertEquals("client-id", connettore.getOauth2ClientCredentialsClientId());
        assertEquals("client-secret", connettore.getOauth2ClientCredentialsClientSecret());
        assertEquals("https://auth.com/token", connettore.getOauth2ClientCredentialsUrlTokenEndpoint());
        assertEquals("read write", connettore.getOauth2ClientCredentialsScope());
        assertEquals("sub-key", connettore.getSubscriptionKeyValue());
        assertNotNull(connettore.getCustomHeaders());
        assertEquals("value", connettore.getCustomHeaders().get("X-Custom"));
        assertFalse(connettore.isAbilitato());
        assertEquals(10000, connettore.getConnectionTimeout());
        assertEquals(60000, connettore.getReadTimeout());
    }

    @Test
    void testCopyConstructor() {
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("X-Header", "header-value");

        Connettore original = Connettore.builder()
                .idConnettore("ORIGINAL")
                .url("https://original.com")
                .tipoAutenticazione(TipoAutenticazione.SSL)
                .httpUser("origUser")
                .httpPassw("origPass")
                .tipoSsl(Connettore.EnumSslType.SERVER)
                .sslKsType("JKS")
                .sslKsLocation("/orig/ks")
                .sslKsPasswd("origKsPass")
                .sslPKeyPasswd("origPKeyPass")
                .sslTsType("PKCS12")
                .sslTsLocation("/orig/ts")
                .sslTsPasswd("origTsPass")
                .sslType("TLSv1.3")
                .httpHeaderName("X-Orig")
                .httpHeaderValue("orig-val")
                .apiKey("orig-api-key")
                .apiId("orig-api-id")
                .oauth2ClientCredentialsClientId("orig-client")
                .oauth2ClientCredentialsClientSecret("orig-secret")
                .oauth2ClientCredentialsUrlTokenEndpoint("https://orig.auth.com")
                .oauth2ClientCredentialsScope("orig-scope")
                .subscriptionKeyValue("orig-sub")
                .customHeaders(customHeaders)
                .abilitato(true)
                .connectionTimeout(7500)
                .readTimeout(45000)
                .build();

        Connettore copy = new Connettore(original);

        assertEquals(original.getIdConnettore(), copy.getIdConnettore());
        assertEquals(original.getUrl(), copy.getUrl());
        assertEquals(original.getTipoAutenticazione(), copy.getTipoAutenticazione());
        assertEquals(original.getHttpUser(), copy.getHttpUser());
        assertEquals(original.getHttpPassw(), copy.getHttpPassw());
        assertEquals(original.getTipoSsl(), copy.getTipoSsl());
        assertEquals(original.getSslKsType(), copy.getSslKsType());
        assertEquals(original.getSslKsLocation(), copy.getSslKsLocation());
        assertEquals(original.getSslKsPasswd(), copy.getSslKsPasswd());
        assertEquals(original.getSslPKeyPasswd(), copy.getSslPKeyPasswd());
        assertEquals(original.getSslTsType(), copy.getSslTsType());
        assertEquals(original.getSslTsLocation(), copy.getSslTsLocation());
        assertEquals(original.getSslTsPasswd(), copy.getSslTsPasswd());
        assertEquals(original.getSslType(), copy.getSslType());
        assertEquals(original.getHttpHeaderName(), copy.getHttpHeaderName());
        assertEquals(original.getHttpHeaderValue(), copy.getHttpHeaderValue());
        assertEquals(original.getApiKey(), copy.getApiKey());
        assertEquals(original.getApiId(), copy.getApiId());
        assertEquals(original.getOauth2ClientCredentialsClientId(), copy.getOauth2ClientCredentialsClientId());
        assertEquals(original.getOauth2ClientCredentialsClientSecret(), copy.getOauth2ClientCredentialsClientSecret());
        assertEquals(original.getOauth2ClientCredentialsUrlTokenEndpoint(), copy.getOauth2ClientCredentialsUrlTokenEndpoint());
        assertEquals(original.getOauth2ClientCredentialsScope(), copy.getOauth2ClientCredentialsScope());
        assertEquals(original.getSubscriptionKeyValue(), copy.getSubscriptionKeyValue());
        assertEquals(original.isAbilitato(), copy.isAbilitato());
        assertEquals(original.getConnectionTimeout(), copy.getConnectionTimeout());
        assertEquals(original.getReadTimeout(), copy.getReadTimeout());

        // Verify deep copy of customHeaders
        assertNotSame(original.getCustomHeaders(), copy.getCustomHeaders());
        assertEquals(original.getCustomHeaders().get("X-Header"), copy.getCustomHeaders().get("X-Header"));
    }

    @Test
    void testCopyConstructorWithNullCustomHeaders() {
        Connettore original = Connettore.builder()
                .idConnettore("NO_HEADERS")
                .url("https://noheaders.com")
                .customHeaders(null)
                .build();

        Connettore copy = new Connettore(original);

        assertNull(copy.getCustomHeaders());
    }

    @Test
    void testEnumSslTypeValues() {
        assertEquals(2, Connettore.EnumSslType.values().length);
        assertEquals(Connettore.EnumSslType.CLIENT, Connettore.EnumSslType.valueOf("CLIENT"));
        assertEquals(Connettore.EnumSslType.SERVER, Connettore.EnumSslType.valueOf("SERVER"));
    }

    @Test
    void testConstants() {
        assertEquals("URL", Connettore.P_URL_NAME);
        assertEquals("TIPOAUTENTICAZIONE", Connettore.P_TIPOAUTENTICAZIONE_NAME);
        assertEquals("HTTPUSER", Connettore.P_HTTPUSER_NAME);
        assertEquals("HTTPPASSW", Connettore.P_HTTPPASSW_NAME);
        assertEquals("TIPOSSL", Connettore.P_TIPOSSL_NAME);
        assertEquals("SSLKSTYPE", Connettore.P_SSLKSTYPE_NAME);
        assertEquals("SSLKSLOCATION", Connettore.P_SSLKSLOCATION_NAME);
        assertEquals("SSLKSPASSWD", Connettore.P_SSLKSPASS_WORD_NAME);
        assertEquals("SSLPKEYPASSWD", Connettore.P_SSLPKEYPASS_WORD_NAME);
        assertEquals("SSLTSTYPE", Connettore.P_SSLTSTYPE_NAME);
        assertEquals("SSLTSLOCATION", Connettore.P_SSLTSLOCATION_NAME);
        assertEquals("SSLTSPASSWD", Connettore.P_SSLTSPASS_WORD_NAME);
        assertEquals("SSLTYPE", Connettore.P_SSLTYPE_NAME);
        assertEquals("HTTP_HEADER_AUTH_HEADER_NAME", Connettore.P_HTTP_HEADER_AUTH_HEADER_NAME_NAME);
        assertEquals("HTTP_HEADER_AUTH_HEADER_VALUE", Connettore.P_HTTP_HEADER_AUTH_HEADER_VALUE_NAME);
        assertEquals("API_KEY_AUTH_API_KEY_NAME", Connettore.P_API_KEY_AUTH_API_KEY_NAME);
        assertEquals("API_KEY_AUTH_API_ID_NAME", Connettore.P_API_KEY_AUTH_API_ID_NAME);
        assertEquals("OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME", Connettore.P_OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME);
        assertEquals("OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME", Connettore.P_OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME);
        assertEquals("OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME", Connettore.P_OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME);
        assertEquals("OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME", Connettore.P_OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME);
        assertEquals("SUBSCRIPTION_KEY_VALUE", Connettore.P_SUBSCRIPTION_KEY_VALUE);
        assertEquals("ABILITATO", Connettore.P_ABILITATO);
        assertEquals("CONNECTION_TIMEOUT", Connettore.P_CONNECTION_TIMEOUT);
        assertEquals("READ_TIMEOUT", Connettore.P_READ_TIMEOUT);
        assertEquals("X-CUSTOM-HEADER-NAME-", Connettore.P_CUSTOM_HEADER_NAME_PREFIX);
        assertEquals("X-CUSTOM-HEADER-VALUE-", Connettore.P_CUSTOM_HEADER_VALUE_PREFIX);
    }

    @Test
    void testEqualsAndHashCode() {
        Connettore conn1 = Connettore.builder()
                .idConnettore("TEST")
                .url("https://test.com")
                .build();

        Connettore conn2 = Connettore.builder()
                .idConnettore("TEST")
                .url("https://test.com")
                .build();

        assertEquals(conn1, conn2);
        assertEquals(conn1.hashCode(), conn2.hashCode());
    }

    @Test
    void testToString() {
        Connettore connettore = Connettore.builder()
                .idConnettore("TEST")
                .url("https://test.com")
                .build();

        String toString = connettore.toString();
        assertTrue(toString.contains("TEST"));
        assertTrue(toString.contains("https://test.com"));
    }

    @Test
    void testSettersAndGetters() {
        Connettore connettore = new Connettore();

        connettore.setIdConnettore("SET_TEST");
        connettore.setUrl("https://setter.com");
        connettore.setTipoAutenticazione(TipoAutenticazione.API_KEY);
        connettore.setAbilitato(false);
        connettore.setConnectionTimeout(15000);
        connettore.setReadTimeout(90000);

        assertEquals("SET_TEST", connettore.getIdConnettore());
        assertEquals("https://setter.com", connettore.getUrl());
        assertEquals(TipoAutenticazione.API_KEY, connettore.getTipoAutenticazione());
        assertFalse(connettore.isAbilitato());
        assertEquals(15000, connettore.getConnectionTimeout());
        assertEquals(90000, connettore.getReadTimeout());
    }

    @Test
    void testAllArgsConstructor() {
        Map<String, String> headers = new HashMap<>();
        headers.put("H1", "V1");

        Connettore connettore = new Connettore(
                "ALL_ARGS",
                "https://allargs.com",
                TipoAutenticazione.NONE,
                "user",
                "pass",
                Connettore.EnumSslType.CLIENT,
                "JKS",
                "/ks",
                "kspass",
                "pkeypass",
                "PKCS12",
                "/ts",
                "tspass",
                "TLS",
                "HeaderName",
                "HeaderValue",
                "apiKey",
                "apiId",
                "clientId",
                "clientSecret",
                "tokenUrl",
                "scope",
                "subKey",
                headers,
                true,
                5000,
                30000
        );

        assertEquals("ALL_ARGS", connettore.getIdConnettore());
        assertEquals("https://allargs.com", connettore.getUrl());
        assertEquals(TipoAutenticazione.NONE, connettore.getTipoAutenticazione());
        assertEquals("user", connettore.getHttpUser());
        assertEquals("pass", connettore.getHttpPassw());
        assertEquals(Connettore.EnumSslType.CLIENT, connettore.getTipoSsl());
        assertNotNull(connettore.getCustomHeaders());
        assertEquals(1, connettore.getCustomHeaders().size());
    }
}
