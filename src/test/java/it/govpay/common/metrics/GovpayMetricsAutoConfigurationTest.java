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
package it.govpay.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class GovpayMetricsAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GovpayMetricsAutoConfiguration.class));

    @Test
    void activatesWithMeterRegistryInServletWebApplication() {
        webRunner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(ExternalCallMetricsRecorder.class);
                    assertThat(context).hasBean("apiTimingMetricsFilterRegistration");
                    assertThat(context).hasBean("requestContextFilterRegistration");
                    assertThat(context).hasBean("externalCallMetricsContext");
                });
    }

    @Test
    void backsOffWithoutMeterRegistry() {
        webRunner.run(context ->
                assertThat(context).doesNotHaveBean(ExternalCallMetricsRecorder.class));
    }

    @Test
    void backsOffWhenDisabledByProperty() {
        webRunner.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("govpay.metrics.enabled=false")
                .run(context ->
                        assertThat(context).doesNotHaveBean(ExternalCallMetricsRecorder.class));
    }

    @Test
    void backsOffInNonWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(GovpayMetricsAutoConfiguration.class))
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context ->
                        assertThat(context).doesNotHaveBean(ExternalCallMetricsRecorder.class));
    }
}
