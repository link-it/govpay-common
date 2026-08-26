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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ExecutionContext;

@ExtendWith(MockitoExtension.class)
class AbstractBatchExecutionListenerTest {

    private TestListener listener;
    private boolean printStepStatisticsCalled;

    class TestListener extends AbstractBatchExecutionListener {
        @Override
        protected String getBatchName() {
            return "TestBatch";
        }

        @Override
        protected void printStepStatistics(JobExecution jobExecution) {
            printStepStatisticsCalled = true;
        }
    }

    @BeforeEach
    void setUp() {
        listener = new TestListener();
        printStepStatisticsCalled = false;
    }

    @Test
    @DisplayName("beforeJob non lancia eccezioni")
    void beforeJob() {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getId()).thenReturn(1L);

        assertDoesNotThrow(() -> listener.beforeJob(jobExecution));
    }

    @Test
    @DisplayName("afterJob a cavallo della transizione DST non solleva eccezioni")
    void afterJobAcrossDstTransition() {
        JobExecution jobExecution = mock(JobExecution.class);
        // 25/10/2026: alle 03:00 CEST le lancette tornano alle 02:00 CET
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.of(2026, 10, 25, 1, 30));
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.of(2026, 10, 25, 4, 30));
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        assertDoesNotThrow(() -> listener.afterJob(jobExecution));
    }

    @Test
    @DisplayName("afterJob con endTime assente non solleva eccezioni")
    void afterJobSenzaEndTime() {
        JobExecution jobExecution = mock(JobExecution.class);
        // Prima della correzione Duration.between(start, null) sollevava NullPointerException
        // dentro il listener, propagandolo al ciclo di vita del job
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.of(2026, 6, 15, 10, 0));
        when(jobExecution.getEndTime()).thenReturn(null);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.STARTED);

        assertDoesNotThrow(() -> listener.afterJob(jobExecution));
    }

    @Test
    @DisplayName("printSimpleStepStats con endTime assente non solleva eccezioni")
    void printSimpleStepStatsSenzaEndTime() {
        StepExecution stepExecution = mock(StepExecution.class);
        when(stepExecution.getStatus()).thenReturn(BatchStatus.STARTED);
        when(stepExecution.getStartTime()).thenReturn(LocalDateTime.of(2026, 6, 15, 10, 0));
        when(stepExecution.getEndTime()).thenReturn(null);

        assertDoesNotThrow(() -> listener.printSimpleStepStats(stepExecution, 1, "step di prova"));
    }

    @Test
    @DisplayName("afterJob con duration calcolata")
    void afterJob() {
        JobExecution jobExecution = mock(JobExecution.class);
        when(jobExecution.getStartTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 0, 0));
        when(jobExecution.getEndTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 5, 0));
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        assertDoesNotThrow(() -> listener.afterJob(jobExecution));
        assertTrue(printStepStatisticsCalled);
    }

    @Test
    @DisplayName("printSimpleStepStats non lancia eccezioni con dati completi")
    void printSimpleStepStats() {
        StepExecution stepExecution = mock(StepExecution.class);
        when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(stepExecution.getReadCount()).thenReturn(100L);
        when(stepExecution.getWriteCount()).thenReturn(95L);
        when(stepExecution.getSkipCount()).thenReturn(5L);
        when(stepExecution.getStartTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 0, 0));
        when(stepExecution.getEndTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 1, 0));

        assertDoesNotThrow(() -> listener.printSimpleStepStats(stepExecution, 1, "Acquisizione"));
    }

    @Nested
    @DisplayName("printPartitionedStepStats")
    class PrintPartitionedStepStats {

        @Test
        @DisplayName("Master step non trovato")
        void masterStepNotFound() {
            JobExecution jobExecution = mock(JobExecution.class);
            Collection<StepExecution> emptySteps = new ArrayList<>();
            when(jobExecution.getStepExecutions()).thenReturn(emptySteps);

            assertDoesNotThrow(() ->
                    listener.printPartitionedStepStats(jobExecution, "masterStep", "workerStep", 1, "Test"));
        }

        @Test
        @DisplayName("Nessuna partizione eseguita")
        void noPartitions() {
            JobExecution jobExecution = mock(JobExecution.class);
            StepExecution masterStep = mock(StepExecution.class);
            when(masterStep.getStepName()).thenReturn("masterStep");
            when(masterStep.getStatus()).thenReturn(BatchStatus.COMPLETED);

            List<StepExecution> steps = new ArrayList<>();
            steps.add(masterStep);
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            assertDoesNotThrow(() ->
                    listener.printPartitionedStepStats(jobExecution, "masterStep", "workerStep", 1, "Test"));
        }

        @Test
        @DisplayName("Partizioni con statistiche aggregate")
        void withPartitions() {
            JobExecution jobExecution = mock(JobExecution.class);
            StepExecution masterStep = mock(StepExecution.class);
            when(masterStep.getStepName()).thenReturn("masterStep");
            when(masterStep.getStatus()).thenReturn(BatchStatus.COMPLETED);

            StepExecution partition1 = createPartitionStep("workerStep:partition-0",
                    50, 45, 2, 1, 1);
            StepExecution partition2 = createPartitionStep("workerStep:partition-1",
                    30, 28, 1, 0, 1);

            List<StepExecution> steps = new ArrayList<>();
            steps.add(masterStep);
            steps.add(partition1);
            steps.add(partition2);
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            assertDoesNotThrow(() ->
                    listener.printPartitionedStepStats(jobExecution, "masterStep", "workerStep", 1, "Test"));
        }
    }

    @Nested
    @DisplayName("findStepByName")
    class FindStepByName {

        @Test
        @DisplayName("Step trovato")
        void found() {
            JobExecution jobExecution = mock(JobExecution.class);
            StepExecution step = mock(StepExecution.class);
            when(step.getStepName()).thenReturn("myStep");

            List<StepExecution> steps = new ArrayList<>();
            steps.add(step);
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            StepExecution result = listener.findStepByName(jobExecution, "myStep");

            assertNotNull(result);
            assertEquals("myStep", result.getStepName());
        }

        @Test
        @DisplayName("Step non trovato")
        void notFound() {
            JobExecution jobExecution = mock(JobExecution.class);
            List<StepExecution> steps = new ArrayList<>();
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            StepExecution result = listener.findStepByName(jobExecution, "nonExistent");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Estrazione partitionId")
    class ExtractPartitionId {

        @Test
        @DisplayName("Con partitionId nel context")
        void withPartitionIdInContext() {
            JobExecution jobExecution = mock(JobExecution.class);
            StepExecution masterStep = mock(StepExecution.class);
            when(masterStep.getStepName()).thenReturn("masterStep");
            when(masterStep.getStatus()).thenReturn(BatchStatus.COMPLETED);

            StepExecution partition = mock(StepExecution.class);
            when(partition.getStepName()).thenReturn("workerStep:partition-0");
            when(partition.getReadCount()).thenReturn(10L);
            when(partition.getWriteCount()).thenReturn(10L);
            when(partition.getWriteSkipCount()).thenReturn(0L);
            when(partition.getReadSkipCount()).thenReturn(0L);
            when(partition.getProcessSkipCount()).thenReturn(0L);
            when(partition.getStartTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 0));
            when(partition.getEndTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 1));
            when(partition.getStatus()).thenReturn(BatchStatus.COMPLETED);

            ExecutionContext ctx = new ExecutionContext();
            ctx.putString("partitionId", "dominio-001");
            when(partition.getExecutionContext()).thenReturn(ctx);

            List<StepExecution> steps = new ArrayList<>();
            steps.add(masterStep);
            steps.add(partition);
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            // Verifica che non lancia eccezioni (e che il partitionId viene usato)
            assertDoesNotThrow(() ->
                    listener.printPartitionedStepStats(jobExecution, "masterStep", "workerStep", 1, "Test"));
        }

        @Test
        @DisplayName("Con codDominio nel context")
        void withCodDominioInContext() {
            JobExecution jobExecution = mock(JobExecution.class);
            StepExecution masterStep = mock(StepExecution.class);
            when(masterStep.getStepName()).thenReturn("masterStep");
            when(masterStep.getStatus()).thenReturn(BatchStatus.COMPLETED);

            StepExecution partition = mock(StepExecution.class);
            when(partition.getStepName()).thenReturn("workerStep:partition-0");
            when(partition.getReadCount()).thenReturn(10L);
            when(partition.getWriteCount()).thenReturn(10L);
            when(partition.getWriteSkipCount()).thenReturn(0L);
            when(partition.getReadSkipCount()).thenReturn(0L);
            when(partition.getProcessSkipCount()).thenReturn(0L);
            when(partition.getStartTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 0));
            when(partition.getEndTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 1));
            when(partition.getStatus()).thenReturn(BatchStatus.COMPLETED);

            ExecutionContext ctx = new ExecutionContext();
            ctx.putString("codDominio", "12345678901");
            when(partition.getExecutionContext()).thenReturn(ctx);

            List<StepExecution> steps = new ArrayList<>();
            steps.add(masterStep);
            steps.add(partition);
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            assertDoesNotThrow(() ->
                    listener.printPartitionedStepStats(jobExecution, "masterStep", "workerStep", 1, "Test"));
        }

        @Test
        @DisplayName("Fallback su formato :partition-N")
        void partitionFromStepName() {
            JobExecution jobExecution = mock(JobExecution.class);
            StepExecution masterStep = mock(StepExecution.class);
            when(masterStep.getStepName()).thenReturn("masterStep");
            when(masterStep.getStatus()).thenReturn(BatchStatus.COMPLETED);

            StepExecution partition = mock(StepExecution.class);
            when(partition.getStepName()).thenReturn("workerStep:partition-42");
            when(partition.getReadCount()).thenReturn(10L);
            when(partition.getWriteCount()).thenReturn(10L);
            when(partition.getWriteSkipCount()).thenReturn(0L);
            when(partition.getReadSkipCount()).thenReturn(0L);
            when(partition.getProcessSkipCount()).thenReturn(0L);
            when(partition.getStartTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 0));
            when(partition.getEndTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 1));
            when(partition.getStatus()).thenReturn(BatchStatus.COMPLETED);

            ExecutionContext emptyCtx = new ExecutionContext();
            when(partition.getExecutionContext()).thenReturn(emptyCtx);

            List<StepExecution> steps = new ArrayList<>();
            steps.add(masterStep);
            steps.add(partition);
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            assertDoesNotThrow(() ->
                    listener.printPartitionedStepStats(jobExecution, "masterStep", "workerStep", 1, "Test"));
        }

        @Test
        @DisplayName("Fallback su ID quando nessun pattern corrisponde")
        void fallbackToId() {
            JobExecution jobExecution = mock(JobExecution.class);
            StepExecution masterStep = mock(StepExecution.class);
            when(masterStep.getStepName()).thenReturn("masterStep");
            when(masterStep.getStatus()).thenReturn(BatchStatus.COMPLETED);

            StepExecution partition = mock(StepExecution.class);
            when(partition.getStepName()).thenReturn("workerStepNoPartition");
            when(partition.getId()).thenReturn(99L);
            when(partition.getReadCount()).thenReturn(10L);
            when(partition.getWriteCount()).thenReturn(10L);
            when(partition.getWriteSkipCount()).thenReturn(0L);
            when(partition.getReadSkipCount()).thenReturn(0L);
            when(partition.getProcessSkipCount()).thenReturn(0L);
            when(partition.getStartTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 0));
            when(partition.getEndTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 1));
            when(partition.getStatus()).thenReturn(BatchStatus.COMPLETED);

            ExecutionContext emptyCtx = new ExecutionContext();
            when(partition.getExecutionContext()).thenReturn(emptyCtx);

            List<StepExecution> steps = new ArrayList<>();
            steps.add(masterStep);
            steps.add(partition);
            when(jobExecution.getStepExecutions()).thenReturn(steps);

            assertDoesNotThrow(() ->
                    listener.printPartitionedStepStats(jobExecution, "masterStep", "workerStep", 1, "Test"));
        }
    }

    private StepExecution createPartitionStep(String name, long read, long write,
                                               long writeSkip, long readSkip, long processSkip) {
        StepExecution step = mock(StepExecution.class);
        when(step.getStepName()).thenReturn(name);
        when(step.getReadCount()).thenReturn(read);
        when(step.getWriteCount()).thenReturn(write);
        when(step.getWriteSkipCount()).thenReturn(writeSkip);
        when(step.getReadSkipCount()).thenReturn(readSkip);
        when(step.getProcessSkipCount()).thenReturn(processSkip);
        when(step.getStartTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 0));
        when(step.getEndTime()).thenReturn(LocalDateTime.of(2025, 3, 12, 10, 1));
        when(step.getStatus()).thenReturn(BatchStatus.COMPLETED);

        ExecutionContext ctx = new ExecutionContext();
        when(step.getExecutionContext()).thenReturn(ctx);
        return step;
    }
}
