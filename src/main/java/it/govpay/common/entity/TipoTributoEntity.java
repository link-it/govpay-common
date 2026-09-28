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

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Tipo di tributo (entrata) globale (tabella {@code tipi_tributo}). Chiave naturale
 * {@code cod_tributo} — issue
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>.
 */
@Entity
@Table(name = "tipi_tributo")
@SequenceGenerator(name = "seq_tipi_tributo", sequenceName = "seq_tipi_tributo", allocationSize = 1)
public class TipoTributoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tipi_tributo")
    @Column(name = "id")
    private Long id;

    @Column(name = "cod_tributo", nullable = false, length = 255)
    private String codTributo;

    @Column(name = "descrizione", length = 255)
    private String descrizione;

    @Convert(converter = TipoContabilitaConverter.class)
    @Column(name = "tipo_contabilita", length = 1)
    private TipoContabilita tipoContabilita;

    @Column(name = "cod_contabilita", length = 255)
    private String codContabilita;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodTributo() {
        return codTributo;
    }

    public void setCodTributo(String codTributo) {
        this.codTributo = codTributo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public TipoContabilita getTipoContabilita() {
        return tipoContabilita;
    }

    public void setTipoContabilita(TipoContabilita tipoContabilita) {
        this.tipoContabilita = tipoContabilita;
    }

    public String getCodContabilita() {
        return codContabilita;
    }

    public void setCodContabilita(String codContabilita) {
        this.codContabilita = codContabilita;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TipoTributoEntity that)) {
            return false;
        }
        return getId() != null && getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId() == null ? System.identityHashCode(this) : Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "TipoTributoEntity{id=" + id + ", codTributo='" + codTributo + "'}";
    }
}
