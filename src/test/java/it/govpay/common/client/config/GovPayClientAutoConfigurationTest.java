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
package it.govpay.common.client.config;

import it.govpay.common.repository.ConnettoreEntityRepository;
import it.govpay.common.client.service.ConnettoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class GovPayClientAutoConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired(required = false)
    private GovPayClientAutoConfiguration govPayClientAutoConfiguration;

    @Autowired(required = false)
    private ConnettoreService connettoreService;

    @Autowired(required = false)
    private ConnettoreEntityRepository connettoreEntityRepository;

    @Test
    void testAutoConfigurationBeanCreated() {
        assertNotNull(govPayClientAutoConfiguration);
    }

    @Test
    void testConnettoreServiceBeanCreated() {
        assertNotNull(connettoreService);
    }

    @Test
    void testConnettoreEntityRepositoryBeanCreated() {
        assertNotNull(connettoreEntityRepository);
    }

    @Test
    void testComponentScanWorking() {
        // Verify that beans in the govpay.common.client package are scanned
        assertTrue(applicationContext.containsBean("connettoreService"));
    }

    @Test
    void testJpaRepositoryEnabled() {
        // Verify that JPA repository is enabled and working
        assertNotNull(connettoreEntityRepository);
        // Should be able to call methods without exception
        assertDoesNotThrow(() -> connettoreEntityRepository.count());
    }
}
