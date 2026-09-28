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

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.core.Ordered;

import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.logging.TransactionContext;

/**
 * Garantisce che l'esecuzione di un job abbia in MDC transaction id e
 * correlation id (BP-LOG-3), anche quando non gira sul thread che l'ha lanciata.
 *
 * <p>Nel percorso normale il contesto e' gia' aperto da
 * {@link JobExecutionHelper#runJob}: il {@code JobOperator} di default e'
 * sincrono, quindi il job gira sul thread chiamante e questo listener non ha
 * nulla da fare. Serve nei casi in cui quel contesto non c'e':
 * <ul>
 *   <li>lancio su un {@code TaskExecutor} asincrono;</li>
 *   <li>restart di una {@code JobExecution} preesistente;</li>
 *   <li>job avviati senza passare dall'helper.</li>
 * </ul>
 *
 * <p>In quei casi il correlation id viene recuperato dal parametro di job
 * {@value JobExecutionHelper#JOB_PARAM_CORRELATION_ID}, che l'helper registra
 * come parametro non identificante: l'identificativo sopravvive quindi nei
 * metadati Spring Batch oltre la vita del thread che ha lanciato il job.
 *
 * <p>Il listener non tocca un contesto gia' presente e ripulisce solo quello che
 * ha aperto lui, per non azzerare l'MDC del chiamante.
 */
public class TransactionContextJobListener implements JobExecutionListener, Ordered {

    /** Ricorda se il contesto e' stato aperto da questo listener sul thread corrente. */
    private static final ThreadLocal<Boolean> APERTO_DA_NOI = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if (TransactionContext.getTransactionId() != null) {
            // Contesto gia' aperto da JobExecutionHelper: non lo tocchiamo.
            APERTO_DA_NOI.set(Boolean.FALSE);
            return;
        }
        TransactionContext.setTransactionId(TransactionContext.nuovoId());
        TransactionContext.setCorrelationId(correlationIdDaParametri(jobExecution));
        APERTO_DA_NOI.set(Boolean.TRUE);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (Boolean.TRUE.equals(APERTO_DA_NOI.get())) {
            TransactionContext.clear();
        }
        APERTO_DA_NOI.remove();
    }

    private static String correlationIdDaParametri(JobExecution jobExecution) {
        // I parametri possono mancare: la tracciatura non deve mai far fallire
        // l'esecuzione di un job per un identificativo assente.
        JobParameters parametri = jobExecution.getJobParameters();
        String valore = (parametri != null)
                ? parametri.getString(JobExecutionHelper.JOB_PARAM_CORRELATION_ID)
                : null;
        return (valore != null && !valore.isBlank()) ? valore : TransactionContext.nuovoId();
    }

    /**
     * Ordine massimo: il contesto deve esistere prima che gli altri listener
     * producano log.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
