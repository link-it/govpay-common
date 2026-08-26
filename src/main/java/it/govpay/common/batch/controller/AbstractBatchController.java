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
package it.govpay.common.batch.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import it.govpay.common.batch.TriggerType;
import it.govpay.common.batch.dto.BatchInfo;
import it.govpay.common.batch.dto.BatchStatusInfo;
import it.govpay.common.batch.dto.ExecutionSummaryInfo;
import it.govpay.common.batch.dto.ExecutionsPage;
import it.govpay.common.batch.dto.LastExecutionInfo;
import it.govpay.common.batch.dto.NextExecutionInfo;
import it.govpay.common.batch.dto.Problem;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;
import it.govpay.common.entity.batch.BatchJobExecutionEntity;
import it.govpay.common.entity.batch.BatchJobExecutionParamEntity;
import it.govpay.common.utils.DurationUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
 *         super(jobExecutionHelper, jobRepository, environment, zoneId, schedulerIntervalMillis, entityManager);
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
    private final JobRepository jobRepository;
    private final Environment environment;
    private final ZoneId applicationZoneId;
    private final long schedulerIntervalMillis;
    private final EntityManager entityManager;

    /**
     * Costruisce il controller base.
     *
     * @param jobExecutionHelper Helper per l'esecuzione del job
     * @param jobRepository JobRepository per interrogare lo stato dei job
     * @param environment Environment per verificare i profili attivi
     * @param applicationZoneId Timezone dell'applicazione
     * @param schedulerIntervalMillis Intervallo di scheduling in millisecondi
     * @param entityManager EntityManager per le query Criteria sullo storico esecuzioni
     */
    protected AbstractBatchController(
            JobExecutionHelper jobExecutionHelper,
            JobRepository jobRepository,
            Environment environment,
            ZoneId applicationZoneId,
            long schedulerIntervalMillis,
            EntityManager entityManager) {
        this.jobExecutionHelper = jobExecutionHelper;
        this.jobRepository = jobRepository;
        this.environment = environment;
        this.applicationZoneId = applicationZoneId;
        this.schedulerIntervalMillis = schedulerIntervalMillis;
        this.entityManager = entityManager;
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

    /**
     * Nome human-readable del batch, per un consumer esterno che non conosce
     * il nome tecnico del job Spring Batch.
     *
     * @return il nome descrittivo del batch
     */
    protected abstract String getDisplayName();

    /**
     * Descrizione human-readable del batch.
     *
     * @return la descrizione del batch
     */
    protected abstract String getDescription();

    // ============ INFO ============

    @GetMapping("/info")
    public ResponseEntity<BatchInfo> info() {
        return ResponseEntity.ok(BatchInfo.builder()
                .jobName(getJobName())
                .displayName(getDisplayName())
                .description(getDescription())
                .build());
    }

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
            return problemResponse(Problem.internalServerError("Errore durante l'avvio: " + e.getMessage()));
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

        return problemResponse(
                Problem.serviceUnavailable("Impossibile terminare forzatamente il job in esecuzione (JobExecution ID: " + currentExecution.getId() + ")"));
    }

    private ResponseEntity<Object> gestisciJobStale(JobExecution currentExecution) {
        log.warn("JobExecution {} rilevata come STALE. Procedo con abbandono e riavvio.", currentExecution.getId());

        if (jobExecutionHelper.getJobConcurrencyService().abandonStaleJobExecution(currentExecution)) {
            log.info("Job stale abbandonato con successo. Avvio nuova esecuzione.");
            return null;
        }

        return problemResponse(
                Problem.serviceUnavailable("Impossibile abbandonare il job stale (JobExecution ID: " + currentExecution.getId() + ")"));
    }

    private ResponseEntity<Object> restituisciJobGiaInEsecuzione(JobExecution currentExecution) {
        String runningClusterId = jobExecutionHelper.getJobConcurrencyService().getClusterIdFromExecution(currentExecution);

        String detail = String.format(
                "Il job %s è già in esecuzione (JobExecution ID: %d, Cluster: %s). Usa il parametro force=true per terminarlo forzatamente.",
                getJobName(),
                currentExecution.getId(),
                runningClusterId);

        return problemResponse(Problem.conflict(detail));
    }

    private ResponseEntity<Object> avviaJobAsincrono() {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Avvio asincrono del job {}", getJobName());
                JobExecution execution = jobExecutionHelper.runJob(getJob(), getJobName(), TriggerType.MANUAL);
                log.info("Job {} terminato con stato: {}", getJobName(), execution.getStatus());
            } catch (Exception e) {
                log.error("Errore durante l'esecuzione asincrona del job: {}", e.getMessage(), e);
            }
        });

        return ResponseEntity.accepted().build();
    }

    // ============ CANCELLAZIONE ============

    /**
     * Richiede l'annullamento cooperativo di una JobExecution.
     * <p>
     * Best-effort: la richiesta viene solo segnalata (vedi
     * {@link JobExecutionHelper#stopExecution(long)}), non blocca in
     * attesa dell'effettivo arresto.
     */
    @DeleteMapping("/executions/{executionId}")
    public ResponseEntity<Object> stopExecution(@PathVariable long executionId) {
        log.info("Richiesta di annullamento della JobExecution {} (batch {})", executionId, getJobName());

        try {
            jobExecutionHelper.stopExecution(executionId);
            return ResponseEntity.accepted().build();
        } catch (JobExecutionNotRunningException e) {
            return problemResponse(Problem.conflict("La JobExecution " + executionId + " non e' in corso."));
        } catch (NoSuchJobExecutionException e) {
            return problemResponse(Problem.notFound("JobExecution " + executionId + " non trovata."));
        }
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
            Duration duration = DurationUtils.since(currentExecution.getStartTime(), applicationZoneId);
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
        List<JobInstance> jobInstances = jobRepository.getJobInstances(getJobName(), 0, 10);

        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> executions = jobRepository.getJobExecutions(jobInstance);
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
                .triggerType(execution.getJobParameters().getString(JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE))
                .build();
    }

    private Long calculateDurationSeconds(JobExecution execution) {
        return calculateDurationSeconds(execution.getStartTime(), execution.getEndTime());
    }

    private Long calculateDurationSeconds(LocalDateTime startTime, LocalDateTime endTime) {
        return DurationUtils.secondsBetween(startTime, endTime, applicationZoneId);
    }

    private String getTruncatedExitDescription(JobExecution execution) {
        return truncateExitDescription(execution.getExitStatus().getExitDescription());
    }

    private String truncateExitDescription(String description) {
        if (description == null) {
            return null;
        }
        if (description.length() > 500) {
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

        List<JobInstance> jobInstances = jobRepository.getJobInstances(getJobName(), 0, 5);
        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> executions = jobRepository.getJobExecutions(jobInstance);
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

    // ============ STORICO ESECUZIONI ============

    /**
     * Elenco paginato delle esecuzioni del batch, piu' recenti prima.
     * <p>
     * {@code stato} filtra sullo stato Spring Batch nativo (es. {@code COMPLETED},
     * {@code FAILED}), come singolo valore o lista comma-separated (es.
     * {@code stato=FAILED,UNKNOWN} — utile ai consumer la cui semantica
     * applicativa mappa su piu' stati grezzi); {@code dataInizioMin}/
     * {@code dataInizioMax} filtrano su {@code coalesce(startTime, createTime)}.
     * Se {@code total=true} viene eseguito anche un {@code COUNT(*)} per
     * valorizzare {@code totalResults}/{@code totalPages}; altrimenti
     * {@code hasNextPage} e' calcolato richiedendo una riga in piu' della
     * pagina, senza COUNT aggiuntivo.
     */
    @GetMapping("/executions")
    public ResponseEntity<Object> listExecutions(
            @RequestParam(required = false) String stato,
            @RequestParam(required = false) OffsetDateTime dataInizioMin,
            @RequestParam(required = false) OffsetDateTime dataInizioMax,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean total) {

        Set<String> stati = parseStatoCsv(stato);
        if (stati == null) {
            return problemResponse(Problem.badRequest("Il parametro 'stato' deve essere una lista (anche di un solo elemento) separata da virgole tra: "
                    + String.join(", ", batchStatusNames()) + "."));
        }
        if (page < 1) {
            return problemResponse(Problem.badRequest("Il parametro 'page' deve essere >= 1."));
        }
        if (limit < 1) {
            return problemResponse(Problem.badRequest("Il parametro 'limit' deve essere >= 1."));
        }

        Specification<BatchJobExecutionEntity> spec = Specification.allOf(
                Stream.of(
                        ExecutionSpecifications.jobNameEquals(getJobName()),
                        ExecutionSpecifications.statoIn(stati),
                        ExecutionSpecifications.dataInizioMin(toLocalDateTime(dataInizioMin)),
                        ExecutionSpecifications.dataInizioMax(toLocalDateTime(dataInizioMax)))
                .filter(Objects::nonNull)
                .toList());

        ExecutionsPage.ExecutionsPageBuilder responseBuilder = ExecutionsPage.builder().page(page).limit(limit);

        List<BatchJobExecutionEntity> rows;
        if (total) {
            long totalElements = countTotal(spec);
            int totalPages = (int) Math.ceil((double) totalElements / limit);
            rows = findSlice(spec, (page - 1) * limit, limit);
            responseBuilder.hasNextPage(page < totalPages)
                    .totalResults(totalElements)
                    .totalPages(totalPages);
        } else {
            List<BatchJobExecutionEntity> sliced = findSlice(spec, (page - 1) * limit, limit + 1);
            boolean hasNext = sliced.size() > limit;
            rows = hasNext ? sliced.subList(0, limit) : sliced;
            responseBuilder.hasNextPage(hasNext);
        }

        Map<Long, String> triggerTypes = findTriggerTypes(rows.stream().map(BatchJobExecutionEntity::getId).toList());
        responseBuilder.results(rows.stream().map(row -> toExecutionSummaryInfo(row, triggerTypes)).toList());

        return ResponseEntity.ok(responseBuilder.build());
    }

    /**
     * Dettaglio di una esecuzione, scoperta per id. 404 se l'esecuzione non
     * esiste o non appartiene a questo batch (stesso {@code getJobName()}).
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<Object> getExecution(@PathVariable long executionId) {
        BatchJobExecutionEntity execution = entityManager.find(BatchJobExecutionEntity.class, executionId);

        if (execution == null || !getJobName().equals(execution.getJobInstance().getJobName())) {
            return problemResponse(Problem.notFound("Esecuzione " + executionId + " non trovata per il batch " + getJobName() + "."));
        }

        Map<String, String> params = findExecutionParams(executionId);

        LastExecutionInfo info = LastExecutionInfo.builder()
                .executionId(execution.getId())
                .clusterId(params.get(JobConcurrencyService.JOB_PARAM_CLUSTER_ID))
                .startTime(effectiveStartTime(execution))
                .endTime(execution.getEndTime())
                .durationSeconds(calculateDurationSeconds(execution.getStartTime(), execution.getEndTime()))
                .status(execution.getStatus())
                .exitCode(execution.getExitCode())
                .exitDescription(truncateExitDescription(execution.getExitMessage()))
                .triggerType(params.get(JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE))
                .build();

        return ResponseEntity.ok(info);
    }

    private ExecutionSummaryInfo toExecutionSummaryInfo(BatchJobExecutionEntity execution, Map<Long, String> triggerTypes) {
        return ExecutionSummaryInfo.builder()
                .executionId(execution.getId())
                .status(execution.getStatus())
                .startTime(effectiveStartTime(execution))
                .endTime(execution.getEndTime())
                .triggerType(triggerTypes.get(execution.getId()))
                .build();
    }

    /** startTime e' null finche' l'esecuzione e' solo in coda (STARTING): createTime e' invece sempre valorizzato. */
    private static LocalDateTime effectiveStartTime(BatchJobExecutionEntity execution) {
        return execution.getStartTime() != null ? execution.getStartTime() : execution.getCreateTime();
    }

    /** {@code null} segnala un valore non valido (400); un set vuoto significa "nessun filtro". */
    private static Set<String> parseStatoCsv(String stato) {
        if (stato == null || stato.isBlank()) {
            return Set.of();
        }
        Set<String> stati = Arrays.stream(stato.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        return stati.stream().allMatch(AbstractBatchController::isValidBatchStatus) ? stati : null;
    }

    private static boolean isValidBatchStatus(String stato) {
        try {
            BatchStatus.valueOf(stato);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static List<String> batchStatusNames() {
        return Stream.of(BatchStatus.values()).map(Enum::name).toList();
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value != null ? value.atZoneSameInstant(applicationZoneId).toLocalDateTime() : null;
    }

    /**
     * Slice via Criteria (nessun COUNT aggiuntivo). Ordine per
     * {@code coalesce(startTime, createTime)} DESC (non esprimibile via
     * Sort/Pageable, solo path di proprieta'), id DESC come spareggio.
     */
    private List<BatchJobExecutionEntity> findSlice(Specification<BatchJobExecutionEntity> spec, int offset, int maxResults) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BatchJobExecutionEntity> q = cb.createQuery(BatchJobExecutionEntity.class);
        Root<BatchJobExecutionEntity> root = q.from(BatchJobExecutionEntity.class);
        Predicate predicate = spec.toPredicate(root, q, cb);
        if (predicate != null) {
            q.where(predicate);
        }
        q.orderBy(cb.desc(dataInizioExpression(cb, root)), cb.desc(root.get("id")));
        TypedQuery<BatchJobExecutionEntity> typed = entityManager.createQuery(q)
                .setFirstResult(offset)
                .setMaxResults(maxResults);
        return typed.getResultList();
    }

    private long countTotal(Specification<BatchJobExecutionEntity> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> q = cb.createQuery(Long.class);
        Root<BatchJobExecutionEntity> root = q.from(BatchJobExecutionEntity.class);
        Predicate predicate = spec.toPredicate(root, q, cb);
        q.select(cb.count(root));
        if (predicate != null) {
            q.where(predicate);
        }
        return entityManager.createQuery(q).getSingleResult();
    }

    private static Expression<LocalDateTime> dataInizioExpression(CriteriaBuilder cb, Root<BatchJobExecutionEntity> root) {
        return cb.coalesce(root.get("startTime"), root.get("createTime"));
    }

    /** Lookup batch dei JobParameters (nessun N+1) per una lista di esecuzioni. */
    private Map<Long, String> findTriggerTypes(List<Long> executionIds) {
        if (executionIds.isEmpty()) {
            return Map.of();
        }
        List<BatchJobExecutionParamEntity> rows = entityManager.createQuery(
                        "SELECT p FROM BatchJobExecutionParamEntity p "
                                + "WHERE p.parameterName = :paramName AND p.jobExecutionId IN :ids",
                        BatchJobExecutionParamEntity.class)
                .setParameter("paramName", JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE)
                .setParameter("ids", executionIds)
                .getResultList();

        Map<Long, String> result = new HashMap<>();
        for (BatchJobExecutionParamEntity row : rows) {
            result.put(row.getJobExecutionId(), row.getParameterValue());
        }
        return result;
    }

    /** Legge tutti i JobParameters di interesse (cluster id, trigger type) per una singola esecuzione. */
    private Map<String, String> findExecutionParams(long executionId) {
        List<BatchJobExecutionParamEntity> rows = entityManager.createQuery(
                        "SELECT p FROM BatchJobExecutionParamEntity p "
                                + "WHERE p.jobExecutionId = :executionId AND p.parameterName IN :names",
                        BatchJobExecutionParamEntity.class)
                .setParameter("executionId", executionId)
                .setParameter("names", Set.of(JobConcurrencyService.JOB_PARAM_CLUSTER_ID, JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE))
                .getResultList();

        Map<String, String> result = new HashMap<>();
        for (BatchJobExecutionParamEntity row : rows) {
            result.put(row.getParameterName(), row.getParameterValue());
        }
        return result;
    }

    // ============ HELPER ============

    private ResponseEntity<Object> problemResponse(Problem problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    // ============ CACHE ============

    /**
     * Svuota le cache applicative del batch.
     * <p>
     * Ogni sottoclasse implementa la logica specifica di reset delle proprie cache
     * (es. connettori, configurazione, dati di dominio, ecc.).
     *
     * @return ResponseEntity con messaggio di conferma
     */
    protected abstract ResponseEntity<String> clearCache();

    @GetMapping("/cache/clear")
    public ResponseEntity<String> clearCacheEndpoint() {
        return clearCache();
    }

    // ============ ACCESSORS ============

    protected JobExecutionHelper getJobExecutionHelper() {
        return jobExecutionHelper;
    }

    protected JobRepository getJobRepository() {
        return jobRepository;
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
