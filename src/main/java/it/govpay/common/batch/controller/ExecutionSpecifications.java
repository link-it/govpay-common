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
package it.govpay.common.batch.controller;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;

import it.govpay.common.entity.batch.BatchJobExecutionEntity;

/**
 * Filtri Criteria per lo storico esecuzioni ({@code GET /executions}).
 * Espone lo stato Spring Batch nativo (STARTING, STARTED, COMPLETED, ...):
 * un'eventuale mappatura verso una semantica applicativa e' responsabilita'
 * del consumer.
 */
final class ExecutionSpecifications {

    private ExecutionSpecifications() {
    }

    static Specification<BatchJobExecutionEntity> jobNameEquals(String jobName) {
        return (root, q, cb) -> cb.equal(root.get("jobInstance").get("jobName"), jobName);
    }

    static Specification<BatchJobExecutionEntity> statoIn(Set<String> stati) {
        if (stati == null || stati.isEmpty()) {
            return null;
        }
        return (root, q, cb) -> root.get("status").in(stati);
    }

    /** {@code value} deve gia' essere convertito nel timezone applicativo. */
    static Specification<BatchJobExecutionEntity> dataInizioMin(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return (root, q, cb) -> cb.greaterThanOrEqualTo(
                cb.coalesce(root.get("startTime"), root.get("createTime")), value);
    }

    /** {@code value} deve gia' essere convertito nel timezone applicativo. */
    static Specification<BatchJobExecutionEntity> dataInizioMax(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return (root, q, cb) -> cb.lessThanOrEqualTo(
                cb.coalesce(root.get("startTime"), root.get("createTime")), value);
    }
}
