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

import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;

import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionCheckResult;
import it.govpay.common.batch.runner.JobExecutionHelper.PreExecutionResult;
import it.govpay.common.batch.service.JobConcurrencyService;

@ExtendWith(MockitoExtension.class)
class JobExecutionHelperTest {

    @Mock
    private JobOperator jobOperator;

    @Mock
    private JobConcurrencyService jobConcurrencyService;

    @Mock
    private Job job;

    private JobExecutionHelper helper;

    private static final String JOB_NAME = "testJob";
    private static final String CLUSTER_ID = "test-cluster";
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Rome");

    @BeforeEach
    void setUp() {
        helper = new JobExecutionHelper(jobOperator, jobConcurrencyService, CLUSTER_ID, ZONE_ID);
    }

    @Test
    @DisplayName("checkBeforeExecution - nessun job in esecuzione")
    void checkBeforeExecution_noRunningJob() {
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);

        PreExecutionResult result = helper.checkBeforeExecution(JOB_NAME);

        assertEquals(PreExecutionCheckResult.CAN_PROCEED, result.result());
        assertTrue(result.canProceed());
        assertNull(result.currentExecution());
        assertNull(result.runningClusterId());
    }

    @Test
    @DisplayName("checkBeforeExecution - job stale abbandonato con successo")
    void checkBeforeExecution_staleJobAbandoned() {
        JobExecution staleExecution = mock(JobExecution.class);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(staleExecution);
        when(jobConcurrencyService.isJobExecutionStale(staleExecution)).thenReturn(true);
        when(jobConcurrencyService.abandonStaleJobExecution(staleExecution)).thenReturn(true);

        PreExecutionResult result = helper.checkBeforeExecution(JOB_NAME);

        assertEquals(PreExecutionCheckResult.STALE_ABANDONED_CAN_PROCEED, result.result());
        assertTrue(result.canProceed());
        assertNotNull(result.currentExecution());
    }

    @Test
    @DisplayName("checkBeforeExecution - job stale non abbandonabile")
    void checkBeforeExecution_staleJobCannotAbandon() {
        JobExecution staleExecution = mock(JobExecution.class);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(staleExecution);
        when(jobConcurrencyService.isJobExecutionStale(staleExecution)).thenReturn(true);
        when(jobConcurrencyService.abandonStaleJobExecution(staleExecution)).thenReturn(false);

        PreExecutionResult result = helper.checkBeforeExecution(JOB_NAME);

        assertEquals(PreExecutionCheckResult.STALE_ABANDON_FAILED, result.result());
        assertFalse(result.canProceed());
    }

    @Test
    @DisplayName("checkBeforeExecution - job in esecuzione su altro nodo")
    void checkBeforeExecution_runningOnOtherNode() {
        JobExecution runningExecution = mock(JobExecution.class);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(runningExecution);
        when(jobConcurrencyService.isJobExecutionStale(runningExecution)).thenReturn(false);
        when(jobConcurrencyService.getClusterIdFromExecution(runningExecution)).thenReturn("other-cluster");

        PreExecutionResult result = helper.checkBeforeExecution(JOB_NAME);

        assertEquals(PreExecutionCheckResult.RUNNING_ON_OTHER_NODE, result.result());
        assertFalse(result.canProceed());
        assertEquals("other-cluster", result.runningClusterId());
    }

    @Test
    @DisplayName("checkBeforeExecution - job in esecuzione su questo nodo")
    void checkBeforeExecution_runningOnThisNode() {
        JobExecution runningExecution = mock(JobExecution.class);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(runningExecution);
        when(jobConcurrencyService.isJobExecutionStale(runningExecution)).thenReturn(false);
        when(jobConcurrencyService.getClusterIdFromExecution(runningExecution)).thenReturn(CLUSTER_ID);

        PreExecutionResult result = helper.checkBeforeExecution(JOB_NAME);

        assertEquals(PreExecutionCheckResult.RUNNING_ON_THIS_NODE, result.result());
        assertFalse(result.canProceed());
        assertEquals(CLUSTER_ID, result.runningClusterId());
    }

    @Test
    @DisplayName("buildJobParameters - contiene parametri standard")
    void buildJobParameters() {
        JobParameters params = helper.buildJobParameters(JOB_NAME);

        assertNotNull(params.getString(JobExecutionHelper.JOB_PARAM_JOB_ID));
        assertNotNull(params.getString(JobExecutionHelper.JOB_PARAM_WHEN));
        assertNotNull(params.getString(JobExecutionHelper.JOB_PARAM_CLUSTER_ID));
        assertEquals(JOB_NAME, params.getString(JobExecutionHelper.JOB_PARAM_JOB_ID));
        assertEquals(CLUSTER_ID, params.getString(JobExecutionHelper.JOB_PARAM_CLUSTER_ID));
    }

    @Test
    @DisplayName("runJob - esegue il job con successo")
    void runJob() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);

        JobExecution result = helper.runJob(job, JOB_NAME);

        assertNotNull(result);
        verify(jobOperator).start(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("executeIfPossible - esegue quando possibile")
    void executeIfPossible_executes() throws Exception {
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);
        JobExecution execution = mock(JobExecution.class);
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);

        JobExecution result = helper.executeIfPossible(job, JOB_NAME);

        assertNotNull(result);
        verify(jobOperator).start(eq(job), any(JobParameters.class));
    }

    @Test
    @DisplayName("executeIfPossible - non esegue quando non possibile")
    void executeIfPossible_doesNotExecute() throws Exception {
        JobExecution runningExecution = mock(JobExecution.class);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(runningExecution);
        when(jobConcurrencyService.isJobExecutionStale(runningExecution)).thenReturn(false);
        when(jobConcurrencyService.getClusterIdFromExecution(runningExecution)).thenReturn("other-cluster");

        JobExecution result = helper.executeIfPossible(job, JOB_NAME);

        assertNull(result);
        verify(jobOperator, never()).start(any(Job.class), any(JobParameters.class));
    }

    @Test
    @DisplayName("stopExecution - delega a JobOperator.stop(JobExecution) e ritorna true")
    void stopExecution_success() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getId()).thenReturn(42L);
        when(jobOperator.stop(execution)).thenReturn(true);

        boolean result = helper.stopExecution(execution);

        assertTrue(result);
        verify(jobOperator).stop(execution);
    }

    @Test
    @DisplayName("stopExecution - propaga JobExecutionNotRunningException")
    void stopExecution_notRunning() throws Exception {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getId()).thenReturn(42L);
        when(jobOperator.stop(execution)).thenThrow(new JobExecutionNotRunningException("non in corso"));

        assertThrows(JobExecutionNotRunningException.class, () -> helper.stopExecution(execution));
    }

    @Test
    @DisplayName("getClusterId - restituisce cluster ID configurato")
    void getClusterId() {
        assertEquals(CLUSTER_ID, helper.getClusterId());
    }

    @Test
    @DisplayName("getJobConcurrencyService - restituisce il service")
    void getJobConcurrencyService() {
        assertSame(jobConcurrencyService, helper.getJobConcurrencyService());
    }
}
