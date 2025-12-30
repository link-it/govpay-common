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
package it.govpay.common.batch.config;

import java.time.ZoneId;

import lombok.Getter;
import lombok.Setter;

/**
 * Classe base per le properties di configurazione dei batch Spring Batch.
 * <p>
 * I progetti batch devono estendere questa classe e annotarla con
 * {@code @ConfigurationProperties} per caricare le configurazioni specifiche.
 * <p>
 * Esempio:
 * <pre>
 * &#64;Configuration
 * &#64;ConfigurationProperties(prefix = "govpay.batch")
 * public class MyBatchProperties extends BatchJobProperties {
 * }
 * </pre>
 */
@Getter
@Setter
public class BatchJobProperties {

    /**
     * Identificativo del cluster/nodo che esegue il batch.
     * Usato per coordinare esecuzioni multiple in ambiente distribuito.
     * Default: "GovPay-Batch"
     */
    private String clusterId = "GovPay-Batch";

    /**
     * Soglia in minuti oltre la quale un job in esecuzione viene considerato "stale" (bloccato).
     * Un job stale può essere automaticamente abbandonato e riavviato.
     * Default: 120 minuti (2 ore)
     */
    private int staleThresholdMinutes = 120;

    /**
     * Timezone dell'applicazione per calcoli temporali.
     * Default: Europe/Rome
     */
    private String timeZone = "Europe/Rome";

    /**
     * Intervallo di scheduling in millisecondi (per modalità scheduler interno).
     * Default: 600000 (10 minuti)
     */
    private long schedulerIntervalMillis = 600000L;

    /**
     * Ritardo iniziale in millisecondi prima della prima esecuzione schedulata.
     * Default: 1 (praticamente immediato)
     */
    private long initialDelayMillis = 1L;

    /**
     * Restituisce il ZoneId configurato.
     *
     * @return ZoneId corrispondente alla timezone configurata
     */
    public ZoneId getZoneId() {
        return ZoneId.of(timeZone);
    }
}
