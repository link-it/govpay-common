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
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Riga sintetica dello storico esecuzioni di un batch (endpoint
 * {@code GET /executions}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ExecutionSummaryInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long executionId;

    /**
     * Stato nativo Spring Batch (STARTING, STARTED, COMPLETED, FAILED,
     * STOPPING, STOPPED, ABANDONED, UNKNOWN).
     */
    private String status;

    /**
     * Data/ora di inizio, o data/ora di creazione se l'esecuzione non e'
     * ancora partita (STARTING).
     */
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /**
     * Provenienza dell'avvio ({@code MANUAL}/{@code SCHEDULED}). Null per le
     * esecuzioni avviate prima dell'introduzione di questo JobParameter.
     */
    private String triggerType;
}
