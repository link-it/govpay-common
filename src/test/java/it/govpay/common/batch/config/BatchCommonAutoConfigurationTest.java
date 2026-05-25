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
package it.govpay.common.batch.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;

@ExtendWith(MockitoExtension.class)
class BatchCommonAutoConfigurationTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobOperator jobOperator;

    @Test
    @DisplayName("createJobConcurrencyService con parametri diretti")
    void createJobConcurrencyService_directParams() {
        JobConcurrencyService service = BatchCommonAutoConfiguration
                .createJobConcurrencyService(jobRepository, 60);

        assertNotNull(service);
    }

    @Test
    @DisplayName("createJobConcurrencyService con BatchJobProperties")
    void createJobConcurrencyService_withProperties() {
        BatchJobProperties properties = new BatchJobProperties();
        properties.setStaleThresholdMinutes(90);

        JobConcurrencyService service = BatchCommonAutoConfiguration
                .createJobConcurrencyService(jobRepository, properties);

        assertNotNull(service);
    }

    @Test
    @DisplayName("createJobExecutionHelper con parametri diretti")
    void createJobExecutionHelper_directParams() {
        JobConcurrencyService concurrencyService = BatchCommonAutoConfiguration
                .createJobConcurrencyService(jobRepository, 120);

        JobExecutionHelper helper = BatchCommonAutoConfiguration
                .createJobExecutionHelper(jobOperator, concurrencyService, "my-cluster",
                        java.time.ZoneId.of("Europe/Rome"));

        assertNotNull(helper);
        assertEquals("my-cluster", helper.getClusterId());
    }

    @Test
    @DisplayName("createJobExecutionHelper con BatchJobProperties")
    void createJobExecutionHelper_withProperties() {
        BatchJobProperties properties = new BatchJobProperties();
        properties.setClusterId("test-cluster");
        properties.setTimeZone("Europe/Rome");

        JobConcurrencyService concurrencyService = BatchCommonAutoConfiguration
                .createJobConcurrencyService(jobRepository, properties);

        JobExecutionHelper helper = BatchCommonAutoConfiguration
                .createJobExecutionHelper(jobOperator, concurrencyService, properties);

        assertNotNull(helper);
        assertEquals("test-cluster", helper.getClusterId());
    }
}
