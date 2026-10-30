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
package it.govpay.common.batch.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.job.parameters.JobParameters;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JobConcurrencyServiceTest {

    @Mock
    private JobRepository jobRepository;

    private JobConcurrencyService service;

    private static final String JOB_NAME = "testJob";
    private static final int STALE_THRESHOLD_MINUTES = 120;

    @BeforeEach
    void setUp() {
        service = new JobConcurrencyService(jobRepository, STALE_THRESHOLD_MINUTES);
    }

    // ===== Abbandono e terminazione forzata: e' il recupero multi-nodo, se si
    // rompe i job bloccati non tornano mai disponibili =====

    private JobExecution esecuzioneConStep(BatchStatus statoStep) {
        JobExecution esecuzione = new JobExecution(1L,
                new JobInstance(1L, JOB_NAME), new JobParameters());
        StepExecution step = new StepExecution("stepDiProva", esecuzione);
        step.setStatus(statoStep);
        esecuzione.addStepExecutions(java.util.List.of(step));
        return esecuzione;
    }

    @Test
    @DisplayName("abandonStaleJobExecution marca come FAILED il job e gli step in corso")
    void abbandonaJobEStepInCorso() {
        JobExecution esecuzione = esecuzioneConStep(BatchStatus.STARTED);

        assertTrue(service.abandonStaleJobExecution(esecuzione));

        assertEquals(BatchStatus.FAILED, esecuzione.getStatus());
        assertNotNull(esecuzione.getEndTime());
        StepExecution step = esecuzione.getStepExecutions().iterator().next();
        assertEquals(BatchStatus.FAILED, step.getStatus());
        assertNotNull(step.getEndTime());
        verify(jobRepository).update(step);
        verify(jobRepository).update(esecuzione);
    }

    @Test
    @DisplayName("abandonStaleJobExecution non tocca gli step gia' conclusi")
    void nonToccaStepConclusi() {
        JobExecution esecuzione = esecuzioneConStep(BatchStatus.COMPLETED);

        assertTrue(service.abandonStaleJobExecution(esecuzione));

        StepExecution step = esecuzione.getStepExecutions().iterator().next();
        assertEquals(BatchStatus.COMPLETED, step.getStatus());
        verify(jobRepository, never()).update(step);
    }

    @Test
    @DisplayName("abandonStaleJobExecution restituisce false se il repository fallisce")
    void abbandonoFallisceSenzaPropagare() {
        JobExecution esecuzione = esecuzioneConStep(BatchStatus.COMPLETED);
        doThrow(new RuntimeException("database non raggiungibile"))
                .when(jobRepository).update(any(JobExecution.class));

        assertFalse(service.abandonStaleJobExecution(esecuzione));
    }

    @Test
    @DisplayName("forceAbandonJobExecution marca come ABANDONED job e step in corso")
    void terminazioneForzata() {
        JobExecution esecuzione = esecuzioneConStep(BatchStatus.STARTED);

        assertTrue(service.forceAbandonJobExecution(esecuzione, "richiesta operatore"));

        assertEquals(BatchStatus.ABANDONED, esecuzione.getStatus());
        StepExecution step = esecuzione.getStepExecutions().iterator().next();
        assertEquals(BatchStatus.ABANDONED, step.getStatus());
        verify(jobRepository).update(step);
    }

    @Test
    @DisplayName("forceAbandonJobExecution restituisce false se il repository fallisce")
    void terminazioneForzataFallisce() {
        JobExecution esecuzione = esecuzioneConStep(BatchStatus.COMPLETED);
        doThrow(new RuntimeException("database non raggiungibile"))
                .when(jobRepository).update(any(JobExecution.class));

        assertFalse(service.forceAbandonJobExecution(esecuzione, "motivo"));
    }

    @Test
    @DisplayName("Una JobExecution nulla non e' abbandonabile")
    void esecuzioneNulla() {
        assertFalse(service.abandonStaleJobExecution(null));
        assertFalse(service.forceAbandonJobExecution(null, "motivo"));
    }

    @Test
    @DisplayName("getCurrentRunningJobExecution - nessun job in esecuzione")
    void getCurrentRunningJobExecution_noRunningJobs() {
        when(jobRepository.findRunningJobExecutions(JOB_NAME)).thenReturn(Collections.emptySet());

        JobExecution result = service.getCurrentRunningJobExecution(JOB_NAME);

        assertNull(result);
        verify(jobRepository).findRunningJobExecutions(JOB_NAME);
    }

    @Test
    @DisplayName("getCurrentRunningJobExecution - job in esecuzione")
    void getCurrentRunningJobExecution_withRunningJob() {
        JobExecution execution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(jobRepository.findRunningJobExecutions(JOB_NAME)).thenReturn(Set.of(execution));

        JobExecution result = service.getCurrentRunningJobExecution(JOB_NAME);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(jobRepository).findRunningJobExecutions(JOB_NAME);
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
    @DisplayName("isJobExecutionStale - soglia valutata sulla durata reale attraverso la transizione DST")
    void isJobExecutionStale_dstTransition() {
        // Clock fermo alle 03:00 ora locale del 25/10/2026, subito dopo il ritorno all'ora
        // solare (le lancette sono tornate da 03:00 CEST a 02:00 CET): 02:00 UTC.
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T02:00:00Z"), ZoneId.of("Europe/Rome"));
        JobConcurrencyService serviceConClock =
                new JobConcurrencyService(jobRepository, STALE_THRESHOLD_MINUTES, clock);

        // Ultimo aggiornamento alle 01:30 ora locale, ancora in ora legale: 23:30 UTC del 24.
        // Sono passati 150 minuti reali, oltre la soglia di 120, mentre la differenza fra le
        // due ore locali ne conterebbe soltanto 90 e l'esecuzione non risulterebbe stale.
        JobExecution execution = createMockJobExecution(1L, BatchStatus.STARTED);
        when(execution.getLastUpdated()).thenReturn(LocalDateTime.of(2026, 10, 25, 1, 30));

        assertTrue(serviceConClock.isJobExecutionStale(execution));
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

        when(execution.getJobParameters()).thenReturn(params);
        when(params.getString(JobConcurrencyService.JOB_PARAM_CLUSTER_ID)).thenReturn("test-cluster");

        String result = service.getClusterIdFromExecution(execution);

        assertEquals("test-cluster", result);
    }

    @Test
    @DisplayName("getClusterIdFromExecution - senza cluster ID")
    void getClusterIdFromExecution_withoutClusterId() {
        JobExecution execution = mock(JobExecution.class);
        JobParameters params = mock(JobParameters.class);

        when(execution.getJobParameters()).thenReturn(params);
        when(params.getString(JobConcurrencyService.JOB_PARAM_CLUSTER_ID)).thenReturn(null);

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

    @Test
    @DisplayName("getStaleThresholdMinutes - restituisce la soglia configurata")
    void getStaleThresholdMinutes() {
        assertEquals(STALE_THRESHOLD_MINUTES, service.getStaleThresholdMinutes());
    }
}
