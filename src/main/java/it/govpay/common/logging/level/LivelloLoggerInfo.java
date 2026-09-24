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
package it.govpay.common.logging.level;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Stato di un logger gestito dinamicamente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
public class LivelloLoggerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Nome del logger (tipicamente un package). */
    private String logger;

    /** Livello esplicitamente configurato sul logger, {@code null} se ereditato. */
    private String livelloConfigurato;

    /** Livello effettivo del logger, tenuto conto dell'ereditarieta'. */
    private String livelloEffettivo;

    /** Livello con cui il logger risultava configurato all'avvio dell'applicazione. */
    private String livelloIniziale;

    /** {@code true} se il livello corrente proviene dalla configurazione dinamica. */
    private boolean gestitoDinamicamente;
}
