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

import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionCheckResult;
import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionResult;

@ExtendWith(MockitoExtension.class)
class AbstractScheduledJobRunnerTest {

    @Mock
    private JobExecutionHelper jobExecutionHelper;

    @Mock
    private Job job;

    private static final String JOB_NAME = "testScheduledJob";

    private TestScheduledRunner runner;
    private boolean onJobCompletedCalled;
    private JobExecution completedExecution;

    class TestScheduledRunner extends AbstractScheduledJobRunner {
        TestScheduledRunner(JobExecutionHelper helper, Job job, String jobName) {
            super(helper, job, jobName);
        }

        @Override
        protected void onJobCompleted(JobExecution execution) {
            onJobCompletedCalled = true;
            completedExecution = execution;
        }
    }

    @BeforeEach
    void setUp() {
        runner = new TestScheduledRunner(jobExecutionHelper, job, JOB_NAME);
        onJobCompletedCalled = false;
        completedExecution = null;
    }

    @Test
    @DisplayName("CAN_PROCEED - esegue e ritorna execution")
    void canProceed() throws Exception {
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.CAN_PROCEED, null, null));

        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecutionHelper.runJob(eq(job), eq(JOB_NAME))).thenReturn(execution);

        JobExecution result = runner.executeScheduledJob();

        assertNotNull(result);
        assertSame(execution, result);
        assertTrue(onJobCompletedCalled);
        assertSame(execution, completedExecution);
    }

    @Test
    @DisplayName("STALE_ABANDONED_CAN_PROCEED - esegue e ritorna execution")
    void staleAbandoned() throws Exception {
        JobExecution staleExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.STALE_ABANDONED_CAN_PROCEED, staleExec, null));

        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(jobExecutionHelper.runJob(eq(job), eq(JOB_NAME))).thenReturn(execution);

        JobExecution result = runner.executeScheduledJob();

        assertNotNull(result);
        assertTrue(onJobCompletedCalled);
    }

    @Test
    @DisplayName("STALE_ABANDON_FAILED - ritorna null")
    void staleAbandonFailed() throws Exception {
        JobExecution staleExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.STALE_ABANDON_FAILED, staleExec, null));

        JobExecution result = runner.executeScheduledJob();

        assertNull(result);
        assertFalse(onJobCompletedCalled);
    }

    @Test
    @DisplayName("RUNNING_ON_OTHER_NODE - ritorna null")
    void runningOnOtherNode() throws Exception {
        JobExecution runningExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.RUNNING_ON_OTHER_NODE, runningExec, "other-cluster"));

        JobExecution result = runner.executeScheduledJob();

        assertNull(result);
        assertFalse(onJobCompletedCalled);
    }

    @Test
    @DisplayName("RUNNING_ON_THIS_NODE - ritorna null")
    void runningOnThisNode() throws Exception {
        JobExecution runningExec = mock(JobExecution.class);
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.RUNNING_ON_THIS_NODE, runningExec, "my-cluster"));

        JobExecution result = runner.executeScheduledJob();

        assertNull(result);
        assertFalse(onJobCompletedCalled);
    }

    @Test
    @DisplayName("runJob ritorna null - non chiama onJobCompleted")
    void runJobReturnsNull() throws Exception {
        when(jobExecutionHelper.checkBeforeExecution(JOB_NAME))
                .thenReturn(new PreExecutionResult(PreExecutionCheckResult.CAN_PROCEED, null, null));
        when(jobExecutionHelper.runJob(eq(job), eq(JOB_NAME))).thenReturn(null);

        JobExecution result = runner.executeScheduledJob();

        assertNull(result);
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
