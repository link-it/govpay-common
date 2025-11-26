package it.govpay.common.client.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AsyncClientConfigurationTest {

    @Autowired
    @Qualifier("asyncHttpExecutor")
    private Executor asyncHttpExecutor;

    @Test
    void testAsyncHttpExecutorBeanCreated() {
        assertNotNull(asyncHttpExecutor);
    }

    @Test
    void testAsyncHttpExecutorIsThreadPoolTaskExecutor() {
        assertInstanceOf(ThreadPoolTaskExecutor.class, asyncHttpExecutor);
    }

    @Test
    void testAsyncHttpExecutorConfiguration() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncHttpExecutor;

        // Verify default configuration values
        assertTrue(executor.getCorePoolSize() > 0);
        assertTrue(executor.getMaxPoolSize() >= executor.getCorePoolSize());
        assertNotNull(executor.getThreadNamePrefix());
    }

    @Test
    void testAsyncHttpExecutorThreadNamePrefix() {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncHttpExecutor;

        // Verify thread name prefix is set
        String prefix = executor.getThreadNamePrefix();
        assertNotNull(prefix);
        assertFalse(prefix.isEmpty());
    }

    @Test
    void testAsyncHttpExecutorCanExecuteTask() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) asyncHttpExecutor;

        // Test that the executor can actually run tasks
        java.util.concurrent.CompletableFuture<String> future = new java.util.concurrent.CompletableFuture<>();

        executor.execute(() -> future.complete("executed"));

        String result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertEquals("executed", result);
    }
}
