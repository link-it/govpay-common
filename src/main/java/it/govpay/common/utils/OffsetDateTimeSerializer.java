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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdScalarSerializer;

/**
 * Custom serializer for OffsetDateTime to ensure consistent date format in JSON output.
 * <p>
 * Uses configurable date pattern for serialization (default: yyyy-MM-dd'T'HH:mm:ss.SSSXXX).
 * <p>
 * Usage example:
 * <pre>
 * &#64;JsonSerialize(using = OffsetDateTimeSerializer.class)
 * private OffsetDateTime timestamp;
 * </pre>
 * Or globally in ObjectMapper:
 * <pre>
 * SimpleModule module = new SimpleModule();
 * module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
 * objectMapper.registerModule(module);
 * </pre>
 */
public class OffsetDateTimeSerializer extends StdScalarSerializer<OffsetDateTime> {

    private static final long serialVersionUID = 1L;

    private transient DateTimeFormatter formatter;

    /**
     * Default constructor using standard timestamp format with timezone.
     * Pattern: yyyy-MM-dd'T'HH:mm:ss.SSSXXX
     */
    public OffsetDateTimeSerializer() {
        this(DateTimePatterns.PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX);
    }

    /**
     * Constructor with custom date format pattern.
     *
     * @param format the date format pattern to use for serialization
     */
    public OffsetDateTimeSerializer(String format) {
        super(OffsetDateTime.class);
        this.formatter = DateTimeFormatter.ofPattern(format);
    }

    @Override
    public void serialize(OffsetDateTime dateTime, JsonGenerator jsonGenerator, SerializationContext provider) {
        String dateTimeAsString = dateTime != null ? this.formatter.format(dateTime) : null;
        jsonGenerator.writeString(dateTimeAsString);
    }
}
