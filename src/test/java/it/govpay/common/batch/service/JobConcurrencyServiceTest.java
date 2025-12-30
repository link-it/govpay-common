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
package it.govpay.common.batch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobConcurrencyServiceTest {

    @Mock
    private JobExplorer jobExplorer;

    @Mock
    private JobRepository jobRepository;

    private JobConcurrencyService service;

    private static final String JOB_NAME = "testJob";
    private static final int STALE_THRESHOLD_MINUTES = 120;

    @BeforeEach
    void setUp() {
        service = new JobConcurrencyService(jobExplorer, jobRepository, STALE_THRESHOLD_MINUTES);
    }

    @Test
    @DisplayName("getCurrentRunningJobExecution - nessun job in esecuzione")
    void getCurrentRunningJobExecution_noRunningJobs() {
        when(jobExplorer.findRunningJobExecutions(JOB_NAME)).thenReturn(Collections.emptySet());

        JobExecution result = service.getCurrentRunningJobExecution(JOB_NAME);

        assertNull(result);
        verify(jobExplorer).findRunningJobExecutions(JOB_NAME);
    }

    @Test
    @DisplayName("getCurrentRunningJobExecution - job in esecuzione")
    void getCurrentRunningJobExecution_withRunningJob() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(jobExplorer.findRunningJobExecutions(JOB_NAME)).thenReturn(Set.of(execution));

        JobExecution result = service.getCurrentRunningJobExecution(JOB_NAME);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(jobExplorer).findRunningJobExecutions(JOB_NAME);
    }

    @Test
    @DisplayName("isJobExecutionStale - null execution")
    void isJobExecutionStale_nullExecution() {
        assertFalse(service.isJobExecutionStale(null));
    }

    @Test
    @DisplayName("isJobExecutionStale - stato UNKNOWN")
    void isJobExecutionStale_unknownStatus() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.UNKNOWN);

        assertTrue(service.isJobExecutionStale(execution));
    }

    @Test
    @DisplayName("isJobExecutionStale - stato ABANDONED")
    void isJobExecutionStale_abandonedStatus() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.ABANDONED);

        assertTrue(service.isJobExecutionStale(execution));
    }

    @Test
    @DisplayName("isJobExecutionStale - job started recentemente")
    void isJobExecutionStale_recentlyStarted() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(execution.getLastUpdated()).thenReturn(LocalDateTime.now().minusMinutes(10));

        assertFalse(service.isJobExecutionStale(execution));
    }

    @Test
    @DisplayName("isJobExecutionStale - job stale per timeout")
    void isJobExecutionStale_staleByTimeout() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(execution.getLastUpdated()).thenReturn(LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES + 10));

        assertTrue(service.isJobExecutionStale(execution));
    }

    @Test
    @DisplayName("abandonStaleJobExecution - null execution")
    void abandonStaleJobExecution_nullExecution() {
        assertFalse(service.abandonStaleJobExecution(null));
    }

    @Test
    @DisplayName("abandonStaleJobExecution - successo")
    void abandonStaleJobExecution_success() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(execution.getStepExecutions()).thenReturn(Collections.emptyList());

        boolean result = service.abandonStaleJobExecution(execution);

        assertTrue(result);
        verify(execution).setStatus(BatchStatus.FAILED);
        verify(execution).setEndTime(any(LocalDateTime.class));
        verify(jobRepository).update(execution);
    }

    @Test
    @DisplayName("forceAbandonJobExecution - null execution")
    void forceAbandonJobExecution_nullExecution() {
        assertFalse(service.forceAbandonJobExecution(null, "test reason"));
    }

    @Test
    @DisplayName("forceAbandonJobExecution - successo")
    void forceAbandonJobExecution_success() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(execution.getStepExecutions()).thenReturn(Collections.emptyList());

        boolean result = service.forceAbandonJobExecution(execution, "Test force abandon");

        assertTrue(result);
        verify(execution).setStatus(BatchStatus.ABANDONED);
        verify(execution).setEndTime(any(LocalDateTime.class));
        verify(jobRepository).update(execution);
    }

    @Test
    @DisplayName("getClusterIdFromExecution - null execution")
    void getClusterIdFromExecution_nullExecution() {
        assertNull(service.getClusterIdFromExecution(null));
    }

    @Test
    @DisplayName("getClusterIdFromExecution - con cluster ID")
    void getClusterIdFromExecution_withClusterId() {
        JobExecution execution = mock(JobExecution.class);
        JobParameters params = mock(JobParameters.class);
        JobParameter<?> clusterParam = mock(JobParameter.class);

        when(execution.getJobParameters()).thenReturn(params);
        when(params.getParameters()).thenReturn(Map.of(JobConcurrencyService.JOB_PARAM_CLUSTER_ID, clusterParam));
        when(clusterParam.getValue()).thenReturn("test-cluster");

        String result = service.getClusterIdFromExecution(execution);

        assertEquals("test-cluster", result);
    }

    @Test
    @DisplayName("getClusterIdFromExecution - senza cluster ID")
    void getClusterIdFromExecution_withoutClusterId() {
        JobExecution execution = mock(JobExecution.class);
        JobParameters params = mock(JobParameters.class);

        when(execution.getJobParameters()).thenReturn(params);
        when(params.getParameters()).thenReturn(Collections.emptyMap());

        String result = service.getClusterIdFromExecution(execution);

        assertNull(result);
    }

    private JobExecution createMockJobExecution(Long id, BatchStatus status) {
        JobExecution execution = mock(JobExecution.class);
        JobInstance jobInstance = mock(JobInstance.class);

        when(execution.getId()).thenReturn(id);
        when(execution.getStatus()).thenReturn(status);
        when(execution.getJobInstance()).thenReturn(jobInstance);
        when(jobInstance.getJobName()).thenReturn(JOB_NAME);

        return execution;
    }
}
