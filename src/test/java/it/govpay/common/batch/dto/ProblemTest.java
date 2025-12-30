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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProblemTest {

    @Test
    @DisplayName("internalServerError - crea Problem 500")
    void internalServerError() {
        Problem problem = Problem.internalServerError("Test error");

        assertEquals("Errore interno del server", problem.getTitle());
        assertEquals(500, problem.getStatus());
        assertEquals("Test error", problem.getDetail());
        assertNull(problem.getType());
        assertNull(problem.getInstance());
    }

    @Test
    @DisplayName("serviceUnavailable - crea Problem 503")
    void serviceUnavailable() {
        Problem problem = Problem.serviceUnavailable("Service down");

        assertEquals("Servizio non disponibile", problem.getTitle());
        assertEquals(503, problem.getStatus());
        assertEquals("Service down", problem.getDetail());
    }

    @Test
    @DisplayName("conflict - crea Problem 409")
    void conflict() {
        Problem problem = Problem.conflict("Resource busy");

        assertEquals("Conflitto", problem.getTitle());
        assertEquals(409, problem.getStatus());
        assertEquals("Resource busy", problem.getDetail());
    }

    @Test
    @DisplayName("notFound - crea Problem 404")
    void notFound() {
        Problem problem = Problem.notFound("Resource not found");

        assertEquals("Risorsa non trovata", problem.getTitle());
        assertEquals(404, problem.getStatus());
        assertEquals("Resource not found", problem.getDetail());
    }

    @Test
    @DisplayName("badRequest - crea Problem 400")
    void badRequest() {
        Problem problem = Problem.badRequest("Invalid input");

        assertEquals("Richiesta non valida", problem.getTitle());
        assertEquals(400, problem.getStatus());
        assertEquals("Invalid input", problem.getDetail());
    }

    @Test
    @DisplayName("builder - costruisce Problem completo")
    void builder() {
        Problem problem = Problem.builder()
                .title("Custom Title")
                .status(422)
                .detail("Custom detail")
                .build();

        assertEquals("Custom Title", problem.getTitle());
        assertEquals(422, problem.getStatus());
        assertEquals("Custom detail", problem.getDetail());
    }
}
