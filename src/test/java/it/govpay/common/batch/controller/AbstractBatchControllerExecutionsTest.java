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
package it.govpay.common.batch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import it.govpay.common.batch.controller.AbstractBatchControllerTest.TestBatchController;
import it.govpay.common.batch.dto.ExecutionSummaryInfo;
import it.govpay.common.batch.dto.ExecutionsPage;
import it.govpay.common.batch.dto.LastExecutionInfo;
import it.govpay.common.batch.dto.Problem;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;
import it.govpay.common.client.TestApplication;
import it.govpay.common.entity.batch.BatchJobExecutionEntity;
import it.govpay.common.entity.batch.BatchJobExecutionParamEntity;
import it.govpay.common.entity.batch.BatchJobInstanceEntity;
import jakarta.persistence.EntityManager;

/**
 * Test di integrazione (EntityManager reale, H2) per lo storico esecuzioni
 * ({@code GET /executions}, {@code GET /executions/{id}}): la logica Criteria
 * non e' verificabile in modo significativo con mock puri.
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@Transactional
class AbstractBatchControllerExecutionsTest {

    private static final String JOB_NAME = "executionsTestJob";
    private static final String OTHER_JOB_NAME = "otherTestJob";
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Rome");

    @Autowired
    private EntityManager entityManager;

    private TestBatchController controller;
    private long nextId;

    @BeforeEach
    void setUp() {
        controller = new TestBatchController(mock(JobExecutionHelper.class), mock(JobRepository.class),
                mock(Environment.class), ZONE_ID, 600_000L, entityManager, mock(Job.class), JOB_NAME);
        nextId = System.nanoTime();
    }

    private BatchJobInstanceEntity persistInstance(String jobName) {
        long id = nextId++;
        BatchJobInstanceEntity instance = BatchJobInstanceEntity.builder()
                .id(id).jobName(jobName).jobKey("key-" + id).version(0L).build();
        entityManager.persist(instance);
        return instance;
    }

    private BatchJobExecutionEntity persistExecution(BatchJobInstanceEntity instance, LocalDateTime createTime,
            LocalDateTime startTime, LocalDateTime endTime, String status) {
        long id = nextId++;
        BatchJobExecutionEntity execution = BatchJobExecutionEntity.builder()
                .id(id).jobInstance(instance).createTime(createTime).startTime(startTime).endTime(endTime)
                .status(status).exitCode(status).exitMessage("dettaglio " + status).version(0L).build();
        entityManager.persist(execution);
        return execution;
    }

    private void persistParam(long executionId, String name, String value) {
        entityManager.persist(BatchJobExecutionParamEntity.builder()
                .jobExecutionId(executionId)
                .parameterName(name)
                .parameterType("STRING")
                .parameterValue(value)
                .identifying("N")
                .build());
    }

    @SuppressWarnings("unchecked")
    private ExecutionsPage listExecutions(String stato, OffsetDateTime min, OffsetDateTime max, int page, int limit, boolean total) {
        ResponseEntity<Object> response = controller.listExecutions(stato, min, max, page, limit, total);
        return (ExecutionsPage) response.getBody();
    }

    @Nested
    @DisplayName("listExecutions")
    class ListExecutions {

        @Test
        @DisplayName("Ordina per dataInizio DESC (coalesce startTime/createTime), id DESC a parita'")
        void ordinaPerDataInizioDesc() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            BatchJobExecutionEntity older = persistExecution(instance,
                    LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 1, 10, 0), null, "COMPLETED");
            BatchJobExecutionEntity newer = persistExecution(instance,
                    LocalDateTime.of(2026, 1, 2, 10, 0), null, null, "STARTING");
            entityManager.flush();

            ResponseEntity<Object> response = controller.listExecutions(null, null, null, 1, 10, false);

            assertEquals(200, response.getStatusCode().value());
            ExecutionsPage page = (ExecutionsPage) response.getBody();
            assertEquals(2, page.getResults().size());
            assertEquals(newer.getId(), page.getResults().get(0).getExecutionId());
            assertEquals(older.getId(), page.getResults().get(1).getExecutionId());
            // STARTING: startTime ancora null, dataInizio ricade su createTime.
            assertEquals(newer.getCreateTime(), page.getResults().get(0).getStartTime());
        }

        @Test
        @DisplayName("Esclude le esecuzioni di altri batch")
        void scopingPerJobName() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            BatchJobInstanceEntity otherInstance = persistInstance(OTHER_JOB_NAME);
            BatchJobExecutionEntity mine = persistExecution(instance, LocalDateTime.now(), LocalDateTime.now(), null, "STARTED");
            persistExecution(otherInstance, LocalDateTime.now(), LocalDateTime.now(), null, "STARTED");
            entityManager.flush();

            ExecutionsPage page = listExecutions(null, null, null, 1, 10, false);

            assertEquals(1, page.getResults().size());
            assertEquals(mine.getId(), page.getResults().get(0).getExecutionId());
        }

        @Test
        @DisplayName("Filtro stato")
        void filtroStato() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            BatchJobExecutionEntity completed = persistExecution(instance, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), "COMPLETED");
            persistExecution(instance, LocalDateTime.now(), LocalDateTime.now(), null, "FAILED");
            entityManager.flush();

            ExecutionsPage page = listExecutions("COMPLETED", null, null, 1, 10, false);

            assertEquals(1, page.getResults().size());
            assertEquals(completed.getId(), page.getResults().get(0).getExecutionId());
            assertEquals("COMPLETED", page.getResults().get(0).getStatus());
        }

        @Test
        @DisplayName("Filtro stato - lista comma-separated")
        void filtroStatoCsv() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            BatchJobExecutionEntity failed = persistExecution(instance, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), "FAILED");
            BatchJobExecutionEntity unknown = persistExecution(instance, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), "UNKNOWN");
            persistExecution(instance, LocalDateTime.now(), LocalDateTime.now(), null, "COMPLETED");
            entityManager.flush();

            ExecutionsPage page = listExecutions("FAILED, UNKNOWN", null, null, 1, 10, false);

            List<Long> ids = page.getResults().stream().map(ExecutionSummaryInfo::getExecutionId).toList();
            assertEquals(2, ids.size());
            assertTrue(ids.contains(failed.getId()));
            assertTrue(ids.contains(unknown.getId()));
        }

        @Test
        @DisplayName("Stato non valido - 400")
        void statoNonValido() {
            ResponseEntity<Object> response = controller.listExecutions("NON_ESISTE", null, null, 1, 10, false);

            assertEquals(400, response.getStatusCode().value());
            assertTrue(((Problem) response.getBody()).getDetail().contains("stato"));
        }

        @Test
        @DisplayName("Stato non valido nella lista CSV - 400")
        void statoNonValidoInCsv() {
            ResponseEntity<Object> response = controller.listExecutions("COMPLETED,NON_ESISTE", null, null, 1, 10, false);

            assertEquals(400, response.getStatusCode().value());
        }

        @Test
        @DisplayName("page < 1 - 400")
        void pageNonValido() {
            ResponseEntity<Object> response = controller.listExecutions(null, null, null, 0, 10, false);

            assertEquals(400, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Filtro dataInizioMin/dataInizioMax")
        void filtroDataInizio() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            persistExecution(instance, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 1, 1, 0, 0), null, "STARTED");
            BatchJobExecutionEntity inRange = persistExecution(instance,
                    LocalDateTime.of(2026, 3, 1, 0, 0), LocalDateTime.of(2026, 3, 1, 0, 0), null, "STARTED");
            persistExecution(instance, LocalDateTime.of(2026, 6, 1, 0, 0), LocalDateTime.of(2026, 6, 1, 0, 0), null, "STARTED");
            entityManager.flush();

            OffsetDateTime min = LocalDateTime.of(2026, 2, 1, 0, 0).atZone(ZONE_ID).toOffsetDateTime();
            OffsetDateTime max = LocalDateTime.of(2026, 4, 1, 0, 0).atZone(ZONE_ID).toOffsetDateTime();

            ExecutionsPage page = listExecutions(null, min, max, 1, 10, false);

            assertEquals(1, page.getResults().size());
            assertEquals(inRange.getId(), page.getResults().get(0).getExecutionId());
        }

        @Test
        @DisplayName("Paginazione senza total: hasNextPage calcolato senza COUNT")
        void paginazioneSenzaTotal() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            for (int i = 0; i < 3; i++) {
                persistExecution(instance, LocalDateTime.now().minusMinutes(i), LocalDateTime.now().minusMinutes(i), null, "STARTED");
            }
            entityManager.flush();

            ExecutionsPage page = listExecutions(null, null, null, 1, 2, false);

            assertEquals(2, page.getResults().size());
            assertTrue(page.isHasNextPage());
            assertNull(page.getTotalResults());
            assertNull(page.getTotalPages());
        }

        @Test
        @DisplayName("Paginazione con total=true: valorizza totalResults/totalPages")
        void paginazioneConTotal() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            for (int i = 0; i < 3; i++) {
                persistExecution(instance, LocalDateTime.now().minusMinutes(i), LocalDateTime.now().minusMinutes(i), null, "STARTED");
            }
            entityManager.flush();

            ExecutionsPage page = listExecutions(null, null, null, 1, 2, true);

            assertEquals(2, page.getResults().size());
            assertTrue(page.isHasNextPage());
            assertEquals(3L, page.getTotalResults());
            assertEquals(2, page.getTotalPages());
        }

        @Test
        @DisplayName("triggerType popolato dal JobParameter, senza N+1")
        void triggerTypePopolato() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            BatchJobExecutionEntity execution = persistExecution(instance, LocalDateTime.now(), LocalDateTime.now(), null, "STARTED");
            persistParam(execution.getId(), JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE, "MANUAL");
            entityManager.flush();

            ExecutionsPage page = listExecutions(null, null, null, 1, 10, false);

            List<ExecutionSummaryInfo> results = page.getResults();
            assertEquals(1, results.size());
            assertEquals("MANUAL", results.get(0).getTriggerType());
        }
    }

    @Nested
    @DisplayName("getExecution")
    class GetExecution {

        @Test
        @DisplayName("Esecuzione trovata: include clusterId/triggerType dai JobParameters")
        void esecuzioneTrovata() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            BatchJobExecutionEntity execution = persistExecution(instance,
                    LocalDateTime.of(2026, 5, 1, 10, 0), LocalDateTime.of(2026, 5, 1, 10, 0),
                    LocalDateTime.of(2026, 5, 1, 10, 5), "COMPLETED");
            persistParam(execution.getId(), JobConcurrencyService.JOB_PARAM_CLUSTER_ID, "cluster-xyz");
            persistParam(execution.getId(), JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE, "SCHEDULED");
            entityManager.flush();

            ResponseEntity<Object> response = controller.getExecution(execution.getId());

            assertEquals(200, response.getStatusCode().value());
            LastExecutionInfo info = (LastExecutionInfo) response.getBody();
            assertEquals(execution.getId(), info.getExecutionId());
            assertEquals("cluster-xyz", info.getClusterId());
            assertEquals("SCHEDULED", info.getTriggerType());
            assertEquals("COMPLETED", info.getStatus());
            assertEquals("COMPLETED", info.getExitCode());
            assertEquals("dettaglio COMPLETED", info.getExitDescription());
            assertEquals(300L, info.getDurationSeconds());
        }

        @Test
        @DisplayName("Esecuzione STARTING (startTime ancora null): dataInizio ricade su createTime")
        void esecuzioneInCoda() {
            BatchJobInstanceEntity instance = persistInstance(JOB_NAME);
            BatchJobExecutionEntity execution = persistExecution(instance,
                    LocalDateTime.of(2026, 5, 1, 10, 0), null, null, "STARTING");
            entityManager.flush();

            ResponseEntity<Object> response = controller.getExecution(execution.getId());

            assertEquals(200, response.getStatusCode().value());
            LastExecutionInfo info = (LastExecutionInfo) response.getBody();
            assertEquals(execution.getCreateTime(), info.getStartTime());
            assertNull(info.getDurationSeconds());
        }

        @Test
        @DisplayName("Esecuzione inesistente - 404")
        void esecuzioneInesistente() {
            ResponseEntity<Object> response = controller.getExecution(Long.MAX_VALUE - 1);

            assertEquals(404, response.getStatusCode().value());
        }

        @Test
        @DisplayName("Esecuzione di un altro batch - 404 (scoping)")
        void esecuzioneAltroBatch() {
            BatchJobInstanceEntity otherInstance = persistInstance(OTHER_JOB_NAME);
            BatchJobExecutionEntity execution = persistExecution(otherInstance, LocalDateTime.now(), LocalDateTime.now(), null, "STARTED");
            entityManager.flush();

            ResponseEntity<Object> response = controller.getExecution(execution.getId());

            assertEquals(404, response.getStatusCode().value());
            assertFalse(((Problem) response.getBody()).getDetail() == null);
        }
    }
}
