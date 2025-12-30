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
package it.govpay.common.batch.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;

import lombok.extern.slf4j.Slf4j;

/**
 * Listener base astratto per stampare un riepilogo dell'esecuzione del batch.
 * <p>
 * Fornisce funzionalita' comuni per:
 * <ul>
 *   <li>Log di inizio batch con job ID e timestamp</li>
 *   <li>Riepilogo finale con status, durata e statistiche per step</li>
 *   <li>Supporto per step partizionati con statistiche aggregate</li>
 * </ul>
 * <p>
 * Le sottoclassi possono:
 * <ul>
 *   <li>Personalizzare il nome del batch tramite {@link #getBatchName()}</li>
 *   <li>Aggiungere statistiche specifiche sovrascrivendo {@link #printStepStatistics(JobExecution)}</li>
 *   <li>Personalizzare i messaggi di log</li>
 * </ul>
 */
@Slf4j
public abstract class AbstractBatchExecutionListener implements JobExecutionListener {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int SEPARATOR_LENGTH = 80;
    private static final String SEPARATOR = "=".repeat(SEPARATOR_LENGTH);
    private static final String SEPARATOR_DASH = "-".repeat(SEPARATOR_LENGTH);

    /**
     * Restituisce il nome del batch da visualizzare nei log.
     *
     * @return nome del batch
     */
    protected abstract String getBatchName();

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(SEPARATOR);
        log.info("INIZIO BATCH {}", getBatchName());
        log.info("Job ID: {}", jobExecution.getJobId());
        log.info("Avvio: {}", LocalDateTime.now().format(TIME_FORMATTER));
        log.info(SEPARATOR);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info(SEPARATOR);
        log.info("RIEPILOGO ESECUZIONE BATCH");
        log.info(SEPARATOR);

        // Statistiche generali
        Duration duration = Duration.between(
                jobExecution.getStartTime(),
                jobExecution.getEndTime());

        log.info("Status finale: {}", jobExecution.getStatus());
        log.info("Durata totale: {} secondi", duration.getSeconds());
        log.info("");

        // Statistiche per step (delegato alle sottoclassi)
        printStepStatistics(jobExecution);

        log.info(SEPARATOR);
    }

    /**
     * Stampa le statistiche per gli step del job.
     * <p>
     * Le sottoclassi devono sovrascrivere questo metodo per stampare
     * le statistiche specifiche dei propri step.
     *
     * @param jobExecution l'esecuzione del job
     */
    protected abstract void printStepStatistics(JobExecution jobExecution);

    // ==================== Utility Methods ====================

    /**
     * Stampa le statistiche di uno step semplice.
     *
     * @param stepExecution lo step da loggare
     * @param stepNumber    numero dello step
     * @param stepLabel     descrizione dello step
     */
    protected void printSimpleStepStats(StepExecution stepExecution, int stepNumber, String stepLabel) {
        log.info("--- STEP {}: {} ---", stepNumber, stepLabel);
        log.info("Status: {}", stepExecution.getStatus());
        log.info("Elementi letti: {}", stepExecution.getReadCount());
        log.info("Elementi scritti: {}", stepExecution.getWriteCount());
        log.info("Elementi saltati: {}", stepExecution.getSkipCount());

        long durationMs = Duration.between(stepExecution.getStartTime(), stepExecution.getEndTime()).toMillis();
        log.info("Durata: {} ms", durationMs);
        log.info("");
    }

    /**
     * Stampa le statistiche di uno step partizionato.
     *
     * @param jobExecution      l'esecuzione del job
     * @param masterStepName    nome dello step master (es. "fdrMetadataAcquisitionStep")
     * @param workerStepPrefix  prefisso degli step worker (es. "fdrMetadataWorkerStep")
     * @param stepNumber        numero dello step
     * @param stepLabel         descrizione dello step
     */
    protected void printPartitionedStepStats(JobExecution jobExecution, String masterStepName,
            String workerStepPrefix, int stepNumber, String stepLabel) {

        StepExecution masterStep = findStepByName(jobExecution, masterStepName);
        if (masterStep == null) {
            log.info("--- STEP {}: {} (non eseguito) ---", stepNumber, stepLabel);
            log.info("");
            return;
        }

        log.info("--- STEP {}: {} (PARTIZIONATO) ---", stepNumber, stepLabel);
        log.info("Status master step: {}", masterStep.getStatus());

        // Trova tutte le partizioni
        List<StepExecution> partitionSteps = jobExecution.getStepExecutions().stream()
                .filter(se -> se.getStepName().startsWith(workerStepPrefix))
                .toList();

        if (partitionSteps.isEmpty()) {
            log.info("Nessuna partizione eseguita (nessun elemento da processare)");
            log.info("");
            return;
        }

        // Calcola statistiche aggregate
        PartitionedStepStats stats = calculatePartitionedStats(partitionSteps);

        log.info("Partizioni totali: {}", partitionSteps.size());
        log.info("Elementi letti: {}", stats.totalRead);
        log.info("Elementi scritti: {}", stats.totalWritten);
        log.info("Elementi saltati: {}", stats.totalSkipped);
        log.info("Errori: {}", stats.totalErrors);
        log.info("Durata totale: {} secondi", stats.totalDuration / 1000);
        log.info("");

        // Dettaglio per partizione se richiesto
        if (!stats.partitionStats.isEmpty()) {
            printPartitionDetails(stats.partitionStats);
        }
    }

    /**
     * Stampa il dettaglio per ogni partizione.
     *
     * @param partitionStats mappa con statistiche per partizione
     */
    protected void printPartitionDetails(Map<String, PartitionStats> partitionStats) {
        log.info("Dettaglio per partizione:");
        log.info(SEPARATOR_DASH);
        log.info(String.format("%-25s %-10s %-10s %-10s %-10s %-10s",
                "PARTIZIONE", "LETTI", "SCRITTI", "SALTATI", "ERRORI", "DURATA(s)"));
        log.info(SEPARATOR_DASH);

        partitionStats.values().forEach(stats ->
                log.info(String.format("%-25s %-10d %-10d %-10d %-10d %-10.1f",
                        truncate(stats.partitionId, 25),
                        stats.readCount,
                        stats.writeCount,
                        stats.skipCount,
                        stats.errorCount,
                        stats.durationMs / 1000.0
                ))
        );
        log.info(SEPARATOR_DASH);
        log.info("");
    }

    /**
     * Trova uno step per nome.
     *
     * @param jobExecution l'esecuzione del job
     * @param stepName     nome dello step
     * @return StepExecution o null se non trovato
     */
    protected StepExecution findStepByName(JobExecution jobExecution, String stepName) {
        return jobExecution.getStepExecutions().stream()
                .filter(se -> se.getStepName().equals(stepName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Calcola le statistiche aggregate per step partizionati.
     */
    private PartitionedStepStats calculatePartitionedStats(Collection<StepExecution> partitionSteps) {
        PartitionedStepStats result = new PartitionedStepStats();

        for (StepExecution partitionExec : partitionSteps) {
            result.totalRead += partitionExec.getReadCount();
            result.totalWritten += partitionExec.getWriteCount();
            result.totalSkipped += partitionExec.getWriteSkipCount();
            result.totalErrors += partitionExec.getReadSkipCount() + partitionExec.getProcessSkipCount();

            long duration = Duration.between(partitionExec.getStartTime(), partitionExec.getEndTime()).toMillis();
            result.totalDuration += duration;

            String partitionId = extractPartitionId(partitionExec);
            PartitionStats stats = new PartitionStats();
            stats.partitionId = partitionId;
            stats.readCount = (int) partitionExec.getReadCount();
            stats.writeCount = (int) partitionExec.getWriteCount();
            stats.skipCount = (int) partitionExec.getWriteSkipCount();
            stats.errorCount = (int) (partitionExec.getReadSkipCount() + partitionExec.getProcessSkipCount());
            stats.status = partitionExec.getStatus().toString();
            stats.durationMs = duration;
            result.partitionStats.put(partitionId, stats);
        }

        return result;
    }

    /**
     * Estrae l'identificativo della partizione dall'esecuzione dello step.
     */
    private String extractPartitionId(StepExecution stepExecution) {
        // Prova a estrarre dal context
        if (stepExecution.getExecutionContext().containsKey("partitionId")) {
            return stepExecution.getExecutionContext().getString("partitionId");
        }
        if (stepExecution.getExecutionContext().containsKey("codDominio")) {
            return stepExecution.getExecutionContext().getString("codDominio");
        }

        // Estrai dal nome dello step (formato: stepName:partition-ID)
        String stepName = stepExecution.getStepName();
        if (stepName.contains(":partition-")) {
            return stepName.substring(stepName.indexOf(":partition-") + 11);
        }

        return "partition-" + stepExecution.getId();
    }

    /**
     * Tronca una stringa alla lunghezza massima.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }

    // ==================== Inner Classes ====================

    /**
     * Statistiche aggregate per step partizionati.
     */
    protected static class PartitionedStepStats {
        long totalRead = 0;
        long totalWritten = 0;
        long totalSkipped = 0;
        long totalErrors = 0;
        long totalDuration = 0;
        Map<String, PartitionStats> partitionStats = new LinkedHashMap<>();
    }

    /**
     * Statistiche per una singola partizione.
     */
    protected static class PartitionStats {
        String partitionId;
        int readCount;
        int writeCount;
        int skipCount;
        int errorCount;
        String status;
        long durationMs;
    }
}
