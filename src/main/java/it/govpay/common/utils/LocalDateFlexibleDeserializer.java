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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Custom deserializer for LocalDate that can handle multiple date formats.
 * <p>
 * Handles both:
 * <ul>
 *   <li>Standard date format: "2025-03-12"</li>
 *   <li>Full datetime format: "2025-03-12T00:00:00.000000+02:00"</li>
 * </ul>
 * <p>
 * This is needed because pagoPA API sometimes sends datetime strings for date fields.
 * <p>
 * Usage example:
 * <pre>
 * &#64;JsonDeserialize(using = LocalDateFlexibleDeserializer.class)
 * private LocalDate dataScadenza;
 * </pre>
 */
public class LocalDateFlexibleDeserializer extends StdScalarDeserializer<LocalDate> {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public LocalDateFlexibleDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        JsonToken currentToken = jsonParser.currentToken();
        if (currentToken == JsonToken.VALUE_STRING) {
            return parseLocalDate(jsonParser.getString());
        }
        return null;
    }

    /**
     * Parses a LocalDate from string with multiple strategies:
     * <ol>
     *   <li>Try parsing as standard LocalDate (yyyy-MM-dd)</li>
     *   <li>Try parsing as OffsetDateTime and extract date part</li>
     *   <li>Try parsing as LocalDateTime and extract date part</li>
     * </ol>
     *
     * @param value the date string to parse
     * @return parsed LocalDate or null if value is null/empty
     * @throws DateTimeParseException if all parsing attempts fail
     */
    public LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String dateString = value.trim();

        // First attempt: parse as standard LocalDate (yyyy-MM-dd)
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            // Continue to next attempt
        }

        // Second attempt: parse as OffsetDateTime and extract date
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateString,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return offsetDateTime.toLocalDate();
        } catch (DateTimeParseException e2) {
            // Continue to next attempt
        }

        // Third attempt: parse as LocalDateTime (without timezone) and extract date
        try {
            return LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e3) {
            // All attempts failed, throw descriptive exception
            throw new DateTimeParseException(
                    "Unable to parse date '" + dateString + "' with any supported format",
                    dateString,
                    0);
        }
    }
}
