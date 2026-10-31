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
package it.govpay.common.batch;

/**
 * Osservatore dei tentativi di avvio di un job (tick), notificato da
 * {@code JobExecutionHelper} per ogni controllo pre-esecuzione e da
 * {@code AbstractBatchController} per ogni richiesta di avvio manuale.
 * <p>
 * Serve a rendere aggregabili situazioni che altrimenti resterebbero solo nei
 * log: avvii saltati perche' il job e' gia' in esecuzione, job stale
 * abbandonati, avvii forzati.
 */
@FunctionalInterface
public interface JobTickObserver {

    /** Osservatore che non fa nulla: default quando le metriche non sono attive. */
    JobTickObserver NESSUNO = (jobName, trigger, esito) -> { };

    /**
     * Notifica l'esito di un tentativo di avvio.
     *
     * @param jobName Nome del job
     * @param trigger Provenienza del tentativo
     * @param esito Esito del controllo pre-esecuzione
     */
    void onTick(String jobName, TriggerType trigger, EsitoTick esito);
}
