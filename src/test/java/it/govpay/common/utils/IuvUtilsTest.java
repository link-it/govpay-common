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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import it.govpay.common.entity.DominioEntity;

class IuvUtilsTest {

    @Test
    @DisplayName("isNumeric - stringa numerica")
    void isNumeric_numericString() {
        assertTrue(IuvUtils.isNumeric("123456789012345"));
        assertTrue(IuvUtils.isNumeric("0"));
        assertTrue(IuvUtils.isNumeric("99999999999999999"));
    }

    @Test
    @DisplayName("isNumeric - stringa non numerica")
    void isNumeric_nonNumericString() {
        assertFalse(IuvUtils.isNumeric("RF12345678901234"));
        assertFalse(IuvUtils.isNumeric("abc"));
        assertFalse(IuvUtils.isNumeric("12.34"));
        assertFalse(IuvUtils.isNumeric(""));
        assertFalse(IuvUtils.isNumeric(null));
    }

    @Test
    @DisplayName("isIuvInterno - AuxDigit 0 con IUV numerico 15 cifre")
    void isIuvInterno_auxDigit0_numerico15cifre() {
        assertTrue(IuvUtils.isIuvInterno("12345678901", 0, null, "123456789012345"));
    }

    @Test
    @DisplayName("isIuvInterno - AuxDigit 0 con IUV numerico non 15 cifre")
    void isIuvInterno_auxDigit0_numericoNon15cifre() {
        assertFalse(IuvUtils.isIuvInterno("12345678901", 0, null, "12345678901234567")); // 17 cifre
        assertFalse(IuvUtils.isIuvInterno("12345678901", 0, null, "1234567890123")); // 13 cifre
    }

    @Test
    @DisplayName("isIuvInterno - AuxDigit 1 con IUV numerico 17 cifre")
    void isIuvInterno_auxDigit1_numerico17cifre() {
        assertTrue(IuvUtils.isIuvInterno("12345678901", 1, null, "12345678901234567"));
    }

    @Test
    @DisplayName("isIuvInterno - AuxDigit 1 con IUV numerico non 17 cifre")
    void isIuvInterno_auxDigit1_numericoNon17cifre() {
        assertFalse(IuvUtils.isIuvInterno("12345678901", 1, null, "123456789012345")); // 15 cifre
    }

    @Test
    @DisplayName("isIuvInterno - AuxDigit 3 con IUV RF e codice segregazione")
    void isIuvInterno_auxDigit3_rfConCodiceSegregazione() {
        // RF + 2 check digit + 2 codice segregazione + resto
        assertTrue(IuvUtils.isIuvInterno("12345678901", 3, 49, "RF1249ABCDEFGHIJ"));
        assertTrue(IuvUtils.isIuvInterno("12345678901", 3, 1, "RF0001ABCDEFGHIJ"));
    }

    @Test
    @DisplayName("isIuvInterno - AuxDigit 3 con IUV numerico 17 cifre")
    void isIuvInterno_auxDigit3_numerico17cifreConCodiceSegregazione() {
        // Inizia con codice segregazione (2 cifre)
        assertTrue(IuvUtils.isIuvInterno("12345678901", 3, 49, "49123456789012345"));
        assertTrue(IuvUtils.isIuvInterno("12345678901", 3, 1, "01123456789012345"));
    }

    @Test
    @DisplayName("isIuvInterno - AuxDigit 3 senza codice segregazione corrispondente")
    void isIuvInterno_auxDigit3_senzaCodiceSegregazioneCorrispondente() {
        assertFalse(IuvUtils.isIuvInterno("12345678901", 3, 49, "RF1250ABCDEFGHIJ")); // codice diverso
        assertFalse(IuvUtils.isIuvInterno("12345678901", 3, 49, "50123456789012345")); // inizia con 50, non 49
    }

    @Test
    @DisplayName("isIuvInterno - dominio null")
    void isIuvInterno_dominioNull() {
        assertFalse(IuvUtils.isIuvInterno(null, "123456789012345"));
    }

    @Test
    @DisplayName("isIuvInterno - IUV null o vuoto")
    void isIuvInterno_iuvNullOrEmpty() {
        assertFalse(IuvUtils.isIuvInterno("12345678901", 0, null, null));
        assertFalse(IuvUtils.isIuvInterno("12345678901", 0, null, ""));
    }

    @Test
    @DisplayName("isIuvInterno - con DominioEntity")
    void isIuvInterno_conDominioInfo() {
    	DominioEntity dominio = new DominioEntity();
    	dominio.setCodDominio("12345678901");
    	dominio.setAuxDigit(0);
    	dominio.setSegregationCode(null);

        assertTrue(IuvUtils.isIuvInterno(dominio, "123456789012345"));
        assertFalse(IuvUtils.isIuvInterno(dominio, "12345678901234567"));
    }
}
