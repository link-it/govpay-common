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
package it.govpay.common.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OffsetDateTimeDeserializerTest {

    private OffsetDateTimeDeserializer deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new OffsetDateTimeDeserializer();
    }

    @Test
    @DisplayName("parseOffsetDateTime - formato standard con timezone")
    void parseOffsetDateTime_standardFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);
        OffsetDateTime result = deserializer.parseOffsetDateTime("2025-01-15T10:30:00.123+01:00", formatter);

        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(10, result.getHour());
        assertEquals(30, result.getMinute());
    }

    @Test
    @DisplayName("parseOffsetDateTime - formato senza secondi")
    void parseOffsetDateTime_withoutSeconds() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);
        OffsetDateTime result = deserializer.parseOffsetDateTime("2025-12-09T00:00+01:00", formatter);

        assertNotNull(result);
        assertEquals(2025, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(9, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
    }

    @Test
    @DisplayName("parseOffsetDateTime - millisecondi variabili")
    void parseOffsetDateTime_variableMilliseconds() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);

        // 3 cifre
        assertNotNull(deserializer.parseOffsetDateTime("2025-01-15T10:30:00.123+01:00", formatter));

        // 6 cifre
        assertNotNull(deserializer.parseOffsetDateTime("2025-01-15T10:30:00.123456+01:00", formatter));

        // 9 cifre
        assertNotNull(deserializer.parseOffsetDateTime("2025-01-15T10:30:00.123456789+01:00", formatter));
    }

    @Test
    @DisplayName("parseOffsetDateTime - senza timezone (fallback a CET)")
    void parseOffsetDateTime_withoutTimezone() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);
        OffsetDateTime result = deserializer.parseOffsetDateTime("2025-01-15T10:30:00.123", formatter);

        assertNotNull(result);
        assertEquals(2025, result.getYear());
        // Offset CET (+01:00)
        assertEquals(1, result.getOffset().getTotalSeconds() / 3600);
    }

    @Test
    @DisplayName("parseOffsetDateTime - con Z per UTC")
    void parseOffsetDateTime_withZ() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);
        OffsetDateTime result = deserializer.parseOffsetDateTime("2025-01-15T10:30:00.123Z", formatter);

        assertNotNull(result);
        assertEquals(0, result.getOffset().getTotalSeconds());
    }

    @Test
    @DisplayName("parseOffsetDateTime - null o vuoto")
    void parseOffsetDateTime_nullOrEmpty() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);

        assertNull(deserializer.parseOffsetDateTime(null, formatter));
        assertNull(deserializer.parseOffsetDateTime("", formatter));
        assertNull(deserializer.parseOffsetDateTime("   ", formatter));
    }

    @Test
    @DisplayName("parseOffsetDateTime - formato non valido")
    void parseOffsetDateTime_invalidFormat() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);

        assertThrows(DateTimeParseException.class, () ->
                deserializer.parseOffsetDateTime("invalid-date", formatter));

        assertThrows(DateTimeParseException.class, () ->
                deserializer.parseOffsetDateTime("2025-13-45T99:99:99", formatter));
    }
}
