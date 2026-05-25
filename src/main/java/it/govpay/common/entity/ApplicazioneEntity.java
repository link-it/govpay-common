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
@Table(name = "applicazioni")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicazioneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_applicazione", nullable = false, unique = true, length = 35)
    private String codApplicazione;

    @Column(name = "auto_iuv", nullable = false)
    private Boolean autoIuv;

    @Column(name = "firma_ricevuta", nullable = false, length = 1)
    private String firmaRicevuta;

    @Column(name = "trusted", nullable = false)
    private Boolean trusted;

    @Column(name = "cod_connettore_integrazione", length = 255)
    private String codConnettoreIntegrazione;

    @Column(name = "cod_applicazione_iuv", length = 3)
    private String codApplicazioneIuv;

    @Column(name = "reg_exp", length = 1024)
    private String regExp;
}
