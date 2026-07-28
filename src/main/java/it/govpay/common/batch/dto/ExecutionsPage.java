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
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagina di risultati dello storico esecuzioni ({@code GET /executions}).
 * <p>
 * {@code totalResults}/{@code totalPages} sono valorizzati solo se il
 * chiamante ha richiesto il conteggio totale (parametro {@code total=true});
 * altrimenti {@code hasNextPage} viene comunque calcolato senza un
 * {@code COUNT(*)} aggiuntivo (richiedendo una riga in piu' della pagina).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class ExecutionsPage implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<ExecutionSummaryInfo> results;

    private int page;

    private int limit;

    private boolean hasNextPage;

    private Long totalResults;

    private Integer totalPages;
}
