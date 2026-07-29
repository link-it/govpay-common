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
package it.govpay.common.batch.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO descrittivo del batch, definito autonomamente da ciascun batch
 * (nome tecnico del job, nome e descrizione pensati per un consumer
 * esterno che non conosce il job Spring Batch sottostante).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class BatchInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Nome tecnico del job Spring Batch.
     */
    private String jobName;

    /**
     * Nome human-readable del batch, definito dal batch stesso.
     */
    private String displayName;

    /**
     * Descrizione human-readable del batch, definita dal batch stesso.
     */
    private String description;
}
