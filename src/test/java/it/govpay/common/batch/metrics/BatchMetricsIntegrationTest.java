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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.scheduling.annotation.Scheduled;

import io.micrometer.core.instrument.MeterRegistry;
import it.govpay.common.batch.config.BatchCommonAutoConfiguration;
import it.govpay.common.batch.config.BatchJobProperties;
import it.govpay.common.batch.config.BatchJobRepositoryAutoConfiguration;
import it.govpay.common.batch.runner.AbstractScheduledJobRunner;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;

/**
 * Metriche batch su uno stack reale: auto-configurazione metriche di Spring
 * Boot, JobRepository JDBC su H2, un job e un runner schedulato come nei
 * progetti batch, che non configurano nulla per le metriche.
 */
class BatchMetricsIntegrationTest {

    private static final String JOB = "jobMetriche";
    private static final String NODO = "nodo-1";
    private static final AtomicBoolean FALLISCI = new AtomicBoolean();

    private EmbeddedDatabase database;

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MetricsAutoConfiguration.class,
                        SimpleMetricsExportAutoConfiguration.class,
                        CompositeMeterRegistryAutoConfiguration.class,
                        BatchJobRepositoryAutoConfiguration.class,
                        BatchMetricsAutoConfiguration.class))
                .withBean(DataSource.class, () -> database)
                .withUserConfiguration(ContestoBatch.class)
                .withPropertyValues("scheduler.metriche.fixedDelay=PT30M");
    }

    @BeforeEach
    void setUp() {
        FALLISCI.set(false);
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("org/springframework/batch/core/schema-h2.sql")
                .build();
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    void esecuzioniSchedulateProduconoLeMetriche() {
        runner().run(context -> {
            assertThat(context).hasNotFailed();
            MeterRegistry registry = context.getBean(MeterRegistry.class);
            RunnerMetriche runnerMetriche = context.getBean(RunnerMetriche.class);

            assertThat(runnerMetriche.esegui().getStatus()).isEqualTo(BatchStatus.COMPLETED);
            FALLISCI.set(true);
            assertThat(runnerMetriche.esegui().getStatus()).isEqualTo(BatchStatus.FAILED);

            // durata ed esito, col tag comune del nodo
            assertThat(registry.get(BatchMetricsRecorder.METRICA_JOB)
                    .tags(BatchMetricsRecorder.TAG_JOB_NAME, JOB, BatchMetricsRecorder.TAG_TRIGGER, "SCHEDULED",
                            BatchMetricsRecorder.TAG_ESITO, "COMPLETED", BatchMetricsAutoConfiguration.TAG_NODO, NODO)
                    .timer().count()).isEqualTo(1);
            assertThat(registry.get(BatchMetricsRecorder.METRICA_JOB)
                    .tags(BatchMetricsRecorder.TAG_ESITO, "FAILED").timer().count()).isEqualTo(1);

            // nessuna esecuzione rimasta attiva
            assertThat(registry.get(BatchMetricsRecorder.METRICA_JOB_ATTIVO)
                    .tag(BatchMetricsRecorder.TAG_JOB_NAME, JOB).longTaskTimer().activeTasks()).isZero();

            // due tick schedulati, entrambi liberi
            assertThat(registry.get(BatchMetricsRecorder.METRICA_TICK)
                    .tags(BatchMetricsRecorder.TAG_JOB_NAME, JOB, BatchMetricsRecorder.TAG_TRIGGER, "SCHEDULED",
                            BatchMetricsRecorder.TAG_ESITO_TICK, "CAN_PROCEED")
                    .counter().count()).isEqualTo(2.0);

            // ultimi esiti
            assertThat(registry.get(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO)
                    .tag(BatchMetricsRecorder.TAG_JOB_NAME, JOB).gauge().value()).isPositive();
            assertThat(registry.get(BatchMetricsRecorder.METRICA_ULTIMO_FALLIMENTO)
                    .tag(BatchMetricsRecorder.TAG_JOB_NAME, JOB).gauge().value()).isPositive();

            // configurazione: intervallo da @Scheduled e soglia di stale
            assertThat(registry.get(BatchMetricsRecorder.METRICA_INTERVALLO)
                    .tags(BatchMetricsRecorder.TAG_JOB_NAME, JOB, BatchMetricsRecorder.TAG_TIPO, "fixedDelay")
                    .gauge().value()).isEqualTo(1800.0);
            assertThat(registry.get(BatchMetricsRecorder.METRICA_SOGLIA_STALE)
                    .tag(BatchMetricsAutoConfiguration.TAG_NODO, NODO).gauge().value()).isEqualTo(5400.0);
        });
    }

    @Test
    void dopoUnRiavvioLUltimoSuccessoSiRileggeDalDatabase() {
        runner().run(context -> assertThat(context.getBean(RunnerMetriche.class).esegui().getStatus())
                .isEqualTo(BatchStatus.COMPLETED));

        // Nuovo contesto sullo stesso database, nessuna esecuzione
        runner().run(context -> {
            MeterRegistry registry = context.getBean(MeterRegistry.class);
            assertThat(registry.get(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO)
                    .tag(BatchMetricsRecorder.TAG_JOB_NAME, JOB).gauge().value()).isPositive();
            assertThat(registry.find(BatchMetricsRecorder.METRICA_ULTIMO_FALLIMENTO).gauge()).isNull();
            assertThat(registry.find(BatchMetricsRecorder.METRICA_JOB).timer()).isNull();
        });
    }

    @Test
    void spenteNonToccanoIJob() {
        runner().withPropertyValues("govpay.metrics.batch.enabled=false").run(context -> {
            assertThat(context.getBean(RunnerMetriche.class).esegui().getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(context.getBean(MeterRegistry.class).find(BatchMetricsRecorder.METRICA_JOB).timer()).isNull();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ContestoBatch {

        @Bean
        BatchJobProperties batchJobProperties() {
            BatchJobProperties properties = new BatchJobProperties();
            properties.setClusterId(NODO);
            properties.setStaleThresholdMinutes(90);
            return properties;
        }

        @Bean
        Job jobMetriche(JobRepository jobRepository, DataSource dataSource) {
            return new JobBuilder(JOB, jobRepository)
                    .start(new StepBuilder("passo", jobRepository)
                            .tasklet((contribution, chunkContext) -> {
                                if (FALLISCI.get()) {
                                    throw new IllegalStateException("errore simulato");
                                }
                                return RepeatStatus.FINISHED;
                            }, new JdbcTransactionManager(dataSource))
                            .build())
                    .build();
        }

        @Bean
        JobConcurrencyService jobConcurrencyService(JobRepository jobRepository, BatchJobProperties properties) {
            return BatchCommonAutoConfiguration.createJobConcurrencyService(jobRepository, properties);
        }

        @Bean
        JobExecutionHelper jobExecutionHelper(JobOperator jobOperator, JobConcurrencyService service,
                BatchJobProperties properties) {
            return BatchCommonAutoConfiguration.createJobExecutionHelper(jobOperator, service,
                    properties.getClusterId(), ZoneId.of("Europe/Rome"));
        }

        @Bean
        RunnerMetriche runnerMetriche(JobExecutionHelper helper, Job job) {
            return new RunnerMetriche(helper, job);
        }
    }

    static class RunnerMetriche extends AbstractScheduledJobRunner {

        RunnerMetriche(JobExecutionHelper helper, Job job) {
            super(helper, job, JOB);
        }

        // Lo scheduling non e' attivo nel test: il metodo si invoca a mano
        @Scheduled(fixedDelayString = "${scheduler.metriche.fixedDelay:7200000}")
        public JobExecution esegui() throws Exception {
            return executeScheduledJob();
        }
    }
}
