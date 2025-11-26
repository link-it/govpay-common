package it.govpay.common.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configurazione per l'esecuzione asincrona dei client HTTP.
 *
 * <p>Configura un ThreadPoolTaskExecutor per gestire le chiamate HTTP asincrone
 * tramite AsyncRestTemplateWrapper. Il pool è dimensionabile tramite properties.
 *
 * <p>Configurazione di default:
 * <ul>
 *   <li>Core pool size: 10 thread</li>
 *   <li>Max pool size: 50 thread</li>
 *   <li>Queue capacity: 100 task</li>
 *   <li>Thread name prefix: "async-http-"</li>
 *   <li>Reject policy: CallerRunsPolicy (fallback su thread chiamante)</li>
 * </ul>
 *
 * <p>Personalizzazione tramite application.yml:
 * <pre>
 * govpay:
 *   client:
 *     async:
 *       core-pool-size: 20
 *       max-pool-size: 100
 *       queue-capacity: 200
 * </pre>
 */
@Slf4j
@Configuration
public class AsyncClientConfiguration {

    @Value("${govpay.client.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${govpay.client.async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${govpay.client.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${govpay.client.async.thread-name-prefix:async-http-}")
    private String threadNamePrefix;

    /**
     * Crea e configura l'Executor per le operazioni HTTP asincrone.
     *
     * @return Executor configurato per AsyncRestTemplateWrapper
     */
    @Bean(name = "asyncHttpExecutor")
    public Executor asyncHttpExecutor() {
        log.info("Configurazione AsyncHttpExecutor: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool: numero minimo di thread sempre attivi
        executor.setCorePoolSize(corePoolSize);

        // Max pool: numero massimo di thread quando la queue è piena
        executor.setMaxPoolSize(maxPoolSize);

        // Queue capacity: numero di task in attesa prima di creare nuovi thread
        executor.setQueueCapacity(queueCapacity);

        // Thread name prefix per identificazione nei log
        executor.setThreadNamePrefix(threadNamePrefix);

        // Reject policy: se pool e queue sono pieni, esegue sul thread chiamante
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Aspetta il completamento dei task attivi allo shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Timeout massimo per lo shutdown graceful (secondi)
        executor.setAwaitTerminationSeconds(60);

        // Inizializza il thread pool
        executor.initialize();

        log.info("AsyncHttpExecutor configurato con successo");

        return executor;
    }
}
