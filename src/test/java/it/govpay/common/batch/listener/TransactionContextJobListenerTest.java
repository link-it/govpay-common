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
package it.govpay.common.batch.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.logging.TransactionContext;

/**
 * Verifica il ripristino del contesto di tracciatura quando il job non gira sul
 * thread che l'ha lanciato (launcher asincrono, restart), e il rispetto di un
 * contesto gia' aperto da {@code JobExecutionHelper}.
 */
class TransactionContextJobListenerTest {

    private final TransactionContextJobListener listener = new TransactionContextJobListener();

    /** L'MDC e' per-thread e condiviso fra le classi di test: si parte puliti. */
    @BeforeEach
    void preparaContesto() {
        TransactionContext.clear();
    }

    @AfterEach
    void pulisciContesto() {
        TransactionContext.clear();
    }

    @Test
    @DisplayName("Senza contesto, ripristina il correlation id dai parametri di job")
    void ripristinaDaiParametri() {
        JobExecution esecuzione = esecuzioneCon("corr-persistito");

        listener.beforeJob(esecuzione);

        assertEquals("corr-persistito", TransactionContext.getCorrelationId());
        assertNotNull(UUID.fromString(TransactionContext.getTransactionId()));
    }

    @Test
    @DisplayName("Senza parametro, genera un correlation id")
    void generaSeParametroAssente() {
        JobExecution esecuzione = esecuzioneCon(null);

        listener.beforeJob(esecuzione);

        assertNotNull(UUID.fromString(TransactionContext.getCorrelationId()));
    }

    @Test
    @DisplayName("Non tocca un contesto gia' aperto dall'helper")
    void nonToccaContestoEsistente() {
        TransactionContext.setTransactionId("tx-esistente");
        TransactionContext.setCorrelationId("corr-esistente");

        listener.beforeJob(esecuzioneCon("corr-dai-parametri"));

        assertEquals("tx-esistente", TransactionContext.getTransactionId());
        assertEquals("corr-esistente", TransactionContext.getCorrelationId());
    }

    @Test
    @DisplayName("Ripulisce solo il contesto che ha aperto lui")
    void ripulisceSoloIlProprioContesto() {
        JobExecution esecuzione = esecuzioneCon("corr-persistito");

        listener.beforeJob(esecuzione);
        listener.afterJob(esecuzione);

        assertNull(TransactionContext.getTransactionId());
        assertNull(TransactionContext.getCorrelationId());
    }

    @Test
    @DisplayName("Non azzera l'MDC del chiamante se non lo ha aperto lui")
    void nonAzzeraIlContestoDelChiamante() {
        TransactionContext.setTransactionId("tx-chiamante");
        TransactionContext.setCorrelationId("corr-chiamante");
        JobExecution esecuzione = esecuzioneCon("corr-dai-parametri");

        listener.beforeJob(esecuzione);
        listener.afterJob(esecuzione);

        assertEquals("tx-chiamante", TransactionContext.getTransactionId());
        assertEquals("corr-chiamante", TransactionContext.getCorrelationId());
    }

    private static JobExecution esecuzioneCon(String correlationId) {
        JobParametersBuilder builder = new JobParametersBuilder()
                .addString(JobExecutionHelper.JOB_PARAM_JOB_ID, "jobDiProva");
        if (correlationId != null) {
            builder.addString(JobExecutionHelper.JOB_PARAM_CORRELATION_ID, correlationId, false);
        }
        JobParameters parametri = builder.toJobParameters();
        return new JobExecution(1L, new JobInstance(1L, "jobDiProva"), parametri);
    }
}
