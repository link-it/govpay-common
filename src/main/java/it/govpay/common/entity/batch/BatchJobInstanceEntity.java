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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Riga di {@code batch_job_instance}, tabella standard di Spring Batch.
 * Sola lettura: scritta esclusivamente dal batch proprietario del job
 * tramite {@code JobRepository}.
 */
@Entity
@Table(name = "batch_job_instance")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobInstanceEntity {

    @Id
    @Column(name = "job_instance_id")
    private Long id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    // job_key/version non usati dal codice applicativo, ma mappati per
    // completezza: con ddl-auto=create-drop (test) Hibernate genera lo
    // schema di questa tabella dalla entity, che deve percio' combaciare
    // con le colonne reali su cui scrivono i JdbcDao di Spring Batch.
    @Column(name = "job_key", nullable = false, length = 32)
    private String jobKey;

    @Column(name = "version")
    private Long version;
}
