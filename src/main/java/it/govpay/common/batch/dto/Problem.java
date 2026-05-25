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
import java.net.URI;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe che rappresenta un errore REST secondo il formato RFC 7807 (Problem Details for HTTP APIs).
 * <p>
 * Esempio di risposta JSON:
 * <pre>
 * {
 *   "type": "https://example.com/problems/job-already-running",
 *   "title": "Job già in esecuzione",
 *   "status": 409,
 *   "detail": "Il job batchJob è già in esecuzione sul nodo cluster-1",
 *   "instance": "/api/batch/run"
 * }
 * </pre>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Problem implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * URI che identifica il tipo di problema.
     */
    private URI type;

    /**
     * Breve descrizione del problema (human-readable).
     */
    private String title;

    /**
     * Codice di stato HTTP.
     */
    private Integer status;

    /**
     * Descrizione dettagliata del problema specifico.
     */
    private String detail;

    /**
     * URI che identifica l'istanza specifica del problema.
     */
    private URI instance;

    /**
     * Crea un Problem per errore interno del server (HTTP 500).
     *
     * @param detail Descrizione dettagliata dell'errore
     * @return Problem configurato per errore interno
     */
    public static Problem internalServerError(String detail) {
        return Problem.builder()
                .title("Errore interno del server")
                .status(500)
                .detail(detail)
                .build();
    }

    /**
     * Crea un Problem per servizio non disponibile (HTTP 503).
     *
     * @param detail Descrizione dettagliata del problema
     * @return Problem configurato per servizio non disponibile
     */
    public static Problem serviceUnavailable(String detail) {
        return Problem.builder()
                .title("Servizio non disponibile")
                .status(503)
                .detail(detail)
                .build();
    }

    /**
     * Crea un Problem per conflitto - risorsa già esistente o in uso (HTTP 409).
     *
     * @param detail Descrizione dettagliata del conflitto
     * @return Problem configurato per conflitto
     */
    public static Problem conflict(String detail) {
        return Problem.builder()
                .title("Conflitto")
                .status(409)
                .detail(detail)
                .build();
    }

    /**
     * Crea un Problem per risorsa non trovata (HTTP 404).
     *
     * @param detail Descrizione dettagliata
     * @return Problem configurato per risorsa non trovata
     */
    public static Problem notFound(String detail) {
        return Problem.builder()
                .title("Risorsa non trovata")
                .status(404)
                .detail(detail)
                .build();
    }

    /**
     * Crea un Problem per richiesta non valida (HTTP 400).
     *
     * @param detail Descrizione dettagliata dell'errore di validazione
     * @return Problem configurato per richiesta non valida
     */
    public static Problem badRequest(String detail) {
        return Problem.builder()
                .title("Richiesta non valida")
                .status(400)
                .detail(detail)
                .build();
    }
}
