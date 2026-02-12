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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeSerializerTest {

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private SerializerProvider serializerProvider;

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
