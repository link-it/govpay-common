package it.govpay.common.batch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import it.govpay.common.batch.dto.ExecutionsPage;
import it.govpay.common.batch.dto.LastExecutionInfo;
import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.client.TestApplication;
import it.govpay.common.entity.batch.BatchJobExecutionEntity;
import it.govpay.common.entity.batch.BatchJobInstanceEntity;
import jakarta.persistence.EntityManager;

/**
 * Endpoint di consultazione chiamati fuori da una transazione, come nei batch
 * con {@code spring.jpa.open-in-view=false}: le relazioni LAZY lette dalla
 * risposta devono essere gia' caricate.
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class AbstractBatchControllerSenzaTransazioneTest {

    private static final String JOB_NAME = "senzaTransazioneJob";

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private AbstractBatchControllerTest.TestBatchController controller;
    private long instanceId;
    private long executionId;

    @BeforeEach
    void setUp() {
        controller = new AbstractBatchControllerTest.TestBatchController(mock(JobExecutionHelper.class),
                mock(JobRepository.class), mock(Environment.class), ZoneId.of("Europe/Rome"), 600_000L,
                entityManager, mock(Job.class), JOB_NAME);
        instanceId = System.nanoTime();
        executionId = instanceId + 1;
        transactionTemplate.executeWithoutResult(status -> {
            BatchJobInstanceEntity instance = BatchJobInstanceEntity.builder()
                    .id(instanceId).jobName(JOB_NAME).jobKey("key-" + instanceId).version(0L).build();
            entityManager.persist(instance);
            entityManager.persist(BatchJobExecutionEntity.builder()
                    .id(executionId).jobInstance(instance).createTime(LocalDateTime.of(2026, 5, 1, 10, 0))
                    .status("COMPLETED").exitCode("COMPLETED").version(0L).build());
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.remove(entityManager.find(BatchJobExecutionEntity.class, executionId));
            entityManager.remove(entityManager.find(BatchJobInstanceEntity.class, instanceId));
        });
    }

    @Test
    void getExecutionFuoriTransazione() {
        ResponseEntity<Object> response = controller.getExecution(executionId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(executionId, ((LastExecutionInfo) response.getBody()).getExecutionId());
    }

    @Test
    void listExecutionsFuoriTransazione() {
        ResponseEntity<Object> response = controller.listExecutions(null, null, null, 1, 10, true);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(executionId, ((ExecutionsPage) response.getBody()).getResults().get(0).getExecutionId());
    }
}
