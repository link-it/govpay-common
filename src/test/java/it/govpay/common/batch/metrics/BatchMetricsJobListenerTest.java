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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.JobExecution;

@ExtendWith(MockitoExtension.class)
class BatchMetricsJobListenerTest {

    @Mock
    private BatchMetricsRecorder recorder;

    @Mock
    private JobExecution jobExecution;

    @InjectMocks
    private BatchMetricsJobListener listener;

    @Test
    @DisplayName("beforeJob e afterJob delegano al recorder")
    void delega() {
        listener.beforeJob(jobExecution);
        listener.afterJob(jobExecution);

        verify(recorder).inizioJob(jobExecution);
        verify(recorder).fineJob(jobExecution);
    }

    @Test
    @DisplayName("Un errore delle metriche all'avvio non fa fallire il job")
    void erroreAllAvvio() {
        doThrow(new IllegalStateException("registry chiuso")).when(recorder).inizioJob(jobExecution);

        assertDoesNotThrow(() -> listener.beforeJob(jobExecution));
    }

    @Test
    @DisplayName("Un errore delle metriche al termine non fa fallire il job")
    void erroreAlTermine() {
        doThrow(new IllegalStateException("registry chiuso")).when(recorder).fineJob(jobExecution);

        assertDoesNotThrow(() -> listener.afterJob(jobExecution));
    }
}
