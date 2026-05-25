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

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeSerializerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private SerializationContext serializerProvider;

    @Test
    @DisplayName("Serializzazione con pattern di default")
    void serialize_defaultPattern() throws IOException {
        OffsetDateTimeSerializer serializer = new OffsetDateTimeSerializer();
        OffsetDateTime dateTime = OffsetDateTime.of(2025, 3, 12, 10, 30, 0, 123000000, ZoneOffset.ofHours(1));

        serializer.serialize(dateTime, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("2025-03-12T10:30:00.123+01:00");
    }

    @Test
    @DisplayName("Serializzazione con pattern custom")
    void serialize_customPattern() throws IOException {
        OffsetDateTimeSerializer serializer = new OffsetDateTimeSerializer("yyyy-MM-dd");
        OffsetDateTime dateTime = OffsetDateTime.of(2025, 3, 12, 10, 30, 0, 0, ZoneOffset.UTC);

        serializer.serialize(dateTime, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("2025-03-12");
    }

    @Test
    @DisplayName("Serializzazione con null scrive null")
    void serialize_null() throws IOException {
        OffsetDateTimeSerializer serializer = new OffsetDateTimeSerializer();

        serializer.serialize(null, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString((String) null);
    }
}
