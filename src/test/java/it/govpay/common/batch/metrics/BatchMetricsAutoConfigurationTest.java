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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import it.govpay.common.batch.CatturaLog;
import it.govpay.common.batch.config.BatchJobProperties;
import it.govpay.common.batch.config.ClusterIdPredefinito;

class BatchMetricsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BatchMetricsAutoConfiguration.class));

    private static BatchJobProperties clusterId(String clusterId) {
        BatchJobProperties properties = new BatchJobProperties();
        properties.setClusterId(clusterId);
        return properties;
    }

    /** Applica il filtro a un registry e restituisce il valore del tag nodo di un meter. */
    private static String tagNodo(MeterFilter filtro) {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        registry.config().meterFilter(filtro);
        return registry.counter("prova").getId().getTag(BatchMetricsAutoConfiguration.TAG_NODO);
    }

    @Test
    void attivaDiDefaultConMeterRegistryAncheSenzaWeb() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(BatchMetricsRecorder.class);
                    assertThat(context).hasSingleBean(BatchMetricsBeanPostProcessor.class);
                    assertThat(context).hasSingleBean(BatchMetricsInizializzatore.class);
                    assertThat(context).hasBean("batchMetricsTagNodo");
                });
    }

    @Test
    void spentaConEnabledFalse() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("govpay.metrics.batch.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(BatchMetricsRecorder.class));
    }

    @Test
    void spentaSenzaMeterRegistry() {
        runner.run(context -> assertThat(context).doesNotHaveBean(BatchMetricsRecorder.class));
    }

    @Test
    void spentaSenzaSpringBatch() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withClassLoader(new FilteredClassLoader(Job.class))
                .run(context -> assertThat(context).doesNotHaveBean(BatchMetricsRecorder.class));
    }

    @Test
    void tagNodoDisattivabile() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("govpay.metrics.batch.tag-nodo=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(BatchMetricsRecorder.class);
                    assertThat(context).doesNotHaveBean("batchMetricsTagNodo");
                    assertThat(context).doesNotHaveBean(BatchMetricsNodo.class);
                });
    }

    private static String tagNodoDelContesto(ApplicationContext context) {
        return tagNodo(context.getBean("batchMetricsTagNodo", MeterFilter.class));
    }

    @Test
    void tagNodoDallaPropertyDelClusterIdDeiProgetti() {
        // Come nei batch: cluster-id in govpay.batch.cluster-id, letto con @Value, senza BatchJobProperties
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("govpay.batch.cluster-id=rt-nodo-1")
                .run(context -> {
                    assertThat(tagNodoDelContesto(context)).isEqualTo("rt-nodo-1");
                    assertThat(context.getBean(BatchMetricsNodo.class).fonte()).isEqualTo("govpay.batch.cluster-id");
                });
    }

    @Test
    void tagNodoEsplicitoPrevale() {
        // Per i progetti che usano un'altra property per il cluster-id (es. ACA)
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("govpay.metrics.batch.nodo=aca-nodo-2", "govpay.batch.cluster-id=altro")
                .withBean(BatchJobProperties.class, () -> clusterId("nodo-bjp"))
                .run(context -> assertThat(tagNodoDelContesto(context)).isEqualTo("aca-nodo-2"));
    }

    @Test
    void tagNodoDaBatchJobPropertiesSeNonCeLaProperty() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(BatchJobProperties.class, () -> clusterId("nodo-2"))
                .run(context -> assertThat(tagNodoDelContesto(context)).isEqualTo("nodo-2"));
    }

    @Test
    void tagNodoDallHostSenzaAltreSorgenti() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> assertThat(tagNodoDelContesto(context)).isEqualTo(ClusterIdPredefinito.valore()));
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(BatchJobProperties.class, () -> clusterId(" "))
                .run(context -> assertThat(tagNodoDelContesto(context)).isEqualTo(ClusterIdPredefinito.valore()));
    }

    @Test
    void tagNodoDallHostConPiuBatchJobProperties() {
        // Con piu' bean non se ne sceglie uno a caso, e l'avvio non fallisce
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean("propertiesJobA", BatchJobProperties.class, () -> clusterId("nodo-a"))
                .withBean("propertiesJobB", BatchJobProperties.class, () -> clusterId("nodo-b"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(tagNodoDelContesto(context)).isEqualTo(ClusterIdPredefinito.valore());
                });
    }

    @Test
    void tagNodoUgualeSuTuttiINodiSegnalato() {
        try (CatturaLog log = CatturaLog.di(BatchMetricsAutoConfiguration.class)) {
            runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                    .withPropertyValues("govpay.batch.cluster-id=" + ClusterIdPredefinito.FALLBACK)
                    .run(context -> assertThat(tagNodoDelContesto(context)).isEqualTo(ClusterIdPredefinito.FALLBACK));

            assertThat(log.contiene("uguale su tutti i nodi")).isTrue();
        }
    }

    @Test
    void recorderPersonalizzatoPrevale() {
        BatchMetricsRecorder personalizzato = new BatchMetricsRecorder(new SimpleMeterRegistry(), List.of());
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withBean(BatchMetricsRecorder.class, () -> personalizzato)
                .run(context -> assertThat(context.getBean(BatchMetricsRecorder.class)).isSameAs(personalizzato));
    }

    @Test
    void bucketConfigurabili() {
        runner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("govpay.metrics.batch.duration-buckets=10s,1m",
                        "govpay.metrics.batch.istanze-ultimi-esiti=5")
                .run(context -> {
                    BatchMetricsProperties properties = context.getBean(BatchMetricsProperties.class);
                    assertThat(properties.getDurationBuckets()).hasSize(2);
                    assertThat(properties.getIstanzeUltimiEsiti()).isEqualTo(5);
                });
    }
}
