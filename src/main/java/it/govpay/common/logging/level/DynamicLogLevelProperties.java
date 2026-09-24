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

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Proprieta' di configurazione dei livelli di log dinamici
 * (prefisso {@code govpay.logging.livelli-dinamici}).
 *
 * <pre>
 * govpay:
 *   logging:
 *     livelli-dinamici:
 *       enabled: true
 *       intervallo-refresh: 60s        # 0 disattiva il polling
 *       logger-gestiti:
 *         - it.govpay
 * </pre>
 *
 * <p>{@code logger-gestiti} attua il vincolo di ambito di BP-LOG-1: sono
 * modificabili a runtime solo i logger sotto i prefissi elencati, non l'intera
 * applicazione. Un tentativo di modifica fuori ambito viene rifiutato.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "govpay.logging.livelli-dinamici")
public class DynamicLogLevelProperties {

    /** Abilita la gestione dinamica dei livelli di log. */
    private boolean enabled = true;

    /**
     * Intervallo di rilettura della configurazione da database. E' il ritardo
     * massimo con cui una modifica fatta su un nodo raggiunge gli altri nodi del
     * cluster. {@code 0} (o negativo) disattiva il polling: la configurazione
     * viene allora applicata solo all'avvio e sulle invocazioni esplicite di
     * refresh.
     */
    private Duration intervalloRefresh = Duration.ofSeconds(60);

    /**
     * Ritardo della prima rilettura successiva a quella di avvio.
     */
    private Duration ritardoIniziale = Duration.ofSeconds(60);

    /**
     * Prefissi dei logger gestibili dinamicamente. Un nome di logger e' in ambito
     * se coincide con un prefisso o se ne e' un discendente.
     */
    private List<String> loggerGestiti = List.of("it.govpay");
}
