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

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "domini")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DominioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_dominio", nullable = false, unique = true, length = 35)
    private String codDominio;

    @Column(name = "gln", length = 35)
    private String gln;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;

    @Column(name = "ragione_sociale", nullable = false, length = 70)
    private String ragioneSociale;

    @Column(name = "aux_digit", nullable = false)
    private Integer auxDigit;

    @Column(name = "iuv_prefix", length = 255)
    private String iuvPrefix;

    @Column(name = "segregation_code")
    private Integer segregationCode;

    @Column(name = "cbill", length = 255)
    private String cbill;

    @Column(name = "aut_stampa_poste", length = 255)
    private String autStampaPoste;

    @Column(name = "cod_connettore_my_pivot", length = 255)
    private String codConnettoreMyPivot;

    @Column(name = "cod_connettore_secim", length = 255)
    private String codConnettoreSecim;

    @Column(name = "cod_connettore_gov_pay", length = 255)
    private String codConnettoreGovPay;

    @Column(name = "cod_connettore_hyper_sic_apk", length = 255)
    private String codConnettoreHyperSicApk;

    @Column(name = "intermediato", nullable = false)
    private Boolean intermediato;

    @Column(name = "tassonomia_pago_pa", length = 35)
    private String tassonomiaPagoPa;

    @Column(name = "scarica_fr", nullable = false)
    private Boolean scaricaFr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_stazione")
    private StazioneEntity stazione;

    @Column(name = "id_applicazione_default")
    private Long idApplicazioneDefault;
}
