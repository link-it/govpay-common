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
package it.govpay.common.batch.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;

import it.govpay.common.batch.dto.BatchStatusInfo;
import it.govpay.common.batch.dto.LastExecutionInfo;
import it.govpay.common.batch.dto.NextExecutionInfo;
import it.govpay.common.batch.dto.Problem;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller base astratto per l'esecuzione manuale e il monitoraggio di job batch.
 * <p>
 * Fornisce endpoint REST comuni per:
 * <ul>
 *   <li>Esecuzione manuale del batch (con supporto per force)</li>
 *   <li>Verifica stato corrente del batch</li>
 *   <li>Informazioni sull'ultima esecuzione</li>
 *   <li>Informazioni sulla prossima esecuzione schedulata</li>
 * </ul>
 * <p>
 * Le sottoclassi devono:
 * <ol>
 *   <li>Annotare la classe con {@code @RestController} e {@code @RequestMapping}</li>
 *   <li>Implementare i metodi astratti per fornire le dipendenze specifiche</li>
 *   <li>Opzionalmente definire endpoint aggiuntivi specifici del batch</li>
 * </ol>
 * <p>
 * Esempio di implementazione:
 * <pre>
 * &#64;RestController
 * &#64;RequestMapping("/api/batch")
 * public class MyBatchController extends AbstractBatchController {
 *
 *     public MyBatchController(...) {
 *         super(jobExecutionHelper, jobExplorer, environment, zoneId, schedulerIntervalMillis);
 *     }
 *
 *     &#64;Override
 *     protected Job getJob() { return myJob; }
 *
 *     &#64;Override
 *     protected String getJobName() { return "myJobName"; }
 *
 *     // Esponi gli endpoint
 *     &#64;GetMapping("/run")
 *     public ResponseEntity&lt;Object&gt; run(&#64;RequestParam(defaultValue = "false") boolean force) {
 *         return eseguiJob(force);
 *     }
 *
 *     &#64;GetMapping("/status")
 *     public ResponseEntity&lt;BatchStatusInfo&gt; status() {
 *         return getStatus();
 *     }
 *     // ...
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractBatchController {

    private final JobExecutionHelper jobExecutionHelper;
    private final JobExplorer jobExplorer;
    private final Environment environment;
    private final ZoneId applicationZoneId;
    private final long schedulerIntervalMillis;

    /**
     * Costruisce il controller base.
     *
     * @param jobExecutionHelper Helper per l'esecuzione del job
     * @param jobExplorer JobExplorer per interrogare lo stato dei job
     * @param environment Environment per verificare i profili attivi
     * @param applicationZoneId Timezone dell'applicazione
     * @param schedulerIntervalMillis Intervallo di scheduling in millisecondi
     */
    protected AbstractBatchController(
            JobExecutionHelper jobExecutionHelper,
            JobExplorer jobExplorer,
            Environment environment,
            ZoneId applicationZoneId,
            long schedulerIntervalMillis) {
        this.jobExecutionHelper = jobExecutionHelper;
        this.jobExplorer = jobExplorer;
        this.environment = environment;
        this.applicationZoneId = applicationZoneId;
        this.schedulerIntervalMillis = schedulerIntervalMillis;
    }

    /**
     * Restituisce il Job da eseguire.
     *
     * @return il Job Spring Batch
     */
    protected abstract Job getJob();

    /**
     * Restituisce il nome del job.
     *
     * @return il nome identificativo del job
     */
    protected abstract String getJobName();

    // ============ ESECUZIONE MANUALE ============

    /**
     * Esegue il job manualmente in modo asincrono.
     * <p>
     * Il servizio avvia il job e restituisce immediatamente la risposta senza attendere
     * la terminazione del batch. Lo stato del job può essere verificato tramite
     * l'endpoint /status.
     *
     * @param force Se true, termina forzatamente l'eventuale esecuzione corrente
     * @return ResponseEntity con HTTP 202 (Accepted) se avviato, o Problem in caso di errore
     */
    protected ResponseEntity<Object> eseguiJob(boolean force) {
        log.info("Richiesta esecuzione manuale del job {} (force={})", getJobName(), force);

        try {
            ResponseEntity<Object> runningJobResponse = gestisciJobInEsecuzione(force);
            if (runningJobResponse != null) {
                return runningJobResponse;
            }

            return avviaJobAsincrono();

        } catch (Exception e) {
            log.error("Errore durante l'avvio del job: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Problem.internalServerError("Errore durante l'avvio: " + e.getMessage()));
        }
    }

    private ResponseEntity<Object> gestisciJobInEsecuzione(boolean force) {
        JobConcurrencyService concurrencyService = jobExecutionHelper.getJobConcurrencyService();
        JobExecution currentExecution = concurrencyService.getCurrentRunningJobExecution(getJobName());

        if (currentExecution == null) {
            return null;
        }

        if (force) {
            return gestisciForzaEsecuzione(currentExecution);
        }

        if (concurrencyService.isJobExecutionStale(currentExecution)) {
            return gestisciJobStale(currentExecution);
        }

        return restituisciJobGiaInEsecuzione(currentExecution);
    }

    private ResponseEntity<Object> gestisciForzaEsecuzione(JobExecution currentExecution) {
        log.warn("Parametro force=true: terminazione forzata di JobExecution {}", currentExecution.getId());

        if (jobExecutionHelper.getJobConcurrencyService().forceAbandonJobExecution(currentExecution, "Richiesta esecuzione forzata via API REST")) {
            log.info("Job terminato forzatamente con successo. Avvio nuova esecuzione.");
            return null;
        }

        return ResponseEntity.status(503).body(
                Problem.serviceUnavailable("Impossibile terminare forzatamente il job in esecuzione (JobExecution ID: " + currentExecution.getId() + ")"));
    }

    private ResponseEntity<Object> gestisciJobStale(JobExecution currentExecution) {
        log.warn("JobExecution {} rilevata come STALE. Procedo con abbandono e riavvio.", currentExecution.getId());

        if (jobExecutionHelper.getJobConcurrencyService().abandonStaleJobExecution(currentExecution)) {
            log.info("Job stale abbandonato con successo. Avvio nuova esecuzione.");
            return null;
        }

        return ResponseEntity.status(503).body(
                Problem.serviceUnavailable("Impossibile abbandonare il job stale (JobExecution ID: " + currentExecution.getId() + ")"));
    }

    private ResponseEntity<Object> restituisciJobGiaInEsecuzione(JobExecution currentExecution) {
        String runningClusterId = jobExecutionHelper.getJobConcurrencyService().getClusterIdFromExecution(currentExecution);

        String detail = String.format(
                "Il job %s è già in esecuzione (JobExecution ID: %d, Cluster: %s). Usa il parametro force=true per terminarlo forzatamente.",
                getJobName(),
                currentExecution.getId(),
                runningClusterId);

        return ResponseEntity.status(409).body(Problem.conflict(detail));
    }

    private ResponseEntity<Object> avviaJobAsincrono() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Avvio asincrono del job {}", getJobName());
                JobExecution execution = jobExecutionHelper.runJob(getJob(), getJobName());
                log.info("Job {} terminato con stato: {}", getJobName(), execution.getStatus());
            } catch (Exception e) {
                log.error("Errore durante l'esecuzione asincrona del job: {}", e.getMessage(), e);
            }
        });

        return ResponseEntity.accepted().build();
    }

    // ============ MONITORAGGIO ============

    /**
     * Verifica se il batch è attualmente in esecuzione.
     *
     * @return BatchStatusInfo con le informazioni sullo stato corrente
     */
    protected ResponseEntity<BatchStatusInfo> getStatus() {
        log.debug("Richiesta stato del batch {}", getJobName());

        JobExecution currentExecution = jobExecutionHelper.getJobConcurrencyService()
                .getCurrentRunningJobExecution(getJobName());

        if (currentExecution == null) {
            return ResponseEntity.ok(BatchStatusInfo.builder()
                    .running(false)
                    .build());
        }

        Long runningSeconds = null;
        if (currentExecution.getStartTime() != null) {
            Duration duration = Duration.between(currentExecution.getStartTime(), LocalDateTime.now(applicationZoneId));
            runningSeconds = duration.getSeconds();
        }

        String currentStep = currentExecution.getStepExecutions().stream()
                .filter(se -> se.getStatus() == BatchStatus.STARTED)
                .map(StepExecution::getStepName)
                .findFirst()
                .orElse(null);

        String runningClusterId = jobExecutionHelper.getJobConcurrencyService().getClusterIdFromExecution(currentExecution);

        return ResponseEntity.ok(BatchStatusInfo.builder()
                .running(true)
                .executionId(currentExecution.getId())
                .clusterId(runningClusterId)
                .startTime(currentExecution.getStartTime())
                .runningSeconds(runningSeconds)
                .status(currentExecution.getStatus().name())
                .currentStep(currentStep)
                .build());
    }

    /**
     * Restituisce le informazioni sull'ultima esecuzione completata.
     *
     * @return LastExecutionInfo con le informazioni sull'ultima esecuzione
     */
    protected ResponseEntity<LastExecutionInfo> getLastExecution() {
        log.debug("Richiesta ultima esecuzione del batch {}", getJobName());

        JobExecution lastCompletedExecution = findLastCompletedExecution();

        if (lastCompletedExecution == null) {
            return ResponseEntity.ok(LastExecutionInfo.builder().build());
        }

        return ResponseEntity.ok(buildLastExecutionInfo(lastCompletedExecution));
    }

    private JobExecution findLastCompletedExecution() {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(getJobName(), 0, 10);

        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
            for (JobExecution execution : executions) {
                if (isCompletedExecution(execution)) {
                    return execution;
                }
            }
        }
        return null;
    }

    private boolean isCompletedExecution(JobExecution execution) {
        BatchStatus status = execution.getStatus();
        return status != BatchStatus.STARTED
            && status != BatchStatus.STARTING
            && status != BatchStatus.STOPPING;
    }

    private LastExecutionInfo buildLastExecutionInfo(JobExecution execution) {
        return LastExecutionInfo.builder()
                .executionId(execution.getId())
                .clusterId(jobExecutionHelper.getJobConcurrencyService().getClusterIdFromExecution(execution))
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .durationSeconds(calculateDurationSeconds(execution))
                .status(execution.getStatus().name())
                .exitCode(execution.getExitStatus().getExitCode())
                .exitDescription(getTruncatedExitDescription(execution))
                .build();
    }

    private Long calculateDurationSeconds(JobExecution execution) {
        if (execution.getStartTime() == null || execution.getEndTime() == null) {
            return null;
        }
        return Duration.between(execution.getStartTime(), execution.getEndTime()).getSeconds();
    }

    private String getTruncatedExitDescription(JobExecution execution) {
        String description = execution.getExitStatus().getExitDescription();
        if (description != null && description.length() > 500) {
            return description.substring(0, 500) + "...";
        }
        return description;
    }

    /**
     * Restituisce le informazioni sulla prossima esecuzione schedulata.
     *
     * @return NextExecutionInfo con le informazioni sulla prossima esecuzione
     */
    protected ResponseEntity<NextExecutionInfo> getNextExecution() {
        log.debug("Richiesta prossima esecuzione del batch {}", getJobName());

        boolean isCronMode = environment.matchesProfiles("cron");

        if (isCronMode) {
            return ResponseEntity.ok(NextExecutionInfo.builder()
                    .schedulingMode("cron")
                    .message("Scheduling gestito da cron esterno (OS/container)")
                    .build());
        }

        String intervalFormatted = formatInterval(schedulerIntervalMillis);

        LocalDateTime lastCompletedTime = null;
        LocalDateTime nextExecutionTime = null;

        List<JobInstance> jobInstances = jobExplorer.getJobInstances(getJobName(), 0, 5);
        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
            for (JobExecution execution : executions) {
                if (execution.getEndTime() != null) {
                    lastCompletedTime = execution.getEndTime();
                    nextExecutionTime = lastCompletedTime.plusNanos(schedulerIntervalMillis * 1_000_000);
                    break;
                }
            }
            if (lastCompletedTime != null) break;
        }

        LocalDateTime now = LocalDateTime.now(applicationZoneId);
        if (nextExecutionTime == null) {
            nextExecutionTime = now;
        }

        if (nextExecutionTime.isBefore(now)) {
            JobExecution currentExecution = jobExecutionHelper.getJobConcurrencyService()
                    .getCurrentRunningJobExecution(getJobName());
            if (currentExecution != null) {
                nextExecutionTime = null;
            } else {
                nextExecutionTime = now;
            }
        }

        return ResponseEntity.ok(NextExecutionInfo.builder()
                .schedulingMode("scheduler")
                .nextExecutionTime(nextExecutionTime)
                .intervalMillis(schedulerIntervalMillis)
                .intervalFormatted(intervalFormatted)
                .lastCompletedTime(lastCompletedTime)
                .build());
    }

    /**
     * Formatta un intervallo in millisecondi in formato human-readable.
     *
     * @param millis Intervallo in millisecondi
     * @return Stringa formattata (es. "10 minuti", "2 ore")
     */
    protected String formatInterval(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            long remainingMinutes = minutes % 60;
            if (remainingMinutes > 0) {
                return String.format("%d ore %d minuti", hours, remainingMinutes);
            }
            return String.format("%d ore", hours);
        } else if (minutes > 0) {
            return String.format("%d minuti", minutes);
        } else {
            return String.format("%d secondi", seconds);
        }
    }

    // ============ ACCESSORS ============

    protected JobExecutionHelper getJobExecutionHelper() {
        return jobExecutionHelper;
    }

    protected JobExplorer getJobExplorer() {
        return jobExplorer;
    }

    protected Environment getEnvironment() {
        return environment;
    }

    protected ZoneId getApplicationZoneId() {
        return applicationZoneId;
    }

    protected long getSchedulerIntervalMillis() {
        return schedulerIntervalMillis;
    }
}
