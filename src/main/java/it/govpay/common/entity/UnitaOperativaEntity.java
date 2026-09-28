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
 * Unità operativa di un dominio (tabella {@code uo}). Chiave naturale
 * {@code (cod_uo, id_dominio)} — issue
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>.
 */
@Entity
@Table(name = "uo")
@SequenceGenerator(name = "seq_uo", sequenceName = "seq_uo", allocationSize = 1)
public class UnitaOperativaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_uo")
    @Column(name = "id")
    private Long id;

    @Column(name = "cod_uo", nullable = false, length = 35)
    private String codUo;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;

    @Column(name = "uo_codice_identificativo", length = 35)
    private String uoCodiceIdentificativo;

    @Column(name = "uo_denominazione", length = 70)
    private String uoDenominazione;

    @Column(name = "uo_indirizzo", length = 70)
    private String uoIndirizzo;

    @Column(name = "uo_civico", length = 16)
    private String uoCivico;

    @Column(name = "uo_cap", length = 16)
    private String uoCap;

    @Column(name = "uo_localita", length = 35)
    private String uoLocalita;

    @Column(name = "uo_provincia", length = 35)
    private String uoProvincia;

    @Column(name = "uo_nazione", length = 2)
    private String uoNazione;

    @Column(name = "uo_area", length = 255)
    private String uoArea;

    @Column(name = "uo_url_sito_web", length = 255)
    private String uoUrlSitoWeb;

    @Column(name = "uo_email", length = 255)
    private String uoEmail;

    @Column(name = "uo_pec", length = 255)
    private String uoPec;

    @Column(name = "uo_tel", length = 255)
    private String uoTel;

    @Column(name = "uo_fax", length = 255)
    private String uoFax;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dominio", nullable = false)
    private DominioEntity dominio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodUo() {
        return codUo;
    }

    public void setCodUo(String codUo) {
        this.codUo = codUo;
    }

    public Boolean getAbilitato() {
        return abilitato;
    }

    public void setAbilitato(Boolean abilitato) {
        this.abilitato = abilitato;
    }

    public String getUoCodiceIdentificativo() {
        return uoCodiceIdentificativo;
    }

    public void setUoCodiceIdentificativo(String uoCodiceIdentificativo) {
        this.uoCodiceIdentificativo = uoCodiceIdentificativo;
    }

    public String getUoDenominazione() {
        return uoDenominazione;
    }

    public void setUoDenominazione(String uoDenominazione) {
        this.uoDenominazione = uoDenominazione;
    }

    public String getUoIndirizzo() {
        return uoIndirizzo;
    }

    public void setUoIndirizzo(String uoIndirizzo) {
        this.uoIndirizzo = uoIndirizzo;
    }

    public String getUoCivico() {
        return uoCivico;
    }

    public void setUoCivico(String uoCivico) {
        this.uoCivico = uoCivico;
    }

    public String getUoCap() {
        return uoCap;
    }

    public void setUoCap(String uoCap) {
        this.uoCap = uoCap;
    }

    public String getUoLocalita() {
        return uoLocalita;
    }

    public void setUoLocalita(String uoLocalita) {
        this.uoLocalita = uoLocalita;
    }

    public String getUoProvincia() {
        return uoProvincia;
    }

    public void setUoProvincia(String uoProvincia) {
        this.uoProvincia = uoProvincia;
    }

    public String getUoNazione() {
        return uoNazione;
    }

    public void setUoNazione(String uoNazione) {
        this.uoNazione = uoNazione;
    }

    public String getUoArea() {
        return uoArea;
    }

    public void setUoArea(String uoArea) {
        this.uoArea = uoArea;
    }

    public String getUoUrlSitoWeb() {
        return uoUrlSitoWeb;
    }

    public void setUoUrlSitoWeb(String uoUrlSitoWeb) {
        this.uoUrlSitoWeb = uoUrlSitoWeb;
    }

    public String getUoEmail() {
        return uoEmail;
    }

    public void setUoEmail(String uoEmail) {
        this.uoEmail = uoEmail;
    }

    public String getUoPec() {
        return uoPec;
    }

    public void setUoPec(String uoPec) {
        this.uoPec = uoPec;
    }

    public String getUoTel() {
        return uoTel;
    }

    public void setUoTel(String uoTel) {
        this.uoTel = uoTel;
    }

    public String getUoFax() {
        return uoFax;
    }

    public void setUoFax(String uoFax) {
        this.uoFax = uoFax;
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
        if (!(o instanceof UnitaOperativaEntity that)) {
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
        return "UnitaOperativaEntity{id=" + id + ", codUo='" + codUo + "'}";
    }
}
