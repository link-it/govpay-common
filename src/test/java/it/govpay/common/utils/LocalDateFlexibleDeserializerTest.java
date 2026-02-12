package it.govpay.common.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;

@ExtendWith(MockitoExtension.class)
class LocalDateFlexibleDeserializerTest {

    private LocalDateFlexibleDeserializer deserializer;

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    @BeforeEach
    void setUp() {
        deserializer = new LocalDateFlexibleDeserializer();
    }

    @Nested
    @DisplayName("parseLocalDate")
    class ParseLocalDate {

        @Test
        @DisplayName("Formato standard yyyy-MM-dd")
        void standardFormat() {
            LocalDate result = deserializer.parseLocalDate("2025-03-12");
            assertEquals(LocalDate.of(2025, 3, 12), result);
        }

        @Test
        @DisplayName("Formato OffsetDateTime")
        void offsetDateTimeFormat() {
            LocalDate result = deserializer.parseLocalDate("2025-03-12T00:00:00.000+02:00");
            assertEquals(LocalDate.of(2025, 3, 12), result);
        }

        @Test
        @DisplayName("Formato LocalDateTime senza timezone")
        void localDateTimeFormat() {
            LocalDate result = deserializer.parseLocalDate("2025-03-12T00:00:00");
            assertEquals(LocalDate.of(2025, 3, 12), result);
        }

        @Test
        @DisplayName("Null restituisce null")
        void nullValue() {
            assertNull(deserializer.parseLocalDate(null));
        }

        @Test
        @DisplayName("Stringa vuota restituisce null")
        void emptyString() {
            assertNull(deserializer.parseLocalDate(""));
        }

        @Test
        @DisplayName("Stringa con solo spazi restituisce null")
        void blankString() {
            assertNull(deserializer.parseLocalDate("   "));
        }

        @Test
        @DisplayName("Formato invalido lancia DateTimeParseException")
        void invalidFormat() {
            assertThrows(DateTimeParseException.class, () ->
                    deserializer.parseLocalDate("not-a-date"));
        }
    }

    @Nested
    @DisplayName("deserialize")
    class Deserialize {

        @Test
        @DisplayName("VALUE_STRING token valido")
        void valueStringToken() throws IOException {
            when(jsonParser.getCurrentToken()).thenReturn(JsonToken.VALUE_STRING);
            when(jsonParser.getText()).thenReturn("2025-03-12");

            LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

            assertEquals(LocalDate.of(2025, 3, 12), result);
        }

        @Test
        @DisplayName("Token non VALUE_STRING restituisce null")
        void nonStringToken() throws IOException {
            when(jsonParser.getCurrentToken()).thenReturn(JsonToken.VALUE_NULL);

            LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

            assertNull(result);
        }

        @Test
        @DisplayName("Formato invalido lancia IOException")
        void invalidFormatThrowsIOException() throws IOException {
            when(jsonParser.getCurrentToken()).thenReturn(JsonToken.VALUE_STRING);
            when(jsonParser.getText()).thenReturn("invalid-date");

            assertThrows(IOException.class, () ->
                    deserializer.deserialize(jsonParser, deserializationContext));
        }
    }
}
