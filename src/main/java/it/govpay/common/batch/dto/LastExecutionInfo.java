/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2025 Link.it srl (http://www.link.it).
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
 * DTO per le informazioni sull'ultima esecuzione completata di un batch.
 * <p>
 * Fornisce dettagli su:
 * <ul>
 *   <li>Quando è stata eseguita</li>
 *   <li>Quanto è durata</li>
 *   <li>Come è terminata (stato, exit code)</li>
 *   <li>Su quale nodo è stata eseguita</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class LastExecutionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID dell'ultima esecuzione.
     */
    private Long executionId;

    /**
     * Cluster ID del nodo che ha eseguito il batch.
     */
    private String clusterId;

    /**
     * Data/ora di inizio dell'ultima esecuzione.
     */
    private LocalDateTime startTime;

    /**
     * Data/ora di fine dell'ultima esecuzione.
     */
    private LocalDateTime endTime;

    /**
     * Durata in secondi dell'ultima esecuzione.
     */
    private Long durationSeconds;

    /**
     * Stato finale dell'esecuzione (COMPLETED, FAILED, STOPPED, etc.).
     */
    private String status;

    /**
     * Exit code dell'esecuzione.
     */
    private String exitCode;

    /**
     * Descrizione dell'exit status (può essere troncata per messaggi lunghi).
     */
    private String exitDescription;
}
