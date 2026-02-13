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
        assertNotNull(TipoAutenticazione.valueOf("HTTPBasic"));
        assertNotNull(TipoAutenticazione.valueOf("SSL"));
        assertNotNull(TipoAutenticazione.valueOf("OAUTH2_CLIENT_CREDENTIALS"));
        assertNotNull(TipoAutenticazione.valueOf("API_KEY"));
        assertNotNull(TipoAutenticazione.valueOf("HTTP_HEADER"));
    }

    @Test
    void testGetDescrizione() {
        assertEquals("Nessuna autenticazione", TipoAutenticazione.NONE.getDescrizione());
        assertEquals("HTTP Basic Authentication", TipoAutenticazione.HTTPBasic.getDescrizione());
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
                Arguments.of("HTTPBasic", TipoAutenticazione.HTTPBasic),
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
        assertEquals(1, TipoAutenticazione.HTTPBasic.ordinal());
        assertEquals(2, TipoAutenticazione.SSL.ordinal());
        assertEquals(3, TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS.ordinal());
        assertEquals(4, TipoAutenticazione.API_KEY.ordinal());
        assertEquals(5, TipoAutenticazione.HTTP_HEADER.ordinal());
    }

    @Test
    void testName() {
        assertEquals("NONE", TipoAutenticazione.NONE.name());
        assertEquals("HTTPBasic", TipoAutenticazione.HTTPBasic.name());
        assertEquals("SSL", TipoAutenticazione.SSL.name());
        assertEquals("OAUTH2_CLIENT_CREDENTIALS", TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS.name());
        assertEquals("API_KEY", TipoAutenticazione.API_KEY.name());
        assertEquals("HTTP_HEADER", TipoAutenticazione.HTTP_HEADER.name());
    }
}
