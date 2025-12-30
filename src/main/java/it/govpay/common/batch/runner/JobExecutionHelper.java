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
package it.govpay.common.batch.runner;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import it.govpay.common.batch.service.JobConcurrencyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Helper class per l'esecuzione di job Spring Batch con gestione della concorrenza.
 * <p>
 * Fornisce metodi comuni per:
 * <ul>
 *   <li>Verificare se un job è già in esecuzione</li>
 *   <li>Gestire job stale (bloccati)</li>
 *   <li>Coordinare esecuzioni in ambiente multi-nodo</li>
 *   <li>Eseguire job con parametri standard</li>
 * </ul>
 * <p>
 * Questa classe è utilizzata internamente da {@link AbstractCronJobRunner} e
 * {@link AbstractScheduledJobRunner}, ma può essere usata anche direttamente.
 */
@Slf4j
public class JobExecutionHelper {

    /** Nome del parametro job per l'ID del job */
    public static final String JOB_PARAM_JOB_ID = "JobID";

    /** Nome del parametro job per il timestamp di esecuzione */
    public static final String JOB_PARAM_WHEN = "When";

    /** Nome del parametro job per il cluster ID */
    public static final String JOB_PARAM_CLUSTER_ID = "ClusterID";

    private final JobLauncher jobLauncher;
    private final JobConcurrencyService jobConcurrencyService;
    private final String clusterId;
    private final ZoneId zoneId;

    /**
     * Costruisce un nuovo helper per l'esecuzione di job.
     *
     * @param jobLauncher JobLauncher di Spring Batch
     * @param jobConcurrencyService Service per la gestione della concorrenza
     * @param clusterId Identificativo del cluster/nodo corrente
     * @param zoneId Timezone per i timestamp
     */
    public JobExecutionHelper(JobLauncher jobLauncher, JobConcurrencyService jobConcurrencyService,
            String clusterId, ZoneId zoneId) {
        this.jobLauncher = jobLauncher;
        this.jobConcurrencyService = jobConcurrencyService;
        this.clusterId = clusterId;
        this.zoneId = zoneId;
    }

    /**
     * Enum per il risultato del controllo pre-esecuzione.
     */
    public enum PreExecutionCheckResult {
        /** Nessun job in esecuzione, si può procedere */
        CAN_PROCEED,
        /** Job in esecuzione su altro nodo */
        RUNNING_ON_OTHER_NODE,
        /** Job in esecuzione su questo nodo */
        RUNNING_ON_THIS_NODE,
        /** Job stale abbandonato con successo, si può procedere */
        STALE_ABANDONED_CAN_PROCEED,
        /** Job stale ma impossibile abbandonare */
        STALE_ABANDON_FAILED
    }

    /**
     * Risultato del controllo pre-esecuzione con informazioni aggiuntive.
     */
    public record PreExecutionResult(PreExecutionCheckResult result, JobExecution currentExecution, String runningClusterId) {
        public boolean canProceed() {
            return result == PreExecutionCheckResult.CAN_PROCEED ||
                   result == PreExecutionCheckResult.STALE_ABANDONED_CAN_PROCEED;
        }
    }

    /**
     * Verifica se è possibile eseguire il job, gestendo eventuali esecuzioni in corso o stale.
     * <p>
     * Questo metodo implementa la logica comune per:
     * <ol>
     *   <li>Verificare se c'è un job in esecuzione</li>
     *   <li>Se il job è stale, tentare di abbandonarlo</li>
     *   <li>Restituire il risultato appropriato per la decisione di esecuzione</li>
     * </ol>
     *
     * @param jobName Nome del job da verificare
     * @return PreExecutionResult con il risultato del controllo
     */
    public PreExecutionResult checkBeforeExecution(String jobName) {
        JobExecution currentExecution = jobConcurrencyService.getCurrentRunningJobExecution(jobName);

        if (currentExecution == null) {
            return new PreExecutionResult(PreExecutionCheckResult.CAN_PROCEED, null, null);
        }

        // Verifica se il job è stale
        if (jobConcurrencyService.isJobExecutionStale(currentExecution)) {
            log.warn("JobExecution {} rilevata come STALE. Procedo con abbandono.", currentExecution.getId());

            if (jobConcurrencyService.abandonStaleJobExecution(currentExecution)) {
                log.info("Job stale abbandonato con successo.");
                return new PreExecutionResult(PreExecutionCheckResult.STALE_ABANDONED_CAN_PROCEED, currentExecution, null);
            } else {
                log.error("Impossibile abbandonare il job stale.");
                return new PreExecutionResult(PreExecutionCheckResult.STALE_ABANDON_FAILED, currentExecution, null);
            }
        }

        // Job in esecuzione normale - verifica il cluster
        String runningClusterId = jobConcurrencyService.getClusterIdFromExecution(currentExecution);

        if (runningClusterId != null && !runningClusterId.equals(this.clusterId)) {
            log.info("Il job {} è in esecuzione su un altro nodo ({}).", jobName, runningClusterId);
            return new PreExecutionResult(PreExecutionCheckResult.RUNNING_ON_OTHER_NODE, currentExecution, runningClusterId);
        } else {
            log.warn("Il job {} è ancora in esecuzione sul nodo corrente ({}).", jobName, runningClusterId);
            return new PreExecutionResult(PreExecutionCheckResult.RUNNING_ON_THIS_NODE, currentExecution, runningClusterId);
        }
    }

    /**
     * Esegue un job con i parametri standard.
     *
     * @param job Il job da eseguire
     * @param jobName Nome identificativo del job
     * @return JobExecution dell'esecuzione avviata
     * @throws JobExecutionAlreadyRunningException se il job è già in esecuzione
     * @throws JobRestartException se il job non può essere riavviato
     * @throws JobInstanceAlreadyCompleteException se l'istanza del job è già completata
     * @throws JobParametersInvalidException se i parametri non sono validi
     */
    public JobExecution runJob(Job job, String jobName) throws JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
        JobParameters params = buildJobParameters(jobName);
        return jobLauncher.run(job, params);
    }

    /**
     * Costruisce i parametri standard per un job.
     *
     * @param jobName Nome identificativo del job
     * @return JobParameters con i parametri standard
     */
    public JobParameters buildJobParameters(String jobName) {
        return new JobParametersBuilder()
                .addString(JOB_PARAM_JOB_ID, jobName)
                .addString(JOB_PARAM_WHEN, OffsetDateTime.now(zoneId).toString())
                .addString(JOB_PARAM_CLUSTER_ID, this.clusterId)
                .toJobParameters();
    }

    /**
     * Costruisce i parametri per un job con parametri aggiuntivi personalizzati.
     *
     * @param jobName Nome identificativo del job
     * @param additionalParams Builder con parametri aggiuntivi già configurati
     * @return JobParameters con i parametri standard più quelli aggiuntivi
     */
    public JobParameters buildJobParameters(String jobName, JobParametersBuilder additionalParams) {
        return additionalParams
                .addString(JOB_PARAM_JOB_ID, jobName)
                .addString(JOB_PARAM_WHEN, OffsetDateTime.now(zoneId).toString())
                .addString(JOB_PARAM_CLUSTER_ID, this.clusterId)
                .toJobParameters();
    }

    /**
     * Verifica e gestisce job stale, eseguendo poi il job se possibile.
     * <p>
     * Questo metodo combina checkBeforeExecution e runJob in un'unica operazione.
     *
     * @param job Il job da eseguire
     * @param jobName Nome identificativo del job
     * @return JobExecution se il job è stato eseguito, null se non è stato possibile avviarlo
     * @throws JobExecutionAlreadyRunningException se il job è già in esecuzione
     * @throws JobRestartException se il job non può essere riavviato
     * @throws JobInstanceAlreadyCompleteException se l'istanza del job è già completata
     * @throws JobParametersInvalidException se i parametri non sono validi
     */
    public JobExecution executeIfPossible(Job job, String jobName) throws JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {

        PreExecutionResult checkResult = checkBeforeExecution(jobName);

        if (checkResult.canProceed()) {
            return runJob(job, jobName);
        }

        return null;
    }

    /**
     * Restituisce il JobConcurrencyService.
     *
     * @return il JobConcurrencyService
     */
    public JobConcurrencyService getJobConcurrencyService() {
        return jobConcurrencyService;
    }

    /**
     * Restituisce il cluster ID configurato.
     *
     * @return il cluster ID
     */
    public String getClusterId() {
        return clusterId;
    }
}
