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

    // ── genera/convertiDaNumeroAvviso ────────────────────────────────────────
    // Valori attesi calcolati indipendentemente (non solo coerenza interna fra
    // generazione e conversione), stessa formula di IuvBD.generaIuv/getCheckDigit93.

    @Test
    @DisplayName("genera - AuxDigit 0: reference=applicationCode+progressivo, check digit mod-93 con application code")
    void genera_auxDigit0() {
        IuvUtils.IdentificativiPagamento identificativi = IuvUtils.genera(0, "", null, 5, 1);

        assertEquals("000000000000115", identificativi.iuv());
        assertEquals("005000000000000115", identificativi.numeroAvviso());
    }

    @Test
    @DisplayName("genera - AuxDigit 1: reference di 15 cifre, check digit mod-93 semplice")
    void genera_auxDigit1() {
        IuvUtils.IdentificativiPagamento identificativi = IuvUtils.genera(1, "", null, null, 1);

        assertEquals("00000000000000102", identificativi.iuv());
        assertEquals("100000000000000102", identificativi.numeroAvviso());
    }

    @Test
    @DisplayName("genera - AuxDigit 2 con prefisso numerico: il prefisso occupa le prime cifre della reference")
    void genera_auxDigit2ConPrefisso() {
        IuvUtils.IdentificativiPagamento identificativi = IuvUtils.genera(2, "12", null, null, 42);

        assertEquals("12000000000004259", identificativi.iuv());
        assertEquals("212000000000004259", identificativi.numeroAvviso());
    }

    @Test
    @DisplayName("genera - AuxDigit 3: iuv = codice di segregazione + reference + check digit")
    void genera_auxDigit3() {
        IuvUtils.IdentificativiPagamento identificativi = IuvUtils.genera(3, "", 12, null, 7);

        assertEquals("12000000000000725", identificativi.iuv());
        assertEquals("312000000000000725", identificativi.numeroAvviso());
    }

    @Test
    @DisplayName("genera - AuxDigit 0 senza application code e' un errore di configurazione")
    void genera_auxDigit0SenzaApplicationCode() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> IuvUtils.genera(0, "", null, null, 1));
        assertTrue(e.getMessage().contains("application code"));
    }

    @Test
    @DisplayName("genera - AuxDigit 3 senza codice di segregazione e' un errore di configurazione")
    void genera_auxDigit3SenzaSegregationCode() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> IuvUtils.genera(3, "", null, null, 1));
        assertTrue(e.getMessage().contains("segregazione"));
    }

    @Test
    @DisplayName("genera - un prefisso quasi al limite non deve produrre un numero avviso piu' lungo di 18 cifre "
            + "(il controllo di lunghezza deve usare il limite del singolo AuxDigit, non sempre 15)")
    void genera_rifiutaPrefissoCheSforaLaLunghezza() {
        // AuxDigit 3/0 hanno reference di 13 cifre, non 15: un controllo che verifica sempre
        // "> 15" lascia passare una reference di 14 cifre (prefisso 12 cifre + progressivo di
        // 2 cifre), producendo un IUV di 18 cifre e un numeroAvviso di 19 invece di 18.
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> IuvUtils.genera(3, "123456789012", 12, null, 10));
        assertTrue(e.getMessage().contains("123456789012"));
    }

    @Test
    @DisplayName("convertiDaNumeroAvviso - ricava lo stesso iuv generato, per ciascun AuxDigit (round trip)")
    void convertiDaNumeroAvviso_roundTrip() {
        IuvUtils.IdentificativiPagamento aux0 = IuvUtils.genera(0, "", null, 5, 1);
        assertEquals(aux0.iuv(), IuvUtils.convertiDaNumeroAvviso(aux0.numeroAvviso(), 0, null, 5));

        IuvUtils.IdentificativiPagamento aux1 = IuvUtils.genera(1, "", null, null, 1);
        assertEquals(aux1.iuv(), IuvUtils.convertiDaNumeroAvviso(aux1.numeroAvviso(), 1, null, null));

        IuvUtils.IdentificativiPagamento aux3 = IuvUtils.genera(3, "", 12, null, 7);
        assertEquals(aux3.iuv(), IuvUtils.convertiDaNumeroAvviso(aux3.numeroAvviso(), 3, 12, null));
    }

    @Test
    @DisplayName("convertiDaNumeroAvviso - rifiuta un numeroAvviso con AuxDigit diverso da quello configurato sul dominio")
    void convertiDaNumeroAvviso_rifiutaAuxDigitDiverso() {
        String numeroAvviso = IuvUtils.genera(1, "", null, null, 1).numeroAvviso();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> IuvUtils.convertiDaNumeroAvviso(numeroAvviso, 2, null, null));
        assertTrue(e.getMessage().contains("AuxDigit"));
    }

    @Test
    @DisplayName("convertiDaNumeroAvviso - rifiuta un numeroAvviso con check digit alterato")
    void convertiDaNumeroAvviso_rifiutaCheckDigitAlterato() {
        String numeroAvviso = IuvUtils.genera(1, "", null, null, 1).numeroAvviso();
        String alterato = numeroAvviso.substring(0, numeroAvviso.length() - 1)
                + (numeroAvviso.charAt(numeroAvviso.length() - 1) == '0' ? '1' : '0');

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> IuvUtils.convertiDaNumeroAvviso(alterato, 1, null, null));
        assertTrue(e.getMessage().contains("check digit"));
    }

    @Test
    @DisplayName("convertiDaNumeroAvviso - rifiuta un formato non a 18 cifre numeriche")
    void convertiDaNumeroAvviso_rifiutaFormatoNonValido() {
        assertThrows(IllegalArgumentException.class, () -> IuvUtils.convertiDaNumeroAvviso("123", 1, null, null));
        assertThrows(IllegalArgumentException.class, () -> IuvUtils.convertiDaNumeroAvviso(null, 1, null, null));
    }
}
