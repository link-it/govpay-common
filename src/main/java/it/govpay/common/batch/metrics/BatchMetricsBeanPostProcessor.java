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

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;

import it.govpay.common.batch.runner.JobExecutionHelper;
import it.govpay.common.batch.service.JobConcurrencyService;

/**
 * Aggancia le metriche ai bean batch dei progetti, senza che questi debbano
 * essere modificati:
 * <ul>
 *   <li>registra un {@link BatchMetricsJobListener} su ogni {@link AbstractJob};</li>
 *   <li>imposta {@link BatchMetricsRecorder} come osservatore dei tick di ogni
 *       {@link JobExecutionHelper}, e ne pubblica la soglia di stale.</li>
 * </ul>
 * Il recorder e' risolto in modo lazy tramite {@link ObjectProvider}: un
 * {@code BeanPostProcessor} con dipendenze dirette le inizializzerebbe prima
 * degli altri post-processor.
 */
public class BatchMetricsBeanPostProcessor implements BeanPostProcessor {

    private final ObjectProvider<BatchMetricsRecorder> recorder;

    public BatchMetricsBeanPostProcessor(ObjectProvider<BatchMetricsRecorder> recorder) {
        this.recorder = recorder;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Object target = AopProxyUtils.getSingletonTarget(bean);
        if (target == null) {
            target = bean;
        }

        if (target instanceof AbstractJob job) {
            job.registerJobExecutionListener(new BatchMetricsJobListener(recorder.getObject()));
        } else if (target instanceof JobExecutionHelper helper) {
            BatchMetricsRecorder metriche = recorder.getObject();
            helper.setTickObserver(metriche);
            JobConcurrencyService concurrencyService = helper.getJobConcurrencyService();
            if (concurrencyService != null) {
                metriche.registraSogliaStale(concurrencyService.getStaleThresholdMinutes());
            }
        }
        return bean;
    }
}
