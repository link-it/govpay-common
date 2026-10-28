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

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilter;
import it.govpay.common.batch.config.BatchJobProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto-configurazione delle metriche di esecuzione dei batch (#32).
 * <p>
 * Si attiva quando ci sono Micrometer (un {@link MeterRegistry}, tipicamente
 * via actuator) e Spring Batch, anche senza web, ed e' attiva di default:
 * {@code govpay.metrics.batch.enabled=false} la spegne. Non richiede modifiche
 * nei progetti batch:
 * <ul>
 *   <li>{@link BatchMetricsBeanPostProcessor} aggancia il listener ai job e
 *       l'osservatore dei tick a {@code JobExecutionHelper};</li>
 *   <li>{@link BatchMetricsInizializzatore} pubblica a contesto pronto gli
 *       intervalli di schedulazione e gli ultimi esiti;</li>
 *   <li>un {@link MeterFilter} aggiunge a tutte le metriche il tag comune
 *       {@value #TAG_NODO} (= {@code clusterId}), disattivabile con
 *       {@code govpay.metrics.batch.tag-nodo=false}.</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration(afterName = "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration")
@ConditionalOnClass({ MeterRegistry.class, Job.class })
@ConditionalOnBean(MeterRegistry.class)
@ConditionalOnProperty(name = "govpay.metrics.batch.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(BatchMetricsProperties.class)
public class BatchMetricsAutoConfiguration {

    /** Tag comune con l'identificativo del nodo */
    public static final String TAG_NODO = "nodo";

    /** Default di {@link BatchJobProperties#getClusterId()}: uguale su tutti i nodi */
    private static final String CLUSTER_ID_DEFAULT = new BatchJobProperties().getClusterId();

    @Bean
    @ConditionalOnMissingBean
    public BatchMetricsRecorder batchMetricsRecorder(MeterRegistry meterRegistry, BatchMetricsProperties properties) {
        return new BatchMetricsRecorder(meterRegistry, properties.getDurationBuckets());
    }

    @Bean
    public static BatchMetricsBeanPostProcessor batchMetricsBeanPostProcessor(
            ObjectProvider<BatchMetricsRecorder> recorder) {
        return new BatchMetricsBeanPostProcessor(recorder);
    }

    @Bean
    public BatchMetricsInizializzatore batchMetricsInizializzatore(BatchMetricsRecorder recorder,
            ConfigurableListableBeanFactory beanFactory, ObjectProvider<JobRepository> jobRepository,
            BatchMetricsProperties properties) {
        return new BatchMetricsInizializzatore(recorder, beanFactory, jobRepository,
                properties.getIstanzeUltimiEsiti());
    }

    /**
     * Tag comune {@value #TAG_NODO}, applicato da Spring Boot a tutti i registry.
     * Senza un unico {@link BatchJobProperties} nel contesto non aggiunge nulla:
     * con piu' bean non si sceglie a caso, e l'avvio non deve fallire per le metriche.
     */
    @Bean
    @ConditionalOnProperty(name = "govpay.metrics.batch.tag-nodo", havingValue = "true", matchIfMissing = true)
    public MeterFilter batchMetricsTagNodo(ObjectProvider<BatchJobProperties> batchJobProperties) {
        BatchJobProperties properties = batchJobProperties.getIfUnique();
        String clusterId = (properties != null) ? properties.getClusterId() : null;

        if (!StringUtils.hasText(clusterId)) {
            log.warn("Nessun clusterId univoco configurato: le metriche non avranno il tag {}", TAG_NODO);
            return new MeterFilter() { };
        }
        if (CLUSTER_ID_DEFAULT.equals(clusterId)) {
            log.warn("clusterId e' il default '{}': il tag {} non distingue i nodi del cluster", clusterId, TAG_NODO);
        }
        return MeterFilter.commonTags(Tags.of(TAG_NODO, clusterId));
    }
}
