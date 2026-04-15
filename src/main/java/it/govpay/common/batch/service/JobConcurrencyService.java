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
package it.govpay.common.batch.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service per prevenire l'esecuzione concorrente di job Spring Batch in ambiente multi-nodo.
 * <p>
 * Verifica se un job è già in esecuzione utilizzando il JobExplorer di Spring Batch.
 * Questo consente il coordinamento tra nodi diversi che condividono lo stesso database
 * per i metadati di Spring Batch.
 * <p>
 * Funzionalità:
 * <ul>
 *   <li>Rilevamento job in esecuzione</li>
 *   <li>Rilevamento job "stale" (bloccati da troppo tempo)</li>
 *   <li>Rilevamento job in stati anomali (UNKNOWN, ABANDONED)</li>
 *   <li>Terminazione forzata e recupero automatico</li>
 *   <li>Supporto multi-nodo con cluster ID</li>
 * </ul>
 * <p>
 * Questa classe è pensata per essere istanziata come bean Spring nei progetti batch.
 * Richiede un {@link JobExplorer} e un {@link JobRepository} che devono essere
 * configurati nel contesto Spring.
 */
@Slf4j
@RequiredArgsConstructor
public class JobConcurrencyService {

    /** Nome del parametro job per il cluster ID */
    public static final String JOB_PARAM_CLUSTER_ID = "ClusterID";

    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
    private final int staleThresholdMinutes;

    /**
     * Controlla e restituisce l'esecuzione corrente del job, se esiste.
     * <p>
     * Utilizza il JobExplorer per interrogare le tabelle BATCH_* nel database
     * e verificare se esistono esecuzioni in corso per il job specificato.
     *
     * @param jobName Nome del job da verificare
     * @return l'esecuzione corrente del job oppure null se non ce ne sono
     */
    public JobExecution getCurrentRunningJobExecution(String jobName) {
        Set<JobExecution> runningJobs = jobExplorer.findRunningJobExecutions(jobName);

        if (!runningJobs.isEmpty()) {
            List<JobExecution> list = runningJobs.stream().toList();

            log.info("Trovati {} job '{}' in esecuzione:", list.size(), jobName);
            for (JobExecution je : list) {
                log.info("  - JobExecution ID: {}, JobInstance: {}, Status: {}, Start: {}",
                    je.getId(), je.getJobInstance().getJobName(), je.getStatus(), je.getStartTime());
            }

            return list.get(0);
        }

        return null;
    }

    /**
     * Verifica se un'esecuzione è "stale" (bloccata o in stato anomalo).
     * <p>
     * Un'esecuzione è considerata stale se:
     * <ul>
     *   <li>È in uno stato anomalo (UNKNOWN, ABANDONED)</li>
     *   <li>Non viene aggiornata da più di staleThresholdMinutes minuti</li>
     * </ul>
     *
     * @param jobExecution l'esecuzione del job da verificare
     * @return true se il job è stale, false altrimenti
     */
    public boolean isJobExecutionStale(JobExecution jobExecution) {
        if (jobExecution == null) {
            return false;
        }

        BatchStatus status = jobExecution.getStatus();

        // Verifica stati anomali
        if (status == BatchStatus.UNKNOWN || status == BatchStatus.ABANDONED) {
            log.warn("JobExecution {} è in stato anomalo: {}", jobExecution.getId(), status);
            return true;
        }

        // Verifica se il job non viene aggiornato da troppo tempo
        if (status == BatchStatus.STARTED) {
            LocalDateTime lastUpdated = jobExecution.getLastUpdated();
            if (lastUpdated != null) {
                LocalDateTime now = LocalDateTime.now();
                Duration duration = Duration.between(lastUpdated, now);
                long minutesSinceLastUpdate = duration.toMinutes();

                if (minutesSinceLastUpdate > staleThresholdMinutes) {
                    log.warn("JobExecution {} non aggiornata da {} minuti (soglia: {} minuti). Considerata stale.",
                        jobExecution.getId(), minutesSinceLastUpdate, staleThresholdMinutes);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Abbandona forzatamente un'esecuzione stale, permettendo il riavvio del job.
     * <p>
     * Aggiorna lo stato dell'esecuzione a FAILED nel database Spring Batch,
     * liberando il lock e permettendo a un nuovo nodo di avviare il job.
     * <p>
     * <strong>IMPORTANTE:</strong> Questa operazione non termina il processo Java che sta
     * eseguendo il job bloccato, ma solo aggiorna i metadati nel database.
     * Il processo bloccato potrebbe continuare a girare fino al suo timeout.
     *
     * @param jobExecution L'esecuzione da abbandonare
     * @return true se l'abbandono è riuscito, false altrimenti
     */
    public boolean abandonStaleJobExecution(JobExecution jobExecution) {
        if (jobExecution == null) {
            return false;
        }

        try {
            log.warn("Abbandono forzato JobExecution {} (Status: {}, lastUpdated: {})",
                jobExecution.getId(), jobExecution.getStatus(), jobExecution.getLastUpdated());

            // Aggiorna lo stato a FAILED e imposta end time
            jobExecution.setStatus(BatchStatus.FAILED);
            jobExecution.setEndTime(LocalDateTime.now());
            jobExecution.setExitStatus(ExitStatus.FAILED
                .addExitDescription("Job abbandonato automaticamente: non aggiornato da oltre "
                    + staleThresholdMinutes + " minuti o stato anomalo"));

            // Aggiorna anche tutti gli step in esecuzione
            jobExecution.getStepExecutions().forEach(stepExecution -> {
                if (stepExecution.getStatus() == BatchStatus.STARTED) {
                    log.info("Abbandono StepExecution: {} (stato: {})",
                        stepExecution.getStepName(), stepExecution.getStatus());
                    stepExecution.setStatus(BatchStatus.FAILED);
                    stepExecution.setEndTime(LocalDateTime.now());
                    stepExecution.setExitStatus(ExitStatus.FAILED
                        .addExitDescription("Step abbandonato: job stale"));
                    jobRepository.update(stepExecution);
                }
            });

            // Aggiorna il job execution nel repository
            jobRepository.update(jobExecution);

            log.info("JobExecution {} abbandonata con successo", jobExecution.getId());
            return true;
        } catch (Exception e) {
            log.error("Errore nell'abbandono di JobExecution {}: {}",
                jobExecution.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Forza l'abbandono di un'esecuzione in corso, indipendentemente dal suo stato.
     * <p>
     * A differenza di {@link #abandonStaleJobExecution(JobExecution)}, questo metodo
     * non verifica se l'esecuzione è stale, ma la termina forzatamente.
     * <p>
     * <strong>ATTENZIONE:</strong> Usare con cautela. Questa operazione non termina il processo Java
     * che sta eseguendo il job, ma solo aggiorna i metadati nel database.
     *
     * @param jobExecution L'esecuzione da terminare forzatamente
     * @param reason Motivo della terminazione forzata
     * @return true se la terminazione è riuscita, false altrimenti
     */
    public boolean forceAbandonJobExecution(JobExecution jobExecution, String reason) {
        if (jobExecution == null) {
            return false;
        }

        try {
            log.warn("Terminazione forzata JobExecution {} (Status: {}, Motivo: {})",
                jobExecution.getId(), jobExecution.getStatus(), reason);

            // Aggiorna lo stato a ABANDONED e imposta end time
            jobExecution.setStatus(BatchStatus.ABANDONED);
            jobExecution.setEndTime(LocalDateTime.now());
            jobExecution.setExitStatus(ExitStatus.STOPPED
                .addExitDescription("Job terminato forzatamente: " + reason));

            // Aggiorna anche tutti gli step in esecuzione
            jobExecution.getStepExecutions().forEach(stepExecution -> {
                if (stepExecution.getStatus() == BatchStatus.STARTED) {
                    log.info("Terminazione forzata StepExecution: {} (stato: {})",
                        stepExecution.getStepName(), stepExecution.getStatus());
                    stepExecution.setStatus(BatchStatus.ABANDONED);
                    stepExecution.setEndTime(LocalDateTime.now());
                    stepExecution.setExitStatus(ExitStatus.STOPPED
                        .addExitDescription("Step terminato forzatamente"));
                    jobRepository.update(stepExecution);
                }
            });

            // Aggiorna il job execution nel repository
            jobRepository.update(jobExecution);

            log.info("JobExecution {} terminata forzatamente con successo", jobExecution.getId());
            return true;
        } catch (Exception e) {
            log.error("Errore nella terminazione forzata di JobExecution {}: {}",
                jobExecution.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Estrae il cluster ID da un'esecuzione di job.
     * <p>
     * Il cluster ID identifica il nodo che ha avviato l'esecuzione.
     *
     * @param jobExecution L'esecuzione del job
     * @return il cluster ID oppure null se non presente o se jobExecution è null
     */
    public String getClusterIdFromExecution(JobExecution jobExecution) {
        return getClusterIdFromExecution(jobExecution, JOB_PARAM_CLUSTER_ID);
    }

    /**
     * Estrae il cluster ID da un'esecuzione di job usando un nome parametro personalizzato.
     *
     * @param jobExecution L'esecuzione del job
     * @param clusterIdParamName Nome del parametro che contiene il cluster ID
     * @return il cluster ID oppure null se non presente o se jobExecution è null
     */
    public String getClusterIdFromExecution(JobExecution jobExecution, String clusterIdParamName) {
        if (jobExecution == null) {
            return null;
        }

        var params = jobExecution.getJobParameters();
        String value = params.getString(clusterIdParamName);
        if (value != null) {
            return value;
        }

        return null;
    }

    /**
     * Restituisce il JobExplorer utilizzato.
     *
     * @return il JobExplorer
     */
    public JobExplorer getJobExplorer() {
        return jobExplorer;
    }
}
