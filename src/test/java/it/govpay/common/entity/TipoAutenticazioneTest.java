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
package it.govpay.common.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TipoAutenticazioneTest {

    @Test
    void testEnumValues() {
        assertEquals(6, TipoAutenticazione.values().length);
        assertNotNull(TipoAutenticazione.valueOf("NONE"));
        assertNotNull(TipoAutenticazione.valueOf("HTTP_BASIC"));
        assertNotNull(TipoAutenticazione.valueOf("SSL"));
        assertNotNull(TipoAutenticazione.valueOf("OAUTH2_CLIENT_CREDENTIALS"));
        assertNotNull(TipoAutenticazione.valueOf("API_KEY"));
        assertNotNull(TipoAutenticazione.valueOf("HTTP_HEADER"));
    }

    @Test
    void testGetDescrizione() {
        assertEquals("Nessuna autenticazione", TipoAutenticazione.NONE.getDescrizione());
        assertEquals("HTTP Basic Authentication", TipoAutenticazione.HTTP_BASIC.getDescrizione());
        assertEquals("SSL/TLS con certificati client", TipoAutenticazione.SSL.getDescrizione());
        assertEquals("OAuth2 Client Credentials", TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS.getDescrizione());
        assertEquals("API Key Authentication", TipoAutenticazione.API_KEY.getDescrizione());
        assertEquals("Custom HTTP Header Authentication", TipoAutenticazione.HTTP_HEADER.getDescrizione());
    }

    @ParameterizedTest
    @MethodSource("provideAuthTypeMapping")
    void testFromGovPayAuthType(String input, TipoAutenticazione expected) {
        assertEquals(expected, TipoAutenticazione.fromGovPayAuthType(input));
    }

    private static Stream<Arguments> provideAuthTypeMapping() {
        return Stream.of(
                Arguments.of("NONE", TipoAutenticazione.NONE),
                Arguments.of("HTTPBasic", TipoAutenticazione.HTTP_BASIC),
                Arguments.of("SSL", TipoAutenticazione.SSL),
                Arguments.of("OAUTH2_CLIENT_CREDENTIALS", TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS),
                Arguments.of("API_KEY", TipoAutenticazione.API_KEY),
                Arguments.of("HTTP_HEADER", TipoAutenticazione.HTTP_HEADER)
        );
    }

    @Test
    void testFromGovPayAuthTypeNull() {
        assertEquals(TipoAutenticazione.NONE, TipoAutenticazione.fromGovPayAuthType(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "invalid", "basic", "oauth2", "", " "})
    void testFromGovPayAuthTypeUnknown(String input) {
        assertEquals(TipoAutenticazione.NONE, TipoAutenticazione.fromGovPayAuthType(input));
    }

    @Test
    void testFromGovPayAuthTypeCaseSensitive() {
        // Test case sensitivity - these should return NONE
        assertEquals(TipoAutenticazione.NONE, TipoAutenticazione.fromGovPayAuthType("none"));
        assertEquals(TipoAutenticazione.NONE, TipoAutenticazione.fromGovPayAuthType("httpbasic"));
        assertEquals(TipoAutenticazione.NONE, TipoAutenticazione.fromGovPayAuthType("ssl"));
        assertEquals(TipoAutenticazione.NONE, TipoAutenticazione.fromGovPayAuthType("api_key"));
    }

    @Test
    void testDescrizioneNotNull() {
        for (TipoAutenticazione tipo : TipoAutenticazione.values()) {
            assertNotNull(tipo.getDescrizione());
            assertFalse(tipo.getDescrizione().isEmpty());
        }
    }

    @Test
    void testOrdinal() {
        assertEquals(0, TipoAutenticazione.NONE.ordinal());
        assertEquals(1, TipoAutenticazione.HTTP_BASIC.ordinal());
        assertEquals(2, TipoAutenticazione.SSL.ordinal());
        assertEquals(3, TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS.ordinal());
        assertEquals(4, TipoAutenticazione.API_KEY.ordinal());
        assertEquals(5, TipoAutenticazione.HTTP_HEADER.ordinal());
    }

    @Test
    void testName() {
        assertEquals("NONE", TipoAutenticazione.NONE.name());
        assertEquals("HTTP_BASIC", TipoAutenticazione.HTTP_BASIC.name());
        assertEquals("SSL", TipoAutenticazione.SSL.name());
        assertEquals("OAUTH2_CLIENT_CREDENTIALS", TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS.name());
        assertEquals("API_KEY", TipoAutenticazione.API_KEY.name());
        assertEquals("HTTP_HEADER", TipoAutenticazione.HTTP_HEADER.name());
    }
}
