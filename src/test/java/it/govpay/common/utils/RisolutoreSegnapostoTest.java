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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RisolutoreSegnapostoTest {

    @Test
    @DisplayName("null e vuoto restano vuoti, senza segnaposto da risolvere")
    void testoAssente() {
        assertEquals("", RisolutoreSegnaposto.risolvi(null, Map.of()));
        assertEquals("", RisolutoreSegnaposto.risolvi("", Map.of()));
    }

    @Test
    @DisplayName("un testo senza segnaposto resta invariato")
    void testoLetterale() {
        assertEquals("12345", RisolutoreSegnaposto.risolvi("12345", Map.of()));
    }

    @Test
    @DisplayName("sostituisce piu' segnaposto diversi nello stesso testo")
    void sostituiscePiuSegnaposto() {
        Map<String, String> valori = Map.of("a", "007", "Y", "2026");

        assertEquals("0072026", RisolutoreSegnaposto.risolvi("%(a)%(Y)", valori));
    }

    @Test
    @DisplayName("un segnaposto senza valore noto solleva un errore esplicito, non lo lascia nel testo")
    void segnapostoNonRisolvibile() {
        Map<String, String> valori = new HashMap<>();
        valori.put("Y", "2026");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> RisolutoreSegnaposto.risolvi("%(p)001", valori));
        assertTrue(e.getMessage().contains("%(p)"));
    }
}
