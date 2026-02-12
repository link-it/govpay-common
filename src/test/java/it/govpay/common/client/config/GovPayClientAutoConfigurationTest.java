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
