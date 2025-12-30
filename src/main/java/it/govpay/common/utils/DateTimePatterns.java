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

/**
 * Costanti per i pattern di serializzazione/deserializzazione date.
 * <p>
 * Questi pattern sono utilizzati per gestire i formati variabili delle date
 * nelle API pagoPA.
 */
public final class DateTimePatterns {

    private DateTimePatterns() {
        // Utility class
    }

    /**
     * Pattern con millisecondi variabili (1-9 cifre) per deserializzazione sicura da pagoPA.
     */
    public static final String PATTERN_YYYY_MM_DD_T_HH_MM_SS_MILLIS_VARIABILI =
            "yyyy-MM-dd'T'HH:mm:ss[.[SSSSSSSSS][SSSSSSSS][SSSSSSS][SSSSSS][SSSSS][SSSS][SSS][SS][S]]";

    /**
     * Pattern con millisecondi variabili e timezone XXX.
     */
    public static final String PATTERN_YYYY_MM_DD_T_HH_MM_SS_MILLIS_VARIABILI_XXX =
            "yyyy-MM-dd'T'HH:mm:ss[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]XXX";

    /**
     * Pattern per serializzazione date al GDE (3 cifre millisecondi con timezone).
     * Formato: yyyy-MM-dd'T'HH:mm:ss.SSSXXX (es. 2025-01-15T10:30:00.123+01:00)
     */
    public static final String PATTERN_TIMESTAMP_3_YYYY_MM_DD_T_HH_MM_SS_SSSXXX =
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /**
     * Pattern per serializzazione date con 6 cifre millisecondi e timezone.
     * Formato: yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX
     */
    public static final String PATTERN_TIMESTAMP_6_YYYY_MM_DD_T_HH_MM_SS_SSSXXX =
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX";

    /**
     * Pattern per serializzazione date JSON senza timezone (3 cifre millisecondi).
     * Formato: yyyy-MM-dd'T'HH:mm:ss.SSS
     */
    public static final String PATTERN_DATA_JSON_YYYY_MM_DD_T_HH_MM_SS_SSS =
            "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * Timezone di default per l'applicazione.
     */
    public static final String DEFAULT_TIME_ZONE = "Europe/Rome";
}
