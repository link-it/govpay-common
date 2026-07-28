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
package it.govpay.common.entity.batch;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Riga di {@code batch_job_execution}, tabella standard di Spring Batch.
 * Sola lettura: scritta esclusivamente dal batch proprietario del job
 * tramite {@code JobRepository}.
 */
@Entity
@Table(name = "batch_job_execution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobExecutionEntity {

    @Id
    @Column(name = "job_execution_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_instance_id", nullable = false)
    private BatchJobInstanceEntity jobInstance;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "exit_message", length = 2500)
    private String exitMessage;

    // version/exit_code/last_updated non usati dal codice applicativo, ma
    // mappati per completezza: con ddl-auto=create-drop (test) Hibernate
    // genera lo schema di questa tabella dalla entity, che deve percio'
    // combaciare con le colonne reali su cui scrivono i JdbcDao di Spring
    // Batch (INSERT su tutte le colonne).
    @Column(name = "version")
    private Long version;

    @Column(name = "exit_code", length = 2500)
    private String exitCode;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
