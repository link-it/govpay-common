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

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;

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
            when(jsonParser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
            when(jsonParser.getString()).thenReturn("2025-03-12");

            LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

            assertEquals(LocalDate.of(2025, 3, 12), result);
        }

        @Test
        @DisplayName("Token non VALUE_STRING restituisce null")
        void nonStringToken() throws IOException {
            when(jsonParser.currentToken()).thenReturn(JsonToken.VALUE_NULL);

            LocalDate result = deserializer.deserialize(jsonParser, deserializationContext);

            assertNull(result);
        }

        @Test
        @DisplayName("Formato invalido lancia DateTimeParseException")
        void invalidFormatThrowsException() {
            when(jsonParser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
            when(jsonParser.getString()).thenReturn("invalid-date");

            assertThrows(java.time.format.DateTimeParseException.class, () ->
                    deserializer.deserialize(jsonParser, deserializationContext));
        }
    }
}
