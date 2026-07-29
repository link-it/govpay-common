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
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import it.govpay.common.batch.TriggerType;
import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionCheckResult;
import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionResult;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe base astratta per l'esecuzione di job batch da command line (modalità cron esterno).
 * <p>
 * Questa classe implementa {@link CommandLineRunner} per eseguire il job una sola volta
 * al startup dell'applicazione, tipicamente quando il batch viene lanciato da un cron
 * di sistema o da uno scheduler esterno.
 * <p>
 * Funzionamento:
 * <ul>
 *   <li>Esegue il job una sola volta al startup</li>
 *   <li>Prima di avviare il job, verifica se è già in esecuzione (su qualsiasi nodo)</li>
 *   <li>Se il job è in esecuzione su un altro nodo, esce senza avviarlo</li>
 *   <li>Se il job è stale (bloccato), lo abbandona e avvia una nuova esecuzione</li>
 *   <li>Al termine del job, termina l'applicazione</li>
 * </ul>
 * <p>
 * Per utilizzare questa classe, creare una sottoclasse e annotarla con:
 * <pre>
 * &#64;Component
 * &#64;Profile("cron")
 * public class MyCronJobRunner extends AbstractCronJobRunner {
 *     // ...
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractCronJobRunner implements CommandLineRunner, ApplicationContextAware {

    private ApplicationContext context;
    private final JobExecutionHelper jobExecutionHelper;
    private final Job job;
    private final String jobName;

    /**
     * Costruisce il runner per l'esecuzione da cron.
     *
     * @param jobExecutionHelper Helper per l'esecuzione del job
     * @param job Il job Spring Batch da eseguire
     * @param jobName Nome identificativo del job
     */
    protected AbstractCronJobRunner(JobExecutionHelper jobExecutionHelper, Job job, String jobName) {
        this.jobExecutionHelper = jobExecutionHelper;
        this.job = job;
        this.jobName = jobName;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Avvio {} da command line (modalità cron)", jobName);

        PreExecutionResult checkResult = jobExecutionHelper.checkBeforeExecution(jobName);

        switch (checkResult.result()) {
            case CAN_PROCEED -> executeAndExit();

            case STALE_ABANDONED_CAN_PROCEED -> {
                log.info("Job stale abbandonato con successo. Avvio nuova esecuzione.");
                executeAndExit();
            }

            case STALE_ABANDON_FAILED -> {
                log.error("Impossibile abbandonare il job stale. Uscita con errore.");
                exitApplication(1);
            }

            case RUNNING_ON_OTHER_NODE -> {
                log.info("Il job {} è in esecuzione su un altro nodo ({}). Uscita.",
                    jobName, checkResult.runningClusterId());
                exitApplication(0);
            }

            case RUNNING_ON_THIS_NODE -> {
                log.warn("Il job {} è ancora in esecuzione sul nodo corrente ({}). Uscita.",
                    jobName, checkResult.runningClusterId());
                exitApplication(0);
            }
        }
    }

    /**
     * Esegue il job e termina l'applicazione.
     */
    private void executeAndExit() throws JobExecutionAlreadyRunningException, JobRestartException,
            JobInstanceAlreadyCompleteException, InvalidJobParametersException {
        JobExecution execution = jobExecutionHelper.runJob(job, jobName, TriggerType.SCHEDULED);

        if (execution != null) {
            log.info("{} completato con stato: {}", jobName, execution.getStatus());
            onJobCompleted(execution);
        }

        exitApplication(0);
    }

    /**
     * Hook chiamato quando il job è completato, prima della terminazione dell'applicazione.
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
     * Termina l'applicazione con il codice di uscita specificato.
     *
     * @param exitCode Codice di uscita (0 = successo, != 0 = errore)
     */
    protected void exitApplication(int exitCode) {
        int code = SpringApplication.exit(context, () -> exitCode);
        System.exit(code);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    /**
     * Restituisce l'ApplicationContext.
     *
     * @return l'ApplicationContext
     */
    protected ApplicationContext getApplicationContext() {
        return context;
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
