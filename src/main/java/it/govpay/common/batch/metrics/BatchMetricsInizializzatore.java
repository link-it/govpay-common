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
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.util.StringValueResolver;

import it.govpay.common.batch.runner.AbstractScheduledJobRunner;
import it.govpay.common.batch.runner.IntervalloSchedulazioneResolver;

/**
 * A contesto pronto pubblica i valori che non dipendono dalle esecuzioni:
 * l'intervallo previsto di ogni {@link AbstractScheduledJobRunner} e l'ultimo
 * successo e fallimento di ogni {@link Job}, letti dal {@link JobRepository}.
 */
public class BatchMetricsInizializzatore implements SmartInitializingSingleton {

    private final BatchMetricsRecorder recorder;
    private final ConfigurableListableBeanFactory beanFactory;
    private final ObjectProvider<JobRepository> jobRepository;
    private final int istanzeUltimiEsiti;

    public BatchMetricsInizializzatore(BatchMetricsRecorder recorder, ConfigurableListableBeanFactory beanFactory,
            ObjectProvider<JobRepository> jobRepository, int istanzeUltimiEsiti) {
        this.recorder = recorder;
        this.beanFactory = beanFactory;
        this.jobRepository = jobRepository;
        this.istanzeUltimiEsiti = istanzeUltimiEsiti;
    }

    @Override
    public void afterSingletonsInstantiated() {
        StringValueResolver resolver = new EmbeddedValueResolver(beanFactory);
        beanFactory.getBeanProvider(AbstractScheduledJobRunner.class).orderedStream()
                .forEach(runner -> IntervalloSchedulazioneResolver.risolvi(runner, resolver)
                        .ifPresent(i -> recorder.registraIntervallo(i.jobName(), i.tipo(), i.intervallo())));

        JobRepository repository = jobRepository.getIfAvailable();
        if (repository != null) {
            beanFactory.getBeanProvider(Job.class).orderedStream()
                    .map(Job::getName)
                    .distinct()
                    .forEach(jobName -> recorder.inizializzaUltimiEsiti(jobName, repository, istanzeUltimiEsiti));
        }
    }
}
