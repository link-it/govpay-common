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

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.batch.autoconfigure.BatchAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.BatchTaskExecutor;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Rende persistenti i metadati di Spring Batch su database.
 * <p>
 * Senza questa configurazione {@code BatchAutoConfiguration} di Spring Boot 4
 * estende {@link DefaultBatchConfiguration}, il cui {@code jobRepository()}
 * restituisce un {@code ResourcelessJobRepository}: i job arrivano a COMPLETED
 * senza scrivere una riga nelle tabelle {@code BATCH_*}. Di conseguenza restano
 * senza effetto la guardia anti-concorrenza fra nodi
 * ({@link it.govpay.common.batch.service.JobConcurrencyService}), la soglia
 * {@code govpay.batch.stale-threshold-minutes}, gli endpoint di console di
 * {@link it.govpay.common.batch.controller.AbstractBatchController} e lo
 * svecchiamento in {@code /opt/sql/cleanup}.
 * <p>
 * Il meccanismo di sostituzione e' quello previsto da Spring Boot:
 * {@code BatchAutoConfiguration} e' annotata
 * {@code @ConditionalOnMissingBean(DefaultBatchConfiguration.class)}, quindi
 * dichiarando questa classe - registrata prima - la sua si disattiva.
 * <p>
 * <strong>Transaction manager dedicato.</strong> Il {@code JobRepository} usa un
 * {@link JdbcTransactionManager} costruito qui sul {@code DataSource}, non il
 * {@code JpaTransactionManager} dell'applicazione. Non e' una scorciatoia: in
 * Spring Boot 4 dipendere dal {@code PlatformTransactionManager} in questa fase
 * innesca il ciclo {@code entityManagerFactory -> configurazione batch ->
 * transactionManager -> entityManagerFactory}, lo stesso che i progetti batch
 * aggirano tenendo i propri bean in configurazioni prive di quella dipendenza.
 * Spring Boot prevede esplicitamente questo scenario con il qualificatore
 * {@code @BatchTransactionManager}. Conseguenza da conoscere: la contabilita'
 * dei metadati committa in transazione propria, separata da quella dei dati
 * applicativi degli step, che continuano a usare il transaction manager JPA
 * passato agli {@code StepBuilder}.
 * <p>
 * Le istanze di job restano distinte a ogni lancio perche'
 * {@link it.govpay.common.batch.runner.JobExecutionHelper} include fra i
 * parametri l'istante di avvio: non si incorre in
 * {@code JobInstanceAlreadyCompleteException}, ma le righe crescono a ogni
 * esecuzione ed e' lo svecchiamento periodico a tenerle sotto controllo.
 * <p>
 * Si disattiva con {@code govpay.batch.job-repository.jdbc.enabled=false},
 * tornando al comportamento non persistente di Spring Boot.
 */
/*
 * Ordinamento: DOPO DataSourceAutoConfiguration, altrimenti
 * @ConditionalOnBean(DataSource.class) viene valutata quando il DataSource non
 * e' ancora registrato e questa configurazione viene scartata in silenzio;
 * PRIMA di BatchAutoConfiguration, perche' e' la registrazione di questo bean
 * DefaultBatchConfiguration a farla disattivare.
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class, before = BatchAutoConfiguration.class)
@ConditionalOnClass({ JobRepository.class, JdbcJobRepositoryFactoryBean.class, DataSource.class })
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(name = "govpay.batch.job-repository.jdbc.enabled", matchIfMissing = true)
public class BatchJobRepositoryAutoConfiguration extends DefaultBatchConfiguration {

    private final DataSource dataSource;
    private final JdbcTransactionManager batchTransactionManager;
    private final TaskExecutor taskExecutor;
    private final JobParametersConverter jobParametersConverter;

    public BatchJobRepositoryAutoConfiguration(DataSource dataSource,
            @BatchTaskExecutor ObjectProvider<TaskExecutor> batchTaskExecutor,
            ObjectProvider<JobParametersConverter> jobParametersConverter) {
        this.dataSource = dataSource;
        this.batchTransactionManager = new JdbcTransactionManager(dataSource);
        this.taskExecutor = batchTaskExecutor.getIfAvailable();
        this.jobParametersConverter = jobParametersConverter.getIfAvailable();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sostituisce il {@code ResourcelessJobRepository} con quello JDBC. Prefisso
     * delle tabelle, nomi delle sequence e livello di isolamento restano quelli
     * di default di Spring Batch 6: corrispondono allo schema upstream che le
     * immagini installano in {@code /opt/sql/spring-batch}.
     */
    @Override
    public JobRepository jobRepository() {
        JdbcJobRepositoryFactoryBean factory = new JdbcJobRepositoryFactoryBean();
        factory.setDataSource(this.dataSource);
        factory.setTransactionManager(this.batchTransactionManager);
        try {
            factory.afterPropertiesSet();
            return factory.getObject();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Impossibile creare il JobRepository JDBC per i metadati Spring Batch", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allinea il {@code JobOperator} allo stesso transaction manager del
     * {@code JobRepository}: opera sulle stesse tabelle.
     */
    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return this.batchTransactionManager;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sostituendo {@code BatchAutoConfiguration} si perderebbero i due agganci
     * che la sua configurazione interna offriva: questo, che adotta un
     * {@code TaskExecutor} qualificato {@code @BatchTaskExecutor} se presente,
     * e {@link #getJobParametersConverter()}. Sono riproposti qui perche' la
     * sostituzione riguardi soltanto il {@code JobRepository}.
     */
    @Override
    protected TaskExecutor getTaskExecutor() {
        return (this.taskExecutor != null) ? this.taskExecutor : super.getTaskExecutor();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Vedi {@link #getTaskExecutor()}: aggancio ripreso da
     * {@code BatchAutoConfiguration}. Deprecato a monte in Spring Boot 4.0,
     * mantenuto finche' lo e' anche la classe base.
     */
    @Override
    @Deprecated(since = "4.0.0", forRemoval = true)
    @SuppressWarnings("removal")
    protected JobParametersConverter getJobParametersConverter() {
        return (this.jobParametersConverter != null) ? this.jobParametersConverter
                : super.getJobParametersConverter();
    }
}
