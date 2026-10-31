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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.ObjectProvider;

import it.govpay.common.batch.CatturaLog;
import it.govpay.common.batch.EsitoTick;
import it.govpay.common.batch.TriggerType;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;

@ExtendWith(MockitoExtension.class)
class BatchMetricsBeanPostProcessorTest {

    @Mock
    private ObjectProvider<BatchMetricsRecorder> provider;

    @Mock
    private ObjectProvider<BatchMetricsNodo> nodo;

    @Mock
    private BatchMetricsRecorder recorder;

    @Mock
    private JobOperator jobOperator;

    private BatchMetricsBeanPostProcessor postProcessor;

    @BeforeEach
    void setUp() {
        postProcessor = new BatchMetricsBeanPostProcessor(provider, nodo);
    }

    @Test
    @DisplayName("Registra il listener delle metriche su un AbstractJob")
    void listenerSulJob() {
        when(provider.getObject()).thenReturn(recorder);
        AbstractJob job = mock(AbstractJob.class);

        assertSame(job, postProcessor.postProcessAfterInitialization(job, "job"));

        verify(job).registerJobExecutionListener(any(BatchMetricsJobListener.class));
    }

    @Test
    @DisplayName("Un job dietro proxy riceve il listener sul target, e resta il proxy il bean restituito")
    void listenerSulTargetDelProxy() {
        when(provider.getObject()).thenReturn(recorder);
        AbstractJob target = mock(AbstractJob.class);
        Object proxy = new ProxyFactory(target).getProxy();

        assertSame(proxy, postProcessor.postProcessAfterInitialization(proxy, "job"));

        verify(target).registerJobExecutionListener(any(JobExecutionListener.class));
    }

    @Test
    @DisplayName("Imposta l'osservatore dei tick e pubblica la soglia di stale dell'helper")
    void osservatoreESogliaSullHelper() {
        when(provider.getObject()).thenReturn(recorder);
        JobConcurrencyService service = mock(JobConcurrencyService.class);
        when(service.getStaleThresholdMinutes()).thenReturn(90);
        JobExecutionHelper helper = new JobExecutionHelper(jobOperator, service, "nodo-1", ZoneId.of("Europe/Rome"));

        postProcessor.postProcessAfterInitialization(helper, "jobExecutionHelper");
        helper.registraTick("job", TriggerType.SCHEDULED, EsitoTick.CAN_PROCEED);

        verify(recorder).registraSogliaStale(90);
        verify(recorder).onTick("job", TriggerType.SCHEDULED, EsitoTick.CAN_PROCEED);
    }

    @Test
    @DisplayName("Un helper senza JobConcurrencyService riceve l'osservatore ma nessuna soglia")
    void helperSenzaConcurrencyService() {
        when(provider.getObject()).thenReturn(recorder);
        JobExecutionHelper helper = new JobExecutionHelper(jobOperator, null, "nodo-1", ZoneId.of("Europe/Rome"));

        postProcessor.postProcessAfterInitialization(helper, "jobExecutionHelper");
        helper.registraTick("job", TriggerType.MANUAL, EsitoTick.FORZATO);

        verify(recorder, never()).registraSogliaStale(anyInt());
        verify(recorder).onTick("job", TriggerType.MANUAL, EsitoTick.FORZATO);
    }

    @Test
    @DisplayName("Gli altri bean passano invariati e il recorder non viene nemmeno risolto")
    void altriBean() {
        Job job = mock(Job.class);
        Object bean = new Object();

        assertSame(bean, postProcessor.postProcessAfterInitialization(bean, "altro"));
        assertSame(job, postProcessor.postProcessAfterInitialization(job, "jobNonAbstract"));

        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("Segnala se il tag nodo delle metriche e' diverso dal cluster-id dell'helper")
    void nodoDiversoDalClusterId() {
        when(provider.getObject()).thenReturn(recorder);
        when(nodo.getIfAvailable()).thenReturn(new BatchMetricsNodo("host-1", "identificativo dell'host"));
        JobExecutionHelper helper = new JobExecutionHelper(jobOperator, null, "GovPay-ACA-Batch", ZoneId.of("Europe/Rome"));

        try (CatturaLog log = CatturaLog.di(BatchMetricsBeanPostProcessor.class)) {
            postProcessor.postProcessAfterInitialization(helper, "jobExecutionHelper");

            assertTrue(log.contiene("diverso dal cluster-id del JobExecutionHelper"));
        }
    }

    @Test
    @DisplayName("Nessuna segnalazione se il tag nodo coincide col cluster-id o non e' attivo")
    void nodoCoerenteONonAttivo() {
        when(provider.getObject()).thenReturn(recorder);
        JobExecutionHelper helper = new JobExecutionHelper(jobOperator, null, "nodo-1", ZoneId.of("Europe/Rome"));

        try (CatturaLog log = CatturaLog.di(BatchMetricsBeanPostProcessor.class)) {
            when(nodo.getIfAvailable()).thenReturn(new BatchMetricsNodo("nodo-1", "govpay.batch.cluster-id"));
            postProcessor.postProcessAfterInitialization(helper, "jobExecutionHelper");
            when(nodo.getIfAvailable()).thenReturn(null);
            postProcessor.postProcessAfterInitialization(helper, "jobExecutionHelper");

            assertFalse(log.contiene("diverso dal cluster-id"));
        }
    }
}
