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

import java.io.Serializable;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Riga di {@code batch_job_execution_params}, tabella standard di Spring
 * Batch (chiave naturale {@code job_execution_id + parameter_name}, nessuna
 * colonna id). Sola lettura: scritta esclusivamente dal batch proprietario
 * del job tramite {@code JobRepository}.
 */
@Entity
@Table(name = "batch_job_execution_params")
@IdClass(BatchJobExecutionParamEntity.Pk.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobExecutionParamEntity {

    @Id
    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Id
    @Column(name = "parameter_name", length = 100)
    private String parameterName;

    @Column(name = "parameter_type", nullable = false, length = 100)
    private String parameterType;

    @Column(name = "parameter_value", length = 2500)
    private String parameterValue;

    // Colonna CHAR(1) nello schema standard Spring Batch: senza questo il
    // validator Hibernate si aspetta varchar(1) e l'avvio fallisce con
    // ddl-auto=validate sui DB creati dal DDL del batch.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "identifying", nullable = false, length = 1)
    private String identifying;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long jobExecutionId;
        private String parameterName;
    }
}
