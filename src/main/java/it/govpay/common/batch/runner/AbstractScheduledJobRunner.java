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
package it.govpay.common.batch.runner;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobRestartException;

import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe base astratta per l'esecuzione periodica di job batch (modalità scheduler interno).
 * <p>
 * Questa classe fornisce la logica comune per l'esecuzione schedulata di job batch.
 * Le sottoclassi devono implementare il metodo schedulato con l'annotazione {@code @Scheduled}.
 * <p>
 * Funzionamento:
 * <ul>
 *   <li>Prima di avviare il job, verifica se è già in esecuzione (su qualsiasi nodo)</li>
 *   <li>Se il job è in esecuzione su un altro nodo, esce senza avviarlo</li>
 *   <li>Se il job è stale (bloccato), lo abbandona e avvia una nuova esecuzione</li>
 *   <li>Non termina l'applicazione al termine del job</li>
 * </ul>
 * <p>
 * Esempio di utilizzo:
 * <pre>
 * &#64;Component
 * &#64;Profile("default")
 * &#64;EnableScheduling
 * public class MyScheduledJobRunner extends AbstractScheduledJobRunner {
 *
 *     public MyScheduledJobRunner(JobExecutionHelper helper, Job myJob) {
 *         super(helper, myJob, "myJobName");
 *     }
 *
 *     &#64;Scheduled(fixedDelayString = "${scheduler.myJob.fixedDelayString:600000}",
 *                initialDelayString = "${scheduler.initialDelayString:1}")
 *     public JobExecution runScheduledJob() throws ... {
 *         return executeScheduledJob();
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractScheduledJobRunner {

    private final JobExecutionHelper jobExecutionHelper;
    private final Job job;
    private final String jobName;

    /**
     * Costruisce il runner per l'esecuzione schedulata.
     *
     * @param jobExecutionHelper Helper per l'esecuzione del job
     * @param job Il job Spring Batch da eseguire
     * @param jobName Nome identificativo del job
     */
    protected AbstractScheduledJobRunner(JobExecutionHelper jobExecutionHelper, Job job, String jobName) {
        this.jobExecutionHelper = jobExecutionHelper;
        this.job = job;
        this.jobName = jobName;
    }

    /**
     * Esegue il job schedulato con gestione della concorrenza.
     * <p>
     * Questo metodo deve essere chiamato dal metodo schedulato della sottoclasse.
     * Verifica se il job può essere eseguito e lo avvia se possibile.
     *
     * @return JobExecution se il job è stato eseguito, null se non è stato possibile avviarlo
     * @throws JobExecutionAlreadyRunningException se il job è già in esecuzione
     * @throws JobRestartException se il job non può essere riavviato
     * @throws JobInstanceAlreadyCompleteException se l'istanza del job è già completata
     * @throws InvalidJobParametersException se i parametri non sono validi
     */
    protected JobExecution executeScheduledJob() throws JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException, InvalidJobParametersException {

        log.info("Esecuzione schedulata di {}", jobName);

        PreExecutionResult checkResult = jobExecutionHelper.checkBeforeExecution(jobName);

        switch (checkResult.result()) {
            case CAN_PROCEED -> {
                return executeJob();
            }

            case STALE_ABANDONED_CAN_PROCEED -> {
                log.info("Job stale abbandonato con successo. Avvio nuova esecuzione.");
                return executeJob();
            }

            case STALE_ABANDON_FAILED -> {
                log.error("Impossibile abbandonare il job stale. Uscita senza avviare nuova esecuzione.");
                return null;
            }

            case RUNNING_ON_OTHER_NODE -> {
                log.info("Il job {} è in esecuzione su un altro nodo ({}). Uscita.",
                    jobName, checkResult.runningClusterId());
                return null;
            }

            case RUNNING_ON_THIS_NODE -> {
                log.warn("Il job {} è ancora in esecuzione sul nodo corrente ({}). Uscita.",
                    jobName, checkResult.runningClusterId());
                return null;
            }

            default -> {
                return null;
            }
        }
    }

    /**
     * Esegue il job e notifica il completamento.
     */
    private JobExecution executeJob() throws JobExecutionAlreadyRunningException, JobRestartException,
            JobInstanceAlreadyCompleteException, InvalidJobParametersException {

        JobExecution execution = jobExecutionHelper.runJob(job, jobName);

        if (execution != null) {
            log.info("{} completato con stato: {}", jobName, execution.getStatus());
            onJobCompleted(execution);
        }

        return execution;
    }

    /**
     * Hook chiamato quando il job è completato.
     * <p>
     * Le sottoclassi possono sovrascrivere questo metodo per eseguire operazioni
     * aggiuntive al termine del job.
     *
     * @param execution L'esecuzione completata del job
     */
    protected void onJobCompleted(JobExecution execution) {
        // Hook per le sottoclassi
    }

    /**
     * Restituisce il JobExecutionHelper.
     *
     * @return il JobExecutionHelper
     */
    protected JobExecutionHelper getJobExecutionHelper() {
        return jobExecutionHelper;
    }

    /**
     * Restituisce il Job.
     *
     * @return il Job
     */
    protected Job getJob() {
        return job;
    }

    /**
     * Restituisce il nome del job.
     *
     * @return il nome del job
     */
    protected String getJobName() {
        return jobName;
    }
}
