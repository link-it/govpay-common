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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;

import jakarta.persistence.EntityManager;

import it.govpay.common.batch.dto.BatchInfo;
import it.govpay.common.batch.dto.BatchStatusInfo;
import it.govpay.common.batch.dto.LastExecutionInfo;
import it.govpay.common.batch.dto.NextExecutionInfo;
import it.govpay.common.batch.dto.Problem;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;

@ExtendWith(MockitoExtension.class)
class AbstractBatchControllerTest {

    @Mock
    private JobExecutionHelper jobExecutionHelper;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private Environment environment;

    @Mock
    private JobConcurrencyService concurrencyService;

    @Mock
    private Job job;

    @Mock
    private EntityManager entityManager;

    private static final String JOB_NAME = "testJob";
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Rome");
    private static final long SCHEDULER_INTERVAL = 600000L;

    private TestBatchController controller;

    static class TestBatchController extends AbstractBatchController {
        private final Job job;
        private final String jobName;

        TestBatchController(JobExecutionHelper helper, JobRepository jobRepository,
                           Environment env, ZoneId zoneId, long interval, EntityManager entityManager, Job job, String jobName) {
            super(helper, jobRepository, env, zoneId, interval, entityManager);
            this.job = job;
            this.jobName = jobName;
        }

        @Override
        protected Job getJob() { return job; }

        @Override
        protected String getJobName() { return jobName; }

        @Override
        protected String getDisplayName() { return "Test Batch"; }

        @Override
        protected String getDescription() { return "Batch di test"; }

        @Override
        protected ResponseEntity<String> clearCache() { return ResponseEntity.ok("Cache svuotata"); }

        // Expose protected methods for testing
        public ResponseEntity<Object> testEseguiJob(boolean force) { return eseguiJob(force); }
        public ResponseEntity<BatchStatusInfo> testGetStatus() { return getStatus(); }
        public ResponseEntity<LastExecutionInfo> testGetLastExecution() { return getLastExecution(); }
        public ResponseEntity<NextExecutionInfo> testGetNextExecution() { return getNextExecution(); }
        public String testFormatInterval(long millis) { return formatInterval(millis); }
    }

    @BeforeEach
    void setUp() {
        controller = new TestBatchController(jobExecutionHelper, jobRepository, environment,
                ZONE_ID, SCHEDULER_INTERVAL, entityManager, job, JOB_NAME);
        lenient().when(jobExecutionHelper.getJobConcurrencyService()).thenReturn(concurrencyService);
    }

    @Nested
    @DisplayName("info")
    class Info {

        @Test
        @DisplayName("Restituisce jobName/displayName/description definiti dalla sottoclasse")
        void returnsBatchInfo() {
            ResponseEntity<BatchInfo> response = controller.info();

            assertEquals(200, response.getStatusCode().value());
            BatchInfo info = response.getBody();
            assertEquals(JOB_NAME, info.getJobName());
            assertEquals("Test Batch", info.getDisplayName());
            assertEquals("Batch di test", info.getDescription());
        }
    }

    @Nested
    @DisplayName("eseguiJob")
    class EseguiJob {

        @Test
        @DisplayName("Nessun job in esecuzione - 202 Accepted")
        void noRunningJob() {
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);

            ResponseEntity<Object> response = controller.testEseguiJob(false);

            assertEquals(202, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Job in esecuzione - 409 Conflict")
        void jobAlreadyRunning() {
            JobExecution runningExec = mock(JobExecution.class);
            when(runningExec.getId()).thenReturn(42L);
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(runningExec);
            when(concurrencyService.isJobExecutionStale(runningExec)).thenReturn(false);
            when(concurrencyService.getClusterIdFromExecution(runningExec)).thenReturn("cluster-1");

            ResponseEntity<Object> response = controller.testEseguiJob(false);

            assertEquals(409, response.getStatusCode().value());
            assertInstanceOf(Problem.class, response.getBody());
        }

        @Test
        @DisplayName("Force=true con successo - 202 Accepted")
        void forceSuccess() {
            JobExecution runningExec = mock(JobExecution.class);
            when(runningExec.getId()).thenReturn(42L);
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(runningExec);
            when(concurrencyService.forceAbandonJobExecution(eq(runningExec), anyString())).thenReturn(true);

            ResponseEntity<Object> response = controller.testEseguiJob(true);

            assertEquals(202, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Force=true con fallimento - 503")
        void forceFailed() {
            JobExecution runningExec = mock(JobExecution.class);
            when(runningExec.getId()).thenReturn(42L);
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(runningExec);
            when(concurrencyService.forceAbandonJobExecution(eq(runningExec), anyString())).thenReturn(false);

            ResponseEntity<Object> response = controller.testEseguiJob(true);

            assertEquals(503, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Job stale con successo - 202 Accepted")
        void staleJobSuccess() {
            JobExecution staleExec = mock(JobExecution.class);
            when(staleExec.getId()).thenReturn(42L);
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(staleExec);
            when(concurrencyService.isJobExecutionStale(staleExec)).thenReturn(true);
            when(concurrencyService.abandonStaleJobExecution(staleExec)).thenReturn(true);

            ResponseEntity<Object> response = controller.testEseguiJob(false);

            assertEquals(202, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Job stale con fallimento - 503")
        void staleJobFailed() {
            JobExecution staleExec = mock(JobExecution.class);
            when(staleExec.getId()).thenReturn(42L);
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(staleExec);
            when(concurrencyService.isJobExecutionStale(staleExec)).thenReturn(true);
            when(concurrencyService.abandonStaleJobExecution(staleExec)).thenReturn(false);

            ResponseEntity<Object> response = controller.testEseguiJob(false);

            assertEquals(503, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Eccezione generica - 500")
        void genericException() {
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME))
                    .thenThrow(new RuntimeException("errore imprevisto"));

            ResponseEntity<Object> response = controller.testEseguiJob(false);

            assertEquals(500, response.getStatusCode().value());
            assertInstanceOf(Problem.class, response.getBody());
        }
    }

    @Nested
    @DisplayName("stopExecution")
    class StopExecution {

        @Test
        @DisplayName("Stop accettato - 202")
        void accepted() throws Exception {
            when(jobExecutionHelper.stopExecution(42L)).thenReturn(true);

            ResponseEntity<Object> response = controller.stopExecution(42L);

            assertEquals(202, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Esecuzione non in corso - 409")
        void notRunning() throws Exception {
            when(jobExecutionHelper.stopExecution(42L)).thenThrow(new JobExecutionNotRunningException("non in corso"));

            ResponseEntity<Object> response = controller.stopExecution(42L);

            assertEquals(409, response.getStatusCode().value());
            assertInstanceOf(Problem.class, response.getBody());
        }

        @Test
        @DisplayName("Esecuzione inesistente - 404")
        void notFound() throws Exception {
            when(jobExecutionHelper.stopExecution(99L)).thenThrow(new NoSuchJobExecutionException("non trovata"));

            ResponseEntity<Object> response = controller.stopExecution(99L);

            assertEquals(404, response.getStatusCode().value());
            assertInstanceOf(Problem.class, response.getBody());
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("Nessun job in esecuzione")
        void noRunningJob() {
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);

            ResponseEntity<BatchStatusInfo> response = controller.testGetStatus();

            assertEquals(200, response.getStatusCode().value());
            assertFalse(response.getBody().isRunning());
        }

        @Test
        @DisplayName("Job in esecuzione con startTime e step corrente")
        void runningJobWithDetails() {
            JobExecution exec = mock(JobExecution.class);
            when(exec.getId()).thenReturn(42L);
            LocalDateTime startTime = LocalDateTime.now(ZONE_ID).minusMinutes(5);
            when(exec.getStartTime()).thenReturn(startTime);
            when(exec.getStatus()).thenReturn(BatchStatus.STARTED);

            StepExecution stepExec = mock(StepExecution.class);
            when(stepExec.getStatus()).thenReturn(BatchStatus.STARTED);
            when(stepExec.getStepName()).thenReturn("processStep");
            Collection<StepExecution> steps = List.of(stepExec);
            when(exec.getStepExecutions()).thenReturn(steps);

            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(exec);
            when(concurrencyService.getClusterIdFromExecution(exec)).thenReturn("cluster-1");

            ResponseEntity<BatchStatusInfo> response = controller.testGetStatus();

            assertEquals(200, response.getStatusCode().value());
            BatchStatusInfo info = response.getBody();
            assertTrue(info.isRunning());
            assertEquals(42L, info.getExecutionId());
            assertEquals("cluster-1", info.getClusterId());
            assertNotNull(info.getRunningSeconds());
            assertEquals("STARTED", info.getStatus());
            assertEquals("processStep", info.getCurrentStep());
        }

        @Test
        @DisplayName("Job in esecuzione senza startTime")
        void runningJobWithoutStartTime() {
            JobExecution exec = mock(JobExecution.class);
            when(exec.getId()).thenReturn(42L);
            when(exec.getStartTime()).thenReturn(null);
            when(exec.getStatus()).thenReturn(BatchStatus.STARTING);

            Collection<StepExecution> emptySteps = Collections.emptyList();
            when(exec.getStepExecutions()).thenReturn(emptySteps);

            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(exec);
            when(concurrencyService.getClusterIdFromExecution(exec)).thenReturn("cluster-1");

            ResponseEntity<BatchStatusInfo> response = controller.testGetStatus();

            BatchStatusInfo info = response.getBody();
            assertTrue(info.isRunning());
            assertNull(info.getRunningSeconds());
        }
    }

    @Nested
    @DisplayName("getLastExecution")
    class GetLastExecution {

        @Test
        @DisplayName("Nessuna esecuzione completata")
        void noCompletedExecution() {
            when(jobRepository.getJobInstances(JOB_NAME, 0, 10)).thenReturn(Collections.emptyList());

            ResponseEntity<LastExecutionInfo> response = controller.testGetLastExecution();

            assertEquals(200, response.getStatusCode().value());
            assertNull(response.getBody().getExecutionId());
        }

        @Test
        @DisplayName("Esecuzione completata con tutti i campi")
        void completedExecution() {
            JobInstance instance = mock(JobInstance.class);
            JobExecution exec = mock(JobExecution.class);
            LocalDateTime start = LocalDateTime.of(2025, 3, 12, 10, 0);
            LocalDateTime end = LocalDateTime.of(2025, 3, 12, 10, 5);

            when(exec.getId()).thenReturn(1L);
            when(exec.getStatus()).thenReturn(BatchStatus.COMPLETED);
            when(exec.getStartTime()).thenReturn(start);
            when(exec.getEndTime()).thenReturn(end);
            when(exec.getExitStatus()).thenReturn(new ExitStatus("COMPLETED", "OK"));
            when(exec.getJobParameters()).thenReturn(
                    new JobParametersBuilder().addString(JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE, "MANUAL").toJobParameters());

            when(jobRepository.getJobInstances(JOB_NAME, 0, 10)).thenReturn(List.of(instance));
            when(jobRepository.getJobExecutions(instance)).thenReturn(List.of(exec));
            when(concurrencyService.getClusterIdFromExecution(exec)).thenReturn("cluster-1");

            ResponseEntity<LastExecutionInfo> response = controller.testGetLastExecution();

            LastExecutionInfo info = response.getBody();
            assertEquals(1L, info.getExecutionId());
            assertEquals("cluster-1", info.getClusterId());
            assertEquals(start, info.getStartTime());
            assertEquals(end, info.getEndTime());
            assertEquals(300L, info.getDurationSeconds());
            assertEquals("COMPLETED", info.getStatus());
            assertEquals("COMPLETED", info.getExitCode());
            assertEquals("OK", info.getExitDescription());
            assertEquals("MANUAL", info.getTriggerType());
        }

        @Test
        @DisplayName("Esecuzione a cavallo del ritorno all'ora solare - durata reale")
        void executionAcrossDstTransition() {
            JobInstance instance = mock(JobInstance.class);
            JobExecution exec = mock(JobExecution.class);
            // 25/10/2026: alle 03:00 CEST le lancette tornano alle 02:00 CET.
            // Tra i due istanti locali sono passate quattro ore reali, non tre.
            LocalDateTime start = LocalDateTime.of(2026, 10, 25, 1, 30);
            LocalDateTime end = LocalDateTime.of(2026, 10, 25, 4, 30);

            when(exec.getId()).thenReturn(2L);
            when(exec.getStatus()).thenReturn(BatchStatus.COMPLETED);
            when(exec.getStartTime()).thenReturn(start);
            when(exec.getEndTime()).thenReturn(end);
            when(exec.getExitStatus()).thenReturn(new ExitStatus("COMPLETED", "OK"));
            when(exec.getJobParameters()).thenReturn(new JobParametersBuilder().toJobParameters());

            when(jobRepository.getJobInstances(JOB_NAME, 0, 10)).thenReturn(List.of(instance));
            when(jobRepository.getJobExecutions(instance)).thenReturn(List.of(exec));

            ResponseEntity<LastExecutionInfo> response = controller.testGetLastExecution();

            assertEquals(4 * 3600L, response.getBody().getDurationSeconds());
        }

        @Test
        @DisplayName("Exit description troncata a 500 caratteri")
        void truncatedExitDescription() {
            JobInstance instance = mock(JobInstance.class);
            JobExecution exec = mock(JobExecution.class);
            String longDescription = "X".repeat(600);

            when(exec.getId()).thenReturn(1L);
            when(exec.getStatus()).thenReturn(BatchStatus.FAILED);
            when(exec.getStartTime()).thenReturn(LocalDateTime.now());
            when(exec.getEndTime()).thenReturn(LocalDateTime.now());
            when(exec.getExitStatus()).thenReturn(new ExitStatus("FAILED", longDescription));
            when(exec.getJobParameters()).thenReturn(new JobParametersBuilder().toJobParameters());

            when(jobRepository.getJobInstances(JOB_NAME, 0, 10)).thenReturn(List.of(instance));
            when(jobRepository.getJobExecutions(instance)).thenReturn(List.of(exec));
            when(concurrencyService.getClusterIdFromExecution(exec)).thenReturn("cluster-1");

            ResponseEntity<LastExecutionInfo> response = controller.testGetLastExecution();

            String exitDesc = response.getBody().getExitDescription();
            assertEquals(503, exitDesc.length()); // 500 + "..."
            assertTrue(exitDesc.endsWith("..."));
        }

        @Test
        @DisplayName("startTime/endTime null - durationSeconds null")
        void nullStartEndTime() {
            JobInstance instance = mock(JobInstance.class);
            JobExecution exec = mock(JobExecution.class);

            when(exec.getId()).thenReturn(1L);
            when(exec.getStatus()).thenReturn(BatchStatus.COMPLETED);
            when(exec.getStartTime()).thenReturn(null);
            when(exec.getEndTime()).thenReturn(null);
            when(exec.getExitStatus()).thenReturn(new ExitStatus("COMPLETED", ""));
            when(exec.getJobParameters()).thenReturn(new JobParametersBuilder().toJobParameters());

            when(jobRepository.getJobInstances(JOB_NAME, 0, 10)).thenReturn(List.of(instance));
            when(jobRepository.getJobExecutions(instance)).thenReturn(List.of(exec));
            when(concurrencyService.getClusterIdFromExecution(exec)).thenReturn("cluster-1");

            ResponseEntity<LastExecutionInfo> response = controller.testGetLastExecution();

            assertNull(response.getBody().getDurationSeconds());
        }
    }

    @Nested
    @DisplayName("getNextExecution")
    class GetNextExecution {

        @Test
        @DisplayName("Modalita' cron")
        void cronMode() {
            when(environment.matchesProfiles("cron")).thenReturn(true);

            ResponseEntity<NextExecutionInfo> response = controller.testGetNextExecution();

            assertEquals(200, response.getStatusCode().value());
            assertEquals("cron", response.getBody().getSchedulingMode());
            assertNotNull(response.getBody().getMessage());
        }

        @Test
        @DisplayName("Modalita' scheduler senza esecuzioni precedenti")
        void schedulerNoPreviousExecutions() {
            when(environment.matchesProfiles("cron")).thenReturn(false);
            when(jobRepository.getJobInstances(JOB_NAME, 0, 5)).thenReturn(Collections.emptyList());
            // nextExecutionTime = now, which is NOT before now, so getCurrentRunningJobExecution not called

            ResponseEntity<NextExecutionInfo> response = controller.testGetNextExecution();

            NextExecutionInfo info = response.getBody();
            assertEquals("scheduler", info.getSchedulingMode());
            assertNotNull(info.getNextExecutionTime());
            assertEquals(SCHEDULER_INTERVAL, info.getIntervalMillis());
            assertNotNull(info.getIntervalFormatted());
        }

        @Test
        @DisplayName("Modalita' scheduler con esecuzione precedente")
        void schedulerWithPreviousExecution() {
            when(environment.matchesProfiles("cron")).thenReturn(false);

            JobInstance instance = mock(JobInstance.class);
            JobExecution exec = mock(JobExecution.class);
            LocalDateTime endTime = LocalDateTime.now(ZONE_ID).minusMinutes(1);
            when(exec.getEndTime()).thenReturn(endTime);

            when(jobRepository.getJobInstances(JOB_NAME, 0, 5)).thenReturn(List.of(instance));
            when(jobRepository.getJobExecutions(instance)).thenReturn(List.of(exec));
            // nextExecutionTime = endTime + 10min = 9 min in future, NOT before now

            ResponseEntity<NextExecutionInfo> response = controller.testGetNextExecution();

            NextExecutionInfo info = response.getBody();
            assertEquals("scheduler", info.getSchedulingMode());
            assertNotNull(info.getLastCompletedTime());
        }

        @Test
        @DisplayName("Modalita' scheduler con job in esecuzione")
        void schedulerWithRunningJob() {
            when(environment.matchesProfiles("cron")).thenReturn(false);

            JobInstance instance = mock(JobInstance.class);
            JobExecution exec = mock(JobExecution.class);
            // endTime lontano nel passato per forzare nextExecutionTime.isBefore(now)
            LocalDateTime endTime = LocalDateTime.now(ZONE_ID).minusHours(2);
            when(exec.getEndTime()).thenReturn(endTime);

            when(jobRepository.getJobInstances(JOB_NAME, 0, 5)).thenReturn(List.of(instance));
            when(jobRepository.getJobExecutions(instance)).thenReturn(List.of(exec));

            JobExecution runningExec = mock(JobExecution.class);
            when(concurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(runningExec);

            ResponseEntity<NextExecutionInfo> response = controller.testGetNextExecution();

            NextExecutionInfo info = response.getBody();
            assertEquals("scheduler", info.getSchedulingMode());
            assertNull(info.getNextExecutionTime());
        }
    }

    @Nested
    @DisplayName("formatInterval")
    class FormatInterval {

        @Test
        @DisplayName("Secondi")
        void seconds() {
            assertEquals("30 secondi", controller.testFormatInterval(30000));
        }

        @Test
        @DisplayName("Minuti")
        void minutes() {
            assertEquals("10 minuti", controller.testFormatInterval(600000));
        }

        @Test
        @DisplayName("Ore")
        void hours() {
            assertEquals("2 ore", controller.testFormatInterval(7200000));
        }

        @Test
        @DisplayName("Ore e minuti")
        void hoursAndMinutes() {
            assertEquals("1 ore 30 minuti", controller.testFormatInterval(5400000));
        }
    }
}
