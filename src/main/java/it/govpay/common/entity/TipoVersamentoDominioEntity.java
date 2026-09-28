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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Configurazione per-dominio di un {@link TipoVersamentoEntity} (tabella
 * {@code tipi_vers_domini}): porta gli stessi campi di configurazione del globale
 * (ereditati da {@link AbstractTipoVersamento}) come override per il dominio, più la
 * {@code app_io_api_key} specifica e i riferimenti a dominio e tipo versamento globale.
 * Chiave naturale {@code (id_dominio, id_tipo_versamento)} — issue
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>.
 */
@Entity
@Table(name = "tipi_vers_domini")
@SequenceGenerator(name = "seq_tipi_vers_domini", sequenceName = "seq_tipi_vers_domini", allocationSize = 1)
public class TipoVersamentoDominioEntity extends AbstractTipoVersamento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tipi_vers_domini")
    @Column(name = "id")
    private Long id;

    @Column(name = "app_io_api_key", length = 255)
    private String appIoApiKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dominio", nullable = false)
    private DominioEntity dominio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_versamento", nullable = false)
    private TipoVersamentoEntity tipoVersamento;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAppIoApiKey() {
        return appIoApiKey;
    }

    public void setAppIoApiKey(String appIoApiKey) {
        this.appIoApiKey = appIoApiKey;
    }

    public DominioEntity getDominio() {
        return dominio;
    }

    public void setDominio(DominioEntity dominio) {
        this.dominio = dominio;
    }

    public TipoVersamentoEntity getTipoVersamento() {
        return tipoVersamento;
    }

    public void setTipoVersamento(TipoVersamentoEntity tipoVersamento) {
        this.tipoVersamento = tipoVersamento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TipoVersamentoDominioEntity that)) {
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
        return "TipoVersamentoDominioEntity{id=" + id + "}";
    }
}
