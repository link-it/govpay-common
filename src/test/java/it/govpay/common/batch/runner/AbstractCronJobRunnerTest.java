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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;

import it.govpay.common.batch.TriggerType;
import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionCheckResult;
import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionResult;

@ExtendWith(MockitoExtension.class)
class AbstractCronJobRunnerTest {

    @Mock
    private JobExecutionHelper jobExecutionHelper;

    @Mock
    private Job job;

    private static final String JOB_NAME = "testCronJob";

    private TestCronRunner runner;
    private int exitCode = -1;
    private boolean onJobCompletedCalled;
    private JobExecution completedExecution;

    class TestCronRunner extends AbstractCronJobRunner {
        TestCronRunner(JobExecutionHelper helper, Job job, String jobName) {
            super(helper, job, jobName);
        }

        @Override
        protected void exitApplication(int code) {
            exitCode = code;
            // Override to avoid System.exit in tests
        }

        @Override
        protected void onJobCompleted(JobExecution execution) {
            onJobCompletedCalled = true;
            completedExecution = execution;
        }
    }

    @BeforeEach
    void setUp() {
        runner = new TestCronRunner(jobExecutionHelper, job, JOB_NAME);
        exitCode = -1;
        onJobCompletedCalled = false;
        completedExecution = null;
    }

    @Test
    @DisplayName("CAN_PROCEED - esegue il job ed esce con 0")
    void canProceed() throws Exception {
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.CAN_PROCEED, null, null));

        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecutionHelper.runJob(eq(job), eq(JOB_NAME), eq(TriggerType.SCHEDULED))).thenReturn(execution);

        runner.run();

        assertEquals(0, exitCode);
        assertTrue(onJobCompletedCalled);
        assertSame(execution, completedExecution);
    }

    @Test
    @DisplayName("STALE_ABANDONED_CAN_PROCEED - esegue il job")
    void staleAbandoned() throws Exception {
        JobExecution staleExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.STALE_ABANDONED_CAN_PROCEED, staleExec, null));

        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecutionHelper.runJob(eq(job), eq(JOB_NAME), eq(TriggerType.SCHEDULED))).thenReturn(execution);

        runner.run();

        assertEquals(0, exitCode);
        assertTrue(onJobCompletedCalled);
    }

    @Test
    @DisplayName("STALE_ABANDON_FAILED - esce con errore 1")
    void staleAbandonFailed() throws Exception {
        JobExecution staleExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.STALE_ABANDON_FAILED, staleExec, null));

        runner.run();

        assertEquals(1, exitCode);
        assertFalse(onJobCompletedCalled);
    }

    @Test
    @DisplayName("RUNNING_ON_OTHER_NODE - esce con 0")
    void runningOnOtherNode() throws Exception {
        JobExecution runningExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.RUNNING_ON_OTHER_NODE, runningExec, "other-cluster"));

        runner.run();

        assertEquals(0, exitCode);
        assertFalse(onJobCompletedCalled);
    }

    @Test
    @DisplayName("RUNNING_ON_THIS_NODE - esce con 0")
    void runningOnThisNode() throws Exception {
        JobExecution runningExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.RUNNING_ON_THIS_NODE, runningExec, "my-cluster"));

        runner.run();

        assertEquals(0, exitCode);
        assertFalse(onJobCompletedCalled);
    }

    @Test
    @DisplayName("runJob ritorna null - non chiama onJobCompleted")
    void runJobReturnsNull() throws Exception {
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.CAN_PROCEED, null, null));
        when(jobExecutionHelper.runJob(eq(job), eq(JOB_NAME), eq(TriggerType.SCHEDULED))).thenReturn(null);

        runner.run();

        assertEquals(0, exitCode);
        assertFalse(onJobCompletedCalled);
    }

    @Test
    @DisplayName("Accessors restituiscono valori corretti")
    void accessors() {
        assertSame(job, runner.getJob());
        assertEquals(JOB_NAME, runner.getJobName());
        assertSame(jobExecutionHelper, runner.getJobExecutionHelper());
    }
}
