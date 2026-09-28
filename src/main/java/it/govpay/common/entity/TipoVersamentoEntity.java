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
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Tipologia di versamento globale (catalogo, tabella {@code tipi_versamento}). Chiave
 * naturale {@code cod_tipo_versamento} — issue
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>.
 */
@Entity
@Table(name = "tipi_versamento")
@SequenceGenerator(name = "seq_tipi_versamento", sequenceName = "seq_tipi_versamento", allocationSize = 1)
public class TipoVersamentoEntity extends AbstractTipoVersamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tipi_versamento")
    @Column(name = "id")
    private Long id;

    @Column(name = "cod_tipo_versamento", nullable = false, length = 35)
    private String codTipoVersamento;

    @Column(name = "descrizione", nullable = false, length = 255)
    private String descrizione;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodTipoVersamento() {
        return codTipoVersamento;
    }

    public void setCodTipoVersamento(String codTipoVersamento) {
        this.codTipoVersamento = codTipoVersamento;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TipoVersamentoEntity that)) {
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
        return "TipoVersamentoEntity{id=" + id + ", codTipoVersamento='" + codTipoVersamento + "'}";
    }
}
