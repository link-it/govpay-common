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
@Table(name = "intermediari")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntermediarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_intermediario", nullable = false, unique = true, length = 255)
    private String codIntermediario;

    @Column(name = "cod_connettore_pdd", length = 255)
    private String codConnettorePdd;

    @Column(name = "cod_connettore_recupero_rt", length = 255)
    private String codConnettoreRecuperoRt;

    @Column(name = "cod_connettore_aca", length = 255)
    private String codConnettoreAca;

    @Column(name = "cod_connettore_gpd", length = 255)
    private String codConnettoreGpd;

    @Column(name = "cod_connettore_fr", length = 255)
    private String codConnettoreFr;

    @Column(name = "cod_connettore_backoffice_ec", length = 255)
    private String codConnettoreBackofficeEc;

    @Column(name = "cod_connettore_ftp", length = 255)
    private String codConnettoreFtp;

    @Column(name = "denominazione", length = 255)
    private String denominazione;

    @Column(name = "principal", length = 255)
    private String principal;

    @Column(name = "principal_originale", length = 255)
    private String principalOriginale;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;
}
