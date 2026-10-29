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
package it.govpay.common.batch.metrics;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Proprieta' di configurazione delle metriche dei batch
 * (prefisso {@code govpay.metrics.batch}).
 *
 * <pre>
 * govpay:
 *   metrics:
 *     batch:
 *       enabled: true
 *       tag-nodo: true
 *       duration-buckets: [1s, 5s, 30s, 1m, 5m, 15m, 30m, 1h, 2h, 4h]
 *       istanze-ultimi-esiti: 20
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "govpay.metrics.batch")
public class BatchMetricsProperties {

    /** Abilita le metriche dei batch. Attive di default quando c'e' un {@code MeterRegistry}. */
    private boolean enabled = true;

    /**
     * Aggiunge a tutte le metriche dell'applicazione il tag comune {@code nodo},
     * valorizzato col {@code clusterId} di {@code BatchJobProperties}. Riguarda
     * anche le metriche non batch (es. HTTP).
     */
    private boolean tagNodo = true;

    /**
     * Valore del tag {@code nodo}. Va impostato solo se il progetto configura il
     * cluster-id con una property diversa da {@code govpay.batch.cluster-id}: in
     * quel caso deve avere lo stesso valore del cluster-id.
     */
    private String nodo;

    /** Bucket dell'istogramma della durata delle esecuzioni. */
    private List<Duration> durationBuckets = new ArrayList<>(List.of(
            Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(30),
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30),
            Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(4)));

    /**
     * Istanze recenti di ogni job da cui ricavare, all'avvio, l'ultimo successo e
     * l'ultimo fallimento.
     */
    private int istanzeUltimiEsiti = 20;
}
