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
package it.govpay.common.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DominioEntityTest {

    /** La colonna pagamento_sito e' NOT NULL: un dominio nuovo deve partire da false, non da null. */
    @Test
    void testPagamentoSitoFalseConCostruttoreSenzaArgomenti() {
        assertFalse(new DominioEntity().getPagamentoSito());
    }

    @Test
    void testPagamentoSitoFalseConBuilder() {
        assertFalse(DominioEntity.builder().codDominio("01234567890").build().getPagamentoSito());
        assertTrue(DominioEntity.builder().pagamentoSito(true).build().getPagamentoSito());
    }
}
