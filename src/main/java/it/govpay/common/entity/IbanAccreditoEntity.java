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
 * Conto di accredito (IBAN) di un dominio (tabella {@code iban_accredito}). Chiave
 * naturale {@code (cod_iban, id_dominio)} — issue
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>.
 */
@Entity
@Table(name = "iban_accredito")
@SequenceGenerator(name = "seq_iban_accredito", sequenceName = "seq_iban_accredito", allocationSize = 1)
public class IbanAccreditoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_iban_accredito")
    @Column(name = "id")
    private Long id;

    @Column(name = "cod_iban", nullable = false, length = 255)
    private String codIban;

    @Column(name = "bic_accredito", length = 255)
    private String bicAccredito;

    @Column(name = "postale", nullable = false)
    private Boolean postale;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;

    @Column(name = "descrizione", length = 255)
    private String descrizione;

    @Column(name = "intestatario", length = 255)
    private String intestatario;

    @Column(name = "aut_stampa_poste", length = 255)
    private String autStampaPoste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dominio", nullable = false)
    private DominioEntity dominio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodIban() {
        return codIban;
    }

    public void setCodIban(String codIban) {
        this.codIban = codIban;
    }

    public String getBicAccredito() {
        return bicAccredito;
    }

    public void setBicAccredito(String bicAccredito) {
        this.bicAccredito = bicAccredito;
    }

    public Boolean getPostale() {
        return postale;
    }

    public void setPostale(Boolean postale) {
        this.postale = postale;
    }

    public Boolean getAbilitato() {
        return abilitato;
    }

    public void setAbilitato(Boolean abilitato) {
        this.abilitato = abilitato;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getIntestatario() {
        return intestatario;
    }

    public void setIntestatario(String intestatario) {
        this.intestatario = intestatario;
    }

    public String getAutStampaPoste() {
        return autStampaPoste;
    }

    public void setAutStampaPoste(String autStampaPoste) {
        this.autStampaPoste = autStampaPoste;
    }

    public DominioEntity getDominio() {
        return dominio;
    }

    public void setDominio(DominioEntity dominio) {
        this.dominio = dominio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IbanAccreditoEntity that)) {
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
        return "IbanAccreditoEntity{id=" + id + ", codIban='" + codIban + "'}";
    }
}
