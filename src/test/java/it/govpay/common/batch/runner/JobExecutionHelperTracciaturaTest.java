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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

import it.govpay.common.batch.TriggerType;
import it.govpay.common.batch.service.JobConcurrencyService;
import it.govpay.common.logging.TransactionContext;

/**
 * Verifica la tracciatura dell'esecuzione dei job (BP-LOG-3): il correlation id
 * di chi lancia il job viene ereditato, il transaction id e' nuovo per ogni
 * esecuzione, ed entrambi sono disponibili al job perche' il {@code JobOperator}
 * di default e' sincrono.
 */
@ExtendWith(MockitoExtension.class)
class JobExecutionHelperTracciaturaTest {

    private static final String CLUSTER_ID = "nodo-1";
    private static final String JOB_NAME = "jobDiProva";
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Rome");

    @Mock
    private JobOperator jobOperator;

    @Mock
    private JobConcurrencyService jobConcurrencyService;

    @Mock
    private Job job;

    @Mock
    private JobExecution execution;

    private JobExecutionHelper helper;

    @BeforeEach
    void setUp() {
        helper = new JobExecutionHelper(jobOperator, jobConcurrencyService, CLUSTER_ID, ZONE_ID);
    }

    @AfterEach
    void pulisciContesto() {
        TransactionContext.clear();
    }

    @Test
    @DisplayName("Il correlation id del chiamante e' ereditato dall'esecuzione")
    void ereditaIlCorrelationIdDelChiamante() throws Exception {
        TransactionContext.setCorrelationId("da-richiesta-rest");
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);

        helper.runJob(job, JOB_NAME, TriggerType.MANUAL);

        assertEquals("da-richiesta-rest", parametriCatturati().getString(
                JobExecutionHelper.JOB_PARAM_CORRELATION_ID));
    }

    @Test
    @DisplayName("Senza contesto, l'esecuzione genera un correlation id proprio")
    void generaCorrelationIdSenzaContesto() throws Exception {
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);

        helper.runJob(job, JOB_NAME, TriggerType.SCHEDULED);

        String correlationId = parametriCatturati().getString(
                JobExecutionHelper.JOB_PARAM_CORRELATION_ID);
        assertNotNull(UUID.fromString(correlationId));
    }

    @Test
    @DisplayName("Il job vede transaction id e correlation id in MDC")
    void ilJobVedeGliIdentificativi() throws Exception {
        TransactionContext.setCorrelationId("correlazione-esterna");
        AtomicReference<String> transactionIdNelJob = new AtomicReference<>();
        AtomicReference<String> correlationIdNelJob = new AtomicReference<>();

        when(jobOperator.start(eq(job), any(JobParameters.class))).thenAnswer(invocation -> {
            transactionIdNelJob.set(TransactionContext.getTransactionId());
            correlationIdNelJob.set(TransactionContext.getCorrelationId());
            return execution;
        });

        helper.runJob(job, JOB_NAME, TriggerType.MANUAL);

        assertNotNull(UUID.fromString(transactionIdNelJob.get()));
        assertEquals("correlazione-esterna", correlationIdNelJob.get());
    }

    @Test
    @DisplayName("Ogni esecuzione ha un transaction id diverso")
    void transactionIdDiversoPerEsecuzione() throws Exception {
        AtomicReference<String> primo = new AtomicReference<>();
        AtomicReference<String> secondo = new AtomicReference<>();
        AtomicReference<AtomicReference<String>> destinazione = new AtomicReference<>(primo);

        when(jobOperator.start(eq(job), any(JobParameters.class))).thenAnswer(invocation -> {
            destinazione.get().set(TransactionContext.getTransactionId());
            destinazione.set(secondo);
            return execution;
        });

        helper.runJob(job, JOB_NAME, TriggerType.SCHEDULED);
        helper.runJob(job, JOB_NAME, TriggerType.SCHEDULED);

        assertNotNull(primo.get());
        assertNotEquals(primo.get(), secondo.get());
    }

    @Test
    @DisplayName("Il contesto del chiamante e' ripristinato dopo il lancio")
    void ripristinaIlContestoDelChiamante() throws Exception {
        TransactionContext.setTransactionId("tx-chiamante");
        TransactionContext.setCorrelationId("corr-chiamante");
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);

        helper.runJob(job, JOB_NAME, TriggerType.MANUAL);

        assertEquals("tx-chiamante", TransactionContext.getTransactionId());
        assertEquals("corr-chiamante", TransactionContext.getCorrelationId());
    }

    @Test
    @DisplayName("Senza contesto iniziale l'MDC resta pulito dopo il lancio")
    void nonLasciaResiduiInMdc() throws Exception {
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);

        helper.runJob(job, JOB_NAME, TriggerType.SCHEDULED);

        assertNull(TransactionContext.getTransactionId());
        assertNull(TransactionContext.getCorrelationId());
    }

    @Test
    @DisplayName("CorrelationID e' un parametro non identificante")
    void correlationIdNonIdentificante() throws Exception {
        when(jobOperator.start(eq(job), any(JobParameters.class))).thenReturn(execution);

        helper.runJob(job, JOB_NAME, TriggerType.SCHEDULED);

        JobParameters parametri = parametriCatturati();
        assertFalse(parametri.getParameter(JobExecutionHelper.JOB_PARAM_CORRELATION_ID).identifying(),
                "CorrelationID non deve concorrere all'identita' della JobInstance");
        // i parametri preesistenti restano identificanti
        assertEquals(CLUSTER_ID, parametri.getString(JobExecutionHelper.JOB_PARAM_CLUSTER_ID));
        assertEquals(JOB_NAME, parametri.getString(JobExecutionHelper.JOB_PARAM_JOB_ID));
    }

    private JobParameters parametriCatturati() throws Exception {
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        org.mockito.Mockito.verify(jobOperator, org.mockito.Mockito.atLeastOnce())
                .start(eq(job), captor.capture());
        return captor.getValue();
    }
}
