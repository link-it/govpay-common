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

/**
 * Tipo di contabilità di un tributo, condiviso fra {@code tributi.tipo_contabilita},
 * {@code tipi_tributo.tipo_contabilita} e {@code singoli_versamenti.tipo_contabilita}
 * (stessa codifica {@code VARCHAR(1)} in tutte e tre le tabelle — issue
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>).
 * Le codifiche {@code 3}/{@code 4}/{@code 5} non sono assegnate a nessun valore.
 */
public enum TipoContabilita {
    CAPITOLO("0"),
    SPECIALE("1"),
    SIOPE("2"),
    SRTP_ESCLUSA_RAVV_OPEROSO("6"),
    SRTP_ESCLUSA_ALTRO_OPERATORE("7"),
    SRTP_ESCLUSA("8"),
    ALTRO("9");

    private final String codifica;

    TipoContabilita(String codifica) {
        this.codifica = codifica;
    }

    public String getCodifica() {
        return codifica;
    }

    public static TipoContabilita daCodifica(String codifica) {
        for (TipoContabilita valore : values()) {
            if (valore.codifica.equals(codifica)) {
                return valore;
            }
        }
        return null;
    }
}
