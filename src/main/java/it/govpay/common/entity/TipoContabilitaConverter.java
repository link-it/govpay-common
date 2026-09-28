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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converte {@link TipoContabilita} da/verso la colonna {@code VARCHAR(1)} codificata.
 * Una codifica ignota diventa {@code null} con un log {@code WARN}, non un'eccezione: una
 * riga anomala (es. dato storico con una codifica dismessa) non deve far fallire la lettura
 * di un'intera lista.
 */
@Converter
public class TipoContabilitaConverter implements AttributeConverter<TipoContabilita, String> {

    private static final Logger log = LoggerFactory.getLogger(TipoContabilitaConverter.class);

    @Override
    public String convertToDatabaseColumn(TipoContabilita attribute) {
        return attribute == null ? null : attribute.getCodifica();
    }

    @Override
    public TipoContabilita convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        TipoContabilita valore = TipoContabilita.daCodifica(dbData);
        if (valore == null) {
            log.warn("Codifica tipo_contabilita sconosciuta [{}]: valorizzato a null", dbData);
        }
        return valore;
    }
}
