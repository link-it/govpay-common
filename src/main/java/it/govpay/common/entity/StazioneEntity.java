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
@Table(name = "stazioni")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StazioneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_stazione", nullable = false, unique = true, length = 255)
    private String codStazione;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;

    @Column(name = "application_code")
    private Integer applicationCode;

    @Column(name = "versione", length = 255)
    private String versione;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_intermediario", nullable = false)
    private IntermediarioEntity intermediario;
}
