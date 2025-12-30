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
 * DTO per le informazioni sullo stato corrente di un batch.
 * <p>
 * Utilizzato dagli endpoint REST per comunicare lo stato di esecuzione:
 * <ul>
 *   <li>Se il batch è in esecuzione</li>
 *   <li>Da quanto tempo è in esecuzione</li>
 *   <li>Quale step sta eseguendo</li>
 *   <li>Su quale nodo del cluster sta girando</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class BatchStatusInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Indica se il batch è attualmente in esecuzione.
     */
    private boolean running;

    /**
     * ID dell'esecuzione corrente (se in esecuzione).
     */
    private Long executionId;

    /**
     * Cluster ID del nodo che sta eseguendo il batch.
     */
    private String clusterId;

    /**
     * Data/ora di inizio dell'esecuzione corrente.
     */
    private LocalDateTime startTime;

    /**
     * Durata in secondi dell'esecuzione corrente.
     */
    private Long runningSeconds;

    /**
     * Stato corrente dell'esecuzione (STARTED, STARTING, etc.).
     */
    private String status;

    /**
     * Nome dello step attualmente in esecuzione.
     */
    private String currentStep;
}
