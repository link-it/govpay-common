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
package it.govpay.common.batch.config;

import java.time.ZoneId;

import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;

import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;

/**
 * Factory class per creare i componenti comuni per i batch.
 * <p>
 * Questa classe non è una configurazione Spring auto-registrata, ma fornisce
 * metodi factory statici che i progetti batch possono utilizzare nelle loro
 * configurazioni per creare i bean necessari.
 * <p>
 * Esempio di utilizzo in un progetto batch:
 * <pre>
 * &#64;Configuration
 * public class MyBatchConfig {
 *
 *     &#64;Bean
 *     public JobConcurrencyService jobConcurrencyService(JobExplorer explorer, JobRepository repo) {
 *         return BatchCommonAutoConfiguration.createJobConcurrencyService(explorer, repo, 120);
 *     }
 *
 *     &#64;Bean
 *     public JobExecutionHelper jobExecutionHelper(
 *             JobLauncher launcher,
 *             JobConcurrencyService concurrencyService) {
 *         return BatchCommonAutoConfiguration.createJobExecutionHelper(
 *             launcher, concurrencyService, "my-cluster-id", ZoneId.of("Europe/Rome"));
 *     }
 * }
 * </pre>
 */
public final class BatchCommonAutoConfiguration {

    private BatchCommonAutoConfiguration() {
        // Utility class
    }

    /**
     * Crea un JobConcurrencyService per la gestione della concorrenza dei job.
     *
     * @param jobExplorer JobExplorer per interrogare lo stato dei job
     * @param jobRepository JobRepository per aggiornare lo stato dei job
     * @param staleThresholdMinutes Soglia in minuti per considerare un job stale
     * @return JobConcurrencyService configurato
     */
    public static JobConcurrencyService createJobConcurrencyService(
            JobExplorer jobExplorer,
            JobRepository jobRepository,
            int staleThresholdMinutes) {
        return new JobConcurrencyService(jobExplorer, jobRepository, staleThresholdMinutes);
    }

    /**
     * Crea un JobConcurrencyService usando le properties di configurazione.
     *
     * @param jobExplorer JobExplorer per interrogare lo stato dei job
     * @param jobRepository JobRepository per aggiornare lo stato dei job
     * @param properties Properties di configurazione del batch
     * @return JobConcurrencyService configurato
     */
    public static JobConcurrencyService createJobConcurrencyService(
            JobExplorer jobExplorer,
            JobRepository jobRepository,
            BatchJobProperties properties) {
        return new JobConcurrencyService(jobExplorer, jobRepository, properties.getStaleThresholdMinutes());
    }

    /**
     * Crea un JobExecutionHelper per l'esecuzione dei job.
     *
     * @param jobLauncher JobLauncher di Spring Batch
     * @param jobConcurrencyService Service per la gestione della concorrenza
     * @param clusterId Identificativo del cluster/nodo
     * @param zoneId Timezone per i timestamp
     * @return JobExecutionHelper configurato
     */
    public static JobExecutionHelper createJobExecutionHelper(
            JobLauncher jobLauncher,
            JobConcurrencyService jobConcurrencyService,
            String clusterId,
            ZoneId zoneId) {
        return new JobExecutionHelper(jobLauncher, jobConcurrencyService, clusterId, zoneId);
    }

    /**
     * Crea un JobExecutionHelper usando le properties di configurazione.
     *
     * @param jobLauncher JobLauncher di Spring Batch
     * @param jobConcurrencyService Service per la gestione della concorrenza
     * @param properties Properties di configurazione del batch
     * @return JobExecutionHelper configurato
     */
    public static JobExecutionHelper createJobExecutionHelper(
            JobLauncher jobLauncher,
            JobConcurrencyService jobConcurrencyService,
            BatchJobProperties properties) {
        return new JobExecutionHelper(
                jobLauncher,
                jobConcurrencyService,
                properties.getClusterId(),
                properties.getZoneId());
    }
}
