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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConnettoreMapUtilsTest {

    private Map<String, String> map;

    @BeforeEach
    void setUp() {
        map = new HashMap<>();
        map.put("URL", "https://api.example.com");
        map.put("ABILITATO", "true");
        map.put("DISABILITATO", "false");
        map.put("TIMEOUT", "30000");
        map.put("MAX_RETRY", "5");
        map.put("LONG_VALUE", "9999999999");
        map.put("RATE", "0.75");
        map.put("EMPTY", "");
        map.put("NOT_A_NUMBER", "abc");
        map.put("SPACED_INT", " 42 ");
    }

    @Nested
    @DisplayName("getString")
    class GetStringTest {

        @Test
        @DisplayName("chiave presente restituisce Optional con valore")
        void presentKey() {
            assertTrue(ConnettoreMapUtils.getString(map, "URL").isPresent());
            assertEquals("https://api.example.com", ConnettoreMapUtils.getString(map, "URL").get());
        }

        @Test
        @DisplayName("chiave assente restituisce Optional vuoto")
        void missingKey() {
            assertTrue(ConnettoreMapUtils.getString(map, "INESISTENTE").isEmpty());
        }

        @Test
        @DisplayName("chiave presente con default restituisce valore")
        void presentKeyWithDefault() {
            assertEquals("https://api.example.com", ConnettoreMapUtils.getString(map, "URL", "fallback"));
        }

        @Test
        @DisplayName("chiave assente con default restituisce default")
        void missingKeyWithDefault() {
            assertEquals("fallback", ConnettoreMapUtils.getString(map, "INESISTENTE", "fallback"));
        }

        @Test
        @DisplayName("valore vuoto restituisce stringa vuota")
        void emptyValue() {
            assertEquals("", ConnettoreMapUtils.getString(map, "EMPTY", "fallback"));
        }
    }

    @Nested
    @DisplayName("getBoolean")
    class GetBooleanTest {

        @Test
        @DisplayName("valore true")
        void trueValue() {
            assertTrue(ConnettoreMapUtils.getBoolean(map, "ABILITATO").isPresent());
            assertTrue(ConnettoreMapUtils.getBoolean(map, "ABILITATO").get());
        }

        @Test
        @DisplayName("valore false")
        void falseValue() {
            assertTrue(ConnettoreMapUtils.getBoolean(map, "DISABILITATO").isPresent());
            assertFalse(ConnettoreMapUtils.getBoolean(map, "DISABILITATO").get());
        }

        @Test
        @DisplayName("chiave assente restituisce Optional vuoto")
        void missingKey() {
            assertTrue(ConnettoreMapUtils.getBoolean(map, "INESISTENTE").isEmpty());
        }

        @Test
        @DisplayName("valore non booleano restituisce false (Boolean.parseBoolean)")
        void nonBooleanValue() {
            assertFalse(ConnettoreMapUtils.getBoolean(map, "URL").get());
        }

        @Test
        @DisplayName("chiave assente con default true restituisce true")
        void missingKeyWithDefaultTrue() {
            assertTrue(ConnettoreMapUtils.getBoolean(map, "INESISTENTE", true));
        }

        @Test
        @DisplayName("chiave assente con default false restituisce false")
        void missingKeyWithDefaultFalse() {
            assertFalse(ConnettoreMapUtils.getBoolean(map, "INESISTENTE", false));
        }

        @Test
        @DisplayName("chiave presente ignora default")
        void presentKeyIgnoresDefault() {
            assertTrue(ConnettoreMapUtils.getBoolean(map, "ABILITATO", false));
        }
    }

    @Nested
    @DisplayName("getInteger")
    class GetIntegerTest {

        @Test
        @DisplayName("valore numerico valido")
        void validInt() {
            assertTrue(ConnettoreMapUtils.getInteger(map, "TIMEOUT").isPresent());
            assertEquals(30000, ConnettoreMapUtils.getInteger(map, "TIMEOUT").get());
        }

        @Test
        @DisplayName("chiave assente restituisce Optional vuoto")
        void missingKey() {
            assertTrue(ConnettoreMapUtils.getInteger(map, "INESISTENTE").isEmpty());
        }

        @Test
        @DisplayName("valore non numerico restituisce Optional vuoto")
        void nonNumericValue() {
            assertTrue(ConnettoreMapUtils.getInteger(map, "NOT_A_NUMBER").isEmpty());
        }

        @Test
        @DisplayName("valore con spazi viene trimmato")
        void spacedValue() {
            assertEquals(42, ConnettoreMapUtils.getInteger(map, "SPACED_INT").get());
        }

        @Test
        @DisplayName("chiave assente con default restituisce default")
        void missingKeyWithDefault() {
            assertEquals(5000, ConnettoreMapUtils.getInteger(map, "INESISTENTE", 5000));
        }

        @Test
        @DisplayName("valore non numerico con default restituisce default")
        void nonNumericWithDefault() {
            assertEquals(100, ConnettoreMapUtils.getInteger(map, "NOT_A_NUMBER", 100));
        }

        @Test
        @DisplayName("valore presente ignora default")
        void presentKeyIgnoresDefault() {
            assertEquals(5, ConnettoreMapUtils.getInteger(map, "MAX_RETRY", 3));
        }
    }

    @Nested
    @DisplayName("getLong")
    class GetLongTest {

        @Test
        @DisplayName("valore long valido")
        void validLong() {
            assertTrue(ConnettoreMapUtils.getLong(map, "LONG_VALUE").isPresent());
            assertEquals(9999999999L, ConnettoreMapUtils.getLong(map, "LONG_VALUE").get());
        }

        @Test
        @DisplayName("valore int valido come long")
        void intAsLong() {
            assertEquals(30000L, ConnettoreMapUtils.getLong(map, "TIMEOUT").get());
        }

        @Test
        @DisplayName("chiave assente restituisce Optional vuoto")
        void missingKey() {
            assertTrue(ConnettoreMapUtils.getLong(map, "INESISTENTE").isEmpty());
        }

        @Test
        @DisplayName("valore non numerico restituisce Optional vuoto")
        void nonNumericValue() {
            assertTrue(ConnettoreMapUtils.getLong(map, "NOT_A_NUMBER").isEmpty());
        }

        @Test
        @DisplayName("chiave assente con default restituisce default")
        void missingKeyWithDefault() {
            assertEquals(60000L, ConnettoreMapUtils.getLong(map, "INESISTENTE", 60000L));
        }
    }

    @Nested
    @DisplayName("getDouble")
    class GetDoubleTest {

        @Test
        @DisplayName("valore double valido")
        void validDouble() {
            assertTrue(ConnettoreMapUtils.getDouble(map, "RATE").isPresent());
            assertEquals(0.75, ConnettoreMapUtils.getDouble(map, "RATE").get(), 0.001);
        }

        @Test
        @DisplayName("valore intero come double")
        void intAsDouble() {
            assertEquals(30000.0, ConnettoreMapUtils.getDouble(map, "TIMEOUT").get(), 0.001);
        }

        @Test
        @DisplayName("chiave assente restituisce Optional vuoto")
        void missingKey() {
            assertTrue(ConnettoreMapUtils.getDouble(map, "INESISTENTE").isEmpty());
        }

        @Test
        @DisplayName("valore non numerico restituisce Optional vuoto")
        void nonNumericValue() {
            assertTrue(ConnettoreMapUtils.getDouble(map, "NOT_A_NUMBER").isEmpty());
        }

        @Test
        @DisplayName("chiave assente con default restituisce default")
        void missingKeyWithDefault() {
            assertEquals(1.5, ConnettoreMapUtils.getDouble(map, "INESISTENTE", 1.5), 0.001);
        }

        @Test
        @DisplayName("valore non numerico con default restituisce default")
        void nonNumericWithDefault() {
            assertEquals(2.0, ConnettoreMapUtils.getDouble(map, "NOT_A_NUMBER", 2.0), 0.001);
        }
    }
}
