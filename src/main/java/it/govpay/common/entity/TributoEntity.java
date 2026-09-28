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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Entrata (tributo) configurata per un dominio (tabella {@code tributi}): associa il
 * dominio al {@link TipoTributoEntity} globale, con eventuale override di contabilità e i
 * conti di accredito/appoggio. Chiave naturale {@code (id_dominio, id_tipo_tributo)} —
 * issue <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>.
 */
@Entity
@Table(name = "tributi")
@SequenceGenerator(name = "seq_tributi", sequenceName = "seq_tributi", allocationSize = 1)
public class TributoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_tributi")
    @Column(name = "id")
    private Long id;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;

    @Convert(converter = TipoContabilitaConverter.class)
    @Column(name = "tipo_contabilita", length = 1)
    private TipoContabilita tipoContabilita;

    @Column(name = "codice_contabilita", length = 255)
    private String codiceContabilita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dominio", nullable = false)
    private DominioEntity dominio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_tributo", nullable = false)
    private TipoTributoEntity tipoTributo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_iban_accredito")
    private IbanAccreditoEntity ibanAccredito;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_iban_appoggio")
    private IbanAccreditoEntity ibanAppoggio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getAbilitato() {
        return abilitato;
    }

    public void setAbilitato(Boolean abilitato) {
        this.abilitato = abilitato;
    }

    public TipoContabilita getTipoContabilita() {
        return tipoContabilita;
    }

    public void setTipoContabilita(TipoContabilita tipoContabilita) {
        this.tipoContabilita = tipoContabilita;
    }

    public String getCodiceContabilita() {
        return codiceContabilita;
    }

    public void setCodiceContabilita(String codiceContabilita) {
        this.codiceContabilita = codiceContabilita;
    }

    public DominioEntity getDominio() {
        return dominio;
    }

    public void setDominio(DominioEntity dominio) {
        this.dominio = dominio;
    }

    public TipoTributoEntity getTipoTributo() {
        return tipoTributo;
    }

    public void setTipoTributo(TipoTributoEntity tipoTributo) {
        this.tipoTributo = tipoTributo;
    }

    public IbanAccreditoEntity getIbanAccredito() {
        return ibanAccredito;
    }

    public void setIbanAccredito(IbanAccreditoEntity ibanAccredito) {
        this.ibanAccredito = ibanAccredito;
    }

    public IbanAccreditoEntity getIbanAppoggio() {
        return ibanAppoggio;
    }

    public void setIbanAppoggio(IbanAccreditoEntity ibanAppoggio) {
        this.ibanAppoggio = ibanAppoggio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TributoEntity that)) {
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
        return "TributoEntity{id=" + id + "}";
    }
}
