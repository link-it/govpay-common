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

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener che alimenta {@link BatchMetricsRecorder} con l'inizio e la fine di
 * ogni esecuzione.
 * <p>
 * E' registrato automaticamente su ogni job dall'auto-configurazione delle
 * metriche batch. Un errore nella registrazione delle metriche e' riportato a
 * log e non fa mai fallire il job.
 */
@Slf4j
public class BatchMetricsJobListener implements JobExecutionListener {

    private final BatchMetricsRecorder recorder;

    public BatchMetricsJobListener(BatchMetricsRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        try {
            recorder.inizioJob(jobExecution);
        } catch (RuntimeException e) {
            log.warn("Errore nella registrazione delle metriche all'avvio di JobExecution {}: {}",
                    jobExecution.getId(), e.getMessage(), e);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            recorder.fineJob(jobExecution);
        } catch (RuntimeException e) {
            log.warn("Errore nella registrazione delle metriche al termine di JobExecution {}: {}",
                    jobExecution.getId(), e.getMessage(), e);
        }
    }
}
