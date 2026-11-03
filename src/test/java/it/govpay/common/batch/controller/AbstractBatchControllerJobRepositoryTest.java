package it.govpay.common.batch.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.function.BooleanSupplier;
import java.time.ZoneId;
import java.util.List;

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
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;

import it.govpay.common.batch.config.BatchCommonAutoConfiguration;
import it.govpay.common.batch.config.BatchJobProperties;
import it.govpay.common.batch.config.BatchJobRepositoryAutoConfiguration;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;
import jakarta.persistence.EntityManager;

/**
 * Controller dei batch su un JobRepository JDBC reale (H2), come nei progetti
 * batch: avvii ravvicinati e id di esecuzione sconosciuti.
 */
class AbstractBatchControllerJobRepositoryTest {

    private static final String JOB = "jobControllerJdbc";

    private EmbeddedDatabase database;

    @BeforeEach
    void setUp() {
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

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(BatchJobRepositoryAutoConfiguration.class))
                .withBean(DataSource.class, () -> database)
                .withUserConfiguration(Ctx.class);
    }

    @Test
    void dueRichiesteRavvicinateAvvianoUnaSolaEsecuzione() {
        runner().run(ctx -> {
            JobRepository repo = ctx.getBean(JobRepository.class);
            AbstractBatchControllerTest.TestBatchController controller = new AbstractBatchControllerTest.TestBatchController(
                    ctx.getBean(JobExecutionHelper.class), repo, mock(Environment.class), ZoneId.of("Europe/Rome"),
                    600_000L, mock(EntityManager.class), ctx.getBean(Job.class), JOB);

            assertThat(controller.testEseguiJob(false).getStatusCode().value()).isEqualTo(202);
            assertThat(controller.testEseguiJob(false).getStatusCode().value()).isEqualTo(409);

            attendi(() -> esecuzioni(repo).stream().anyMatch(e -> e.getStatus() == BatchStatus.COMPLETED));
            assertThat(esecuzioni(repo)).hasSize(1);

            // a esecuzione finita la prenotazione e' rilasciata e il job riparte
            attendi(() -> controller.testEseguiJob(false).getStatusCode().value() == 202);
            attendi(() -> esecuzioni(repo).size() == 2 && esecuzioni(repo).stream().noneMatch(JobExecution::isRunning));
        });
    }

    @Test
    void deleteDiUnEsecuzioneInesistente() {
        runner().run(ctx -> {
            AbstractBatchControllerTest.TestBatchController controller = new AbstractBatchControllerTest.TestBatchController(
                    ctx.getBean(JobExecutionHelper.class), ctx.getBean(JobRepository.class), mock(Environment.class),
                    ZoneId.of("Europe/Rome"), 600_000L, mock(EntityManager.class), ctx.getBean(Job.class), JOB);

            assertThat(controller.stopExecution(987_654L).getStatusCode().value()).isEqualTo(404);
        });
    }

    private static void attendi(BooleanSupplier condizione) throws InterruptedException {
        long limite = System.currentTimeMillis() + 10_000;
        while (!condizione.getAsBoolean()) {
            assertThat(System.currentTimeMillis()).as("condizione non raggiunta entro 10 secondi").isLessThan(limite);
            Thread.sleep(100);
        }
    }

    private static List<JobExecution> esecuzioni(JobRepository repo) {
        return repo.getJobInstances(JOB, 0, 10).stream().flatMap(i -> repo.getJobExecutions(i).stream()).toList();
    }

    @Configuration(proxyBeanMethods = false)
    static class Ctx {

        @Bean
        BatchJobProperties batchJobProperties() {
            BatchJobProperties properties = new BatchJobProperties();
            properties.setClusterId("nodo-1");
            properties.setStaleThresholdMinutes(90);
            return properties;
        }

        /** Un passo che dura abbastanza da sovrapporsi alla seconda richiesta */
        @Bean
        Job job(JobRepository jobRepository, DataSource dataSource) {
            return new JobBuilder(JOB, jobRepository)
                    .start(new StepBuilder("passo", jobRepository)
                            .tasklet((contribution, chunkContext) -> {
                                Thread.sleep(1500);
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
    }
}
