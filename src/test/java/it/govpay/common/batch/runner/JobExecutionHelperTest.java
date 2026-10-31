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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobOperator;

import it.govpay.common.batch.EsitoTick;
import it.govpay.common.batch.JobTickObserver;
import it.govpay.common.batch.TriggerType;
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
    @Test
    @DisplayName("checkBeforeExecution - senza trigger notifica un tick SCHEDULED")
    void checkBeforeExecution_notificaTickSchedulato() {
        JobTickObserver observer = mock(JobTickObserver.class);
        helper.setTickObserver(observer);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);

        helper.checkBeforeExecution(JOB_NAME);

        verify(observer).onTick(JOB_NAME, TriggerType.SCHEDULED, EsitoTick.CAN_PROCEED);
    }

    @ParameterizedTest
    @EnumSource(PreExecutionCheckResult.class)
    @DisplayName("checkBeforeExecution - ogni esito diventa il tick omonimo, col trigger indicato")
    void checkBeforeExecution_notificaTickPerOgniEsito(PreExecutionCheckResult atteso) {
        JobTickObserver observer = mock(JobTickObserver.class);
        helper.setTickObserver(observer);
        preparaEsito(atteso);

        PreExecutionResult result = helper.checkBeforeExecution(JOB_NAME, TriggerType.MANUAL);

        assertEquals(atteso, result.result());
        verify(observer).onTick(JOB_NAME, TriggerType.MANUAL, EsitoTick.valueOf(atteso.name()));
        verifyNoMoreInteractions(observer);
    }

    private void preparaEsito(PreExecutionCheckResult esito) {
        if (esito == PreExecutionCheckResult.CAN_PROCEED) {
            when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);
            return;
        }
        JobExecution running = mock(JobExecution.class);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(running);
        boolean stale = esito == PreExecutionCheckResult.STALE_ABANDONED_CAN_PROCEED
                || esito == PreExecutionCheckResult.STALE_ABANDON_FAILED;
        when(jobConcurrencyService.isJobExecutionStale(running)).thenReturn(stale);
        if (stale) {
            when(jobConcurrencyService.abandonStaleJobExecution(running))
                    .thenReturn(esito == PreExecutionCheckResult.STALE_ABANDONED_CAN_PROCEED);
        } else {
            when(jobConcurrencyService.getClusterIdFromExecution(running))
                    .thenReturn(esito == PreExecutionCheckResult.RUNNING_ON_OTHER_NODE ? "other-cluster" : CLUSTER_ID);
        }
    }

    @Test
    @DisplayName("checkBeforeExecution - un osservatore che fallisce non blocca il controllo")
    void checkBeforeExecution_osservatoreInErrore() {
        helper.setTickObserver((jobName, trigger, esito) -> { throw new IllegalStateException("registry non disponibile"); });
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);

        PreExecutionResult result = assertDoesNotThrow(() -> helper.checkBeforeExecution(JOB_NAME));

        assertTrue(result.canProceed());
    }

    @Test
    @DisplayName("setTickObserver(null) - torna all'osservatore che non fa nulla")
    void setTickObserver_nullRipristinaNessuno() {
        JobTickObserver observer = mock(JobTickObserver.class);
        helper.setTickObserver(observer);
        helper.setTickObserver(null);

        assertDoesNotThrow(() -> helper.registraTick(JOB_NAME, TriggerType.MANUAL, EsitoTick.FORZATO));

        verifyNoInteractions(observer);
    }

    @Test
    @DisplayName("executeIfPossible - notifica un solo tick")
    void executeIfPossible_unSoloTick() throws Exception {
        JobTickObserver observer = mock(JobTickObserver.class);
        helper.setTickObserver(observer);
        JobExecution running = mock(JobExecution.class);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(running);
        when(jobConcurrencyService.isJobExecutionStale(running)).thenReturn(false);
        when(jobConcurrencyService.getClusterIdFromExecution(running)).thenReturn("other-cluster");

        assertNull(helper.executeIfPossible(job, JOB_NAME));

        verify(observer, times(1)).onTick(JOB_NAME, TriggerType.SCHEDULED, EsitoTick.RUNNING_ON_OTHER_NODE);
        verifyNoMoreInteractions(observer);
    }
    @Test
    @DisplayName("isEseguitaLocalmente - vero durante l'avvio sincrono, falso dopo")
    void isEseguitaLocalmente_avvioSincrono() throws Exception {
        JobExecution conclusa = mock(JobExecution.class);
        when(conclusa.isRunning()).thenReturn(false);
        boolean[] durante = new boolean[1];
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenAnswer(inv -> {
            durante[0] = helper.isEseguitaLocalmente(JOB_NAME, null);
            return conclusa;
        });

        helper.runJob(job, JOB_NAME);

        assertTrue(durante[0]);
        assertFalse(helper.isEseguitaLocalmente(JOB_NAME, conclusa));
        assertFalse(helper.isEseguitaLocalmente("altroJob", null));
    }

    @Test
    @DisplayName("isEseguitaLocalmente - con avvio asincrono riconosce l'esecuzione ancora in corso")
    void isEseguitaLocalmente_avvioAsincrono() throws Exception {
        JobExecution inCorso = mock(JobExecution.class);
        when(inCorso.isRunning()).thenReturn(true);
        when(inCorso.getId()).thenReturn(10L);
        JobExecution altrove = mock(JobExecution.class);
        when(altrove.getId()).thenReturn(11L);
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(inCorso);

        helper.runJob(job, JOB_NAME);

        assertTrue(helper.isEseguitaLocalmente(JOB_NAME, inCorso));
        assertFalse(helper.isEseguitaLocalmente(JOB_NAME, altrove));
    }

    @Test
    @DisplayName("isEseguitaLocalmente - un'esecuzione locale conclusa viene dimenticata al controllo successivo")
    void isEseguitaLocalmente_dimenticaLeConcluse() throws Exception {
        JobExecution inCorso = mock(JobExecution.class);
        when(inCorso.isRunning()).thenReturn(true);
        when(inCorso.getId()).thenReturn(10L);
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(inCorso);
        helper.runJob(job, JOB_NAME);

        // un controllo trova l'esecuzione ancora in corso: resta locale
        JobExecution stessa = mock(JobExecution.class);
        when(stessa.getId()).thenReturn(10L);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(stessa);
        when(jobConcurrencyService.getClusterIdFromExecution(stessa)).thenReturn(CLUSTER_ID);
        helper.checkBeforeExecution(JOB_NAME);
        assertTrue(helper.isEseguitaLocalmente(JOB_NAME, inCorso));

        // il controllo successivo non la trova piu' in corso: viene dimenticata
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);
        helper.checkBeforeExecution(JOB_NAME);
        assertFalse(helper.isEseguitaLocalmente(JOB_NAME, inCorso));
    }

    @Test
    @DisplayName("isEseguitaLocalmente - l'avvio fallito non lascia il job tra quelli in corso")
    void isEseguitaLocalmente_avvioFallito() throws Exception {
        when(jobOperator.start(eq(job), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("gia' in esecuzione"));

        assertThrows(JobExecutionAlreadyRunningException.class, () -> helper.runJob(job, JOB_NAME));

        assertFalse(helper.isEseguitaLocalmente(JOB_NAME, null));
    }

    @Test
    @DisplayName("isEseguitaLocalmente - un avvio che non restituisce l'esecuzione non lascia tracce")
    void isEseguitaLocalmente_avvioSenzaEsecuzione() throws Exception {
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(null);

        assertNull(helper.runJob(job, JOB_NAME));

        assertFalse(helper.isEseguitaLocalmente(JOB_NAME, null));
    }

    @Test
    @DisplayName("isEseguitaLocalmente - il controllo di un job non dimentica le esecuzioni locali degli altri")
    void isEseguitaLocalmente_altriJobNonToccati() throws Exception {
        JobExecution altroJob = mock(JobExecution.class);
        when(altroJob.isRunning()).thenReturn(true);
        when(altroJob.getId()).thenReturn(20L);
        JobExecution questoJob = mock(JobExecution.class);
        when(questoJob.isRunning()).thenReturn(true);
        when(questoJob.getId()).thenReturn(21L);
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(altroJob, questoJob);
        helper.runJob(job, "altroJob");
        helper.runJob(job, JOB_NAME);

        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(null);
        helper.checkBeforeExecution(JOB_NAME);

        assertTrue(helper.isEseguitaLocalmente("altroJob", altroJob));
        assertFalse(helper.isEseguitaLocalmente(JOB_NAME, questoJob));
    }

    @Test
    @DisplayName("isEseguitaLocalmente - se in corso c'e' un'altra esecuzione, quella locale e' conclusa")
    void isEseguitaLocalmente_inCorsoUnAltraEsecuzione() throws Exception {
        JobExecution locale = mock(JobExecution.class);
        when(locale.isRunning()).thenReturn(true);
        when(locale.getId()).thenReturn(10L);
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(locale);
        helper.runJob(job, JOB_NAME);

        JobExecution altra = mock(JobExecution.class);
        when(altra.getId()).thenReturn(99L);
        when(jobConcurrencyService.getCurrentRunningJobExecution(JOB_NAME)).thenReturn(altra);
        when(jobConcurrencyService.getClusterIdFromExecution(altra)).thenReturn("other-cluster");
        helper.checkBeforeExecution(JOB_NAME);

        assertFalse(helper.isEseguitaLocalmente(JOB_NAME, locale));
    }
}
