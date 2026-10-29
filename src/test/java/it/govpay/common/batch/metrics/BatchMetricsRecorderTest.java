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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import it.govpay.common.batch.EsitoTick;
import it.govpay.common.batch.TriggerType;
import it.govpay.common.batch.runner.JobExecutionHelper;

class BatchMetricsRecorderTest {

    private static final String JOB_NAME = "rtJob";
    private static final ZoneId ZONE = ZoneId.of("Europe/Rome");
    private static final Instant ORA = Instant.parse("2026-10-28T10:00:00Z");
    private static final List<Duration> BUCKET = List.of(
            Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(30),
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofMinutes(30),
            Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(4));

    private MeterRegistry registry;
    private BatchMetricsRecorder recorder;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        recorder = new BatchMetricsRecorder(registry, BUCKET, Clock.fixed(ORA, ZONE));
    }

    private static JobExecution esecuzione(long id, String trigger) {
        JobParametersBuilder params = new JobParametersBuilder();
        if (trigger != null) {
            params.addString(JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE, trigger);
        }
        return new JobExecution(id, new JobInstance(id, JOB_NAME), params.toJobParameters());
    }

    private Timer timer(String trigger, String esito) {
        return registry.find(BatchMetricsRecorder.METRICA_JOB)
                .tags(BatchMetricsRecorder.TAG_JOB_NAME, JOB_NAME,
                        BatchMetricsRecorder.TAG_TRIGGER, trigger,
                        BatchMetricsRecorder.TAG_ESITO, esito)
                .timer();
    }

    private Double gauge(String metrica) {
        Gauge g = registry.find(metrica).tag(BatchMetricsRecorder.TAG_JOB_NAME, JOB_NAME).gauge();
        return g == null ? null : g.value();
    }

    @Nested
    @DisplayName("Tick")
    class Tick {

        @Test
        @DisplayName("Ogni tick incrementa il contatore con job_name, trigger ed esito_tick")
        void contaTick() {
            recorder.onTick(JOB_NAME, TriggerType.SCHEDULED, EsitoTick.RUNNING_ON_OTHER_NODE);
            recorder.onTick(JOB_NAME, TriggerType.SCHEDULED, EsitoTick.RUNNING_ON_OTHER_NODE);
            recorder.onTick(JOB_NAME, TriggerType.MANUAL, EsitoTick.FORZATO);

            assertEquals(2.0, registry.get(BatchMetricsRecorder.METRICA_TICK)
                    .tags(BatchMetricsRecorder.TAG_JOB_NAME, JOB_NAME,
                            BatchMetricsRecorder.TAG_TRIGGER, "SCHEDULED",
                            BatchMetricsRecorder.TAG_ESITO_TICK, "RUNNING_ON_OTHER_NODE")
                    .counter().count());
            assertEquals(1.0, registry.get(BatchMetricsRecorder.METRICA_TICK)
                    .tags(BatchMetricsRecorder.TAG_TRIGGER, "MANUAL",
                            BatchMetricsRecorder.TAG_ESITO_TICK, "FORZATO")
                    .counter().count());
        }
    }

    @Nested
    @DisplayName("Esecuzioni")
    class Esecuzioni {

        @Test
        @DisplayName("Un'esecuzione e' attiva tra inizio e fine, poi finisce nel timer col suo esito")
        void attivaPoiConclusa() {
            JobExecution exec = esecuzione(1L, "SCHEDULED");

            recorder.inizioJob(exec);
            LongTaskTimer attivo = registry.get(BatchMetricsRecorder.METRICA_JOB_ATTIVO)
                    .tags(BatchMetricsRecorder.TAG_JOB_NAME, JOB_NAME, BatchMetricsRecorder.TAG_TRIGGER, "SCHEDULED")
                    .longTaskTimer();
            assertEquals(1, attivo.activeTasks());

            exec.setStatus(BatchStatus.COMPLETED);
            recorder.fineJob(exec);

            assertEquals(0, attivo.activeTasks());
            assertEquals(1, timer("SCHEDULED", "COMPLETED").count());
        }

        @Test
        @DisplayName("Il timer pubblica un bucket per ogni soglia configurata")
        void bucketDellaDurata() {
            JobExecution exec = esecuzione(1L, "SCHEDULED");
            recorder.inizioJob(exec);
            exec.setStatus(BatchStatus.FAILED);
            recorder.fineJob(exec);

            assertEquals(BUCKET.size(), timer("SCHEDULED", "FAILED").takeSnapshot().histogramCounts().length);
        }

        @Test
        @DisplayName("Il trigger MANUAL e' riportato nei tag")
        void triggerManuale() {
            JobExecution exec = esecuzione(1L, "MANUAL");
            recorder.inizioJob(exec);
            exec.setStatus(BatchStatus.STOPPED);
            recorder.fineJob(exec);

            assertEquals(1, timer("MANUAL", "STOPPED").count());
        }

        @Test
        @DisplayName("Trigger assente o non riconosciuto diventa UNKNOWN")
        void triggerSconosciuto() {
            JobExecution senza = esecuzione(1L, null);
            JobExecution invalido = esecuzione(2L, "dal-cron-di-sistema");
            for (JobExecution exec : List.of(senza, invalido)) {
                recorder.inizioJob(exec);
                exec.setStatus(BatchStatus.COMPLETED);
                recorder.fineJob(exec);
            }

            assertEquals(2, timer(BatchMetricsRecorder.TRIGGER_SCONOSCIUTO, "COMPLETED").count());
        }

        @Test
        @DisplayName("Fine senza inizio: la durata si ricava dalle date dell'esecuzione")
        void fineSenzaInizioConDate() {
            JobExecution exec = esecuzione(1L, "SCHEDULED");
            exec.setStartTime(LocalDateTime.of(2026, 10, 28, 9, 0));
            exec.setEndTime(LocalDateTime.of(2026, 10, 28, 9, 45));
            exec.setStatus(BatchStatus.COMPLETED);

            recorder.fineJob(exec);

            assertEquals(45 * 60.0, timer("SCHEDULED", "COMPLETED").totalTime(TimeUnit.SECONDS), 0.001);
        }

        @Test
        @DisplayName("Fine senza inizio a cavallo del cambio dell'ora: la durata e' quella reale")
        void fineSenzaInizioACavalloDelCambioOra() {
            // In Europe/Rome il 25/10/2026 alle 03:00 si torna alle 02:00: dalle 01:30
            // alle 03:30 passano 3 ore, non 2
            JobExecution exec = esecuzione(1L, "SCHEDULED");
            exec.setStartTime(LocalDateTime.of(2026, 10, 25, 1, 30));
            exec.setEndTime(LocalDateTime.of(2026, 10, 25, 3, 30));
            exec.setStatus(BatchStatus.COMPLETED);

            recorder.fineJob(exec);

            assertEquals(3 * 3600.0, timer("SCHEDULED", "COMPLETED").totalTime(TimeUnit.SECONDS), 0.001);
        }

        @Test
        @DisplayName("Fine senza inizio, con la sola data di inizio: nessuna durata")
        void fineSenzaInizioSoloDataInizio() {
            JobExecution exec = esecuzione(1L, "SCHEDULED");
            exec.setStartTime(LocalDateTime.of(2026, 10, 28, 9, 0));
            exec.setStatus(BatchStatus.FAILED);

            recorder.fineJob(exec);

            assertNull(timer("SCHEDULED", "FAILED"));
        }

        @Test
        @DisplayName("Fine senza inizio e senza date: nessuna durata, ma l'esito e' registrato")
        void fineSenzaInizioNeDate() {
            JobExecution exec = esecuzione(1L, "SCHEDULED");
            exec.setStatus(BatchStatus.COMPLETED);

            recorder.fineJob(exec);

            assertNull(timer("SCHEDULED", "COMPLETED"));
            assertEquals(ORA.getEpochSecond(), gauge(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO), 0.001);
        }
    }

    @Nested
    @DisplayName("Ultimi esiti")
    class UltimiEsiti {

        @Test
        @DisplayName("COMPLETED aggiorna l'ultimo successo, FAILED l'ultimo fallimento")
        void successoEFallimento() {
            JobExecution ok = esecuzione(1L, "SCHEDULED");
            ok.setStatus(BatchStatus.COMPLETED);
            recorder.fineJob(ok);
            assertEquals(ORA.getEpochSecond(), gauge(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO), 0.001);
            assertNull(gauge(BatchMetricsRecorder.METRICA_ULTIMO_FALLIMENTO));

            JobExecution ko = esecuzione(2L, "SCHEDULED");
            ko.setStatus(BatchStatus.FAILED);
            recorder.fineJob(ko);
            assertEquals(ORA.getEpochSecond(), gauge(BatchMetricsRecorder.METRICA_ULTIMO_FALLIMENTO), 0.001);
        }

        @Test
        @DisplayName("STOPPED non aggiorna nessuno dei due istanti")
        void stoppedNonConta() {
            JobExecution exec = esecuzione(1L, "SCHEDULED");
            exec.setStatus(BatchStatus.STOPPED);
            recorder.fineJob(exec);

            assertNull(gauge(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO));
            assertNull(gauge(BatchMetricsRecorder.METRICA_ULTIMO_FALLIMENTO));
        }

        @Test
        @DisplayName("All'avvio gli istanti si leggono dalle esecuzioni piu' recenti del JobRepository")
        void inizializzaDalRepository() {
            JobRepository repo = mock(JobRepository.class);
            JobInstance recente = new JobInstance(2L, JOB_NAME);
            JobInstance vecchia = new JobInstance(1L, JOB_NAME);
            when(repo.getJobInstances(JOB_NAME, 0, 20)).thenReturn(List.of(recente, vecchia));
            when(repo.getJobExecutions(recente)).thenReturn(List.of(
                    conclusa(20L, BatchStatus.FAILED, LocalDateTime.of(2026, 10, 28, 8, 0)),
                    conclusa(21L, BatchStatus.STARTED, null),
                    conclusa(22L, BatchStatus.STOPPED, LocalDateTime.of(2026, 10, 28, 9, 0))));
            when(repo.getJobExecutions(vecchia)).thenReturn(List.of(
                    conclusa(10L, BatchStatus.COMPLETED, LocalDateTime.of(2026, 10, 28, 6, 0)),
                    conclusa(11L, BatchStatus.COMPLETED, LocalDateTime.of(2026, 10, 27, 6, 0))));

            recorder.inizializzaUltimiEsiti(JOB_NAME, repo, 20);

            assertEquals(LocalDateTime.of(2026, 10, 28, 6, 0).atZone(ZONE).toEpochSecond(),
                    gauge(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO), 0.001);
            assertEquals(LocalDateTime.of(2026, 10, 28, 8, 0).atZone(ZONE).toEpochSecond(),
                    gauge(BatchMetricsRecorder.METRICA_ULTIMO_FALLIMENTO), 0.001);
        }

        @Test
        @DisplayName("Un istante piu' vecchio non sovrascrive quello piu' recente")
        void siTieneIlPiuRecente() {
            JobExecution ok = esecuzione(1L, "SCHEDULED");
            ok.setStatus(BatchStatus.COMPLETED);
            recorder.fineJob(ok);

            JobRepository repo = mock(JobRepository.class);
            JobInstance vecchia = new JobInstance(1L, JOB_NAME);
            when(repo.getJobInstances(JOB_NAME, 0, 5)).thenReturn(List.of(vecchia));
            when(repo.getJobExecutions(vecchia)).thenReturn(List.of(
                    conclusa(10L, BatchStatus.COMPLETED, LocalDateTime.of(2026, 1, 1, 0, 0))));
            recorder.inizializzaUltimiEsiti(JOB_NAME, repo, 5);

            assertEquals(ORA.getEpochSecond(), gauge(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO), 0.001);
        }

        @Test
        @DisplayName("Nessuna esecuzione conclusa: i gauge non vengono registrati")
        void nessunEsito() {
            JobRepository repo = mock(JobRepository.class);
            when(repo.getJobInstances(JOB_NAME, 0, 20)).thenReturn(List.of());

            recorder.inizializzaUltimiEsiti(JOB_NAME, repo, 20);

            assertNull(gauge(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO));
            assertNull(gauge(BatchMetricsRecorder.METRICA_ULTIMO_FALLIMENTO));
        }

        @Test
        @DisplayName("Un errore del JobRepository non si propaga")
        void erroreDelRepository() {
            JobRepository repo = mock(JobRepository.class);
            when(repo.getJobInstances(JOB_NAME, 0, 20)).thenThrow(new IllegalStateException("database non raggiungibile"));

            assertDoesNotThrow(() -> recorder.inizializzaUltimiEsiti(JOB_NAME, repo, 20));
            assertNull(gauge(BatchMetricsRecorder.METRICA_ULTIMO_SUCCESSO));
        }

        private JobExecution conclusa(long id, BatchStatus status, LocalDateTime fine) {
            JobExecution exec = new JobExecution(id, new JobInstance(id, JOB_NAME), new JobParameters());
            exec.setStatus(status);
            exec.setEndTime(fine);
            return exec;
        }
    }

    @Nested
    @DisplayName("Configurazione")
    class Configurazione {

        @Test
        @DisplayName("L'intervallo e' pubblicato in secondi, per job e tipo, e si aggiorna")
        void intervallo() {
            recorder.registraIntervallo(JOB_NAME, "fixedDelay", Duration.ofHours(2));
            recorder.registraIntervallo(JOB_NAME, "fixedDelay", Duration.ofMinutes(10));

            Gauge g = registry.get(BatchMetricsRecorder.METRICA_INTERVALLO)
                    .tags(BatchMetricsRecorder.TAG_JOB_NAME, JOB_NAME, BatchMetricsRecorder.TAG_TIPO, "fixedDelay")
                    .gauge();
            assertEquals(600.0, g.value(), 0.001);
            assertEquals(1, registry.find(BatchMetricsRecorder.METRICA_INTERVALLO).gauges().size());
        }

        @Test
        @DisplayName("La soglia di stale e' un solo gauge, in secondi, che si aggiorna")
        void sogliaStale() {
            recorder.registraSogliaStale(120);
            recorder.registraSogliaStale(90);

            assertEquals(1, registry.find(BatchMetricsRecorder.METRICA_SOGLIA_STALE).gauges().size());
            assertEquals(90 * 60.0, registry.get(BatchMetricsRecorder.METRICA_SOGLIA_STALE).gauge().value(), 0.001);
        }
    }

    @Test
    @DisplayName("Su Prometheus le serie hanno i nomi documentati nella #31/#32")
    void nomiPrometheus() {
        PrometheusMeterRegistry prometheus = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        BatchMetricsRecorder rec = new BatchMetricsRecorder(prometheus, BUCKET, Clock.fixed(ORA, ZONE));

        rec.onTick(JOB_NAME, TriggerType.SCHEDULED, EsitoTick.CAN_PROCEED);
        JobExecution ok = esecuzione(1L, "SCHEDULED");
        rec.inizioJob(ok);
        ok.setStatus(BatchStatus.COMPLETED);
        rec.fineJob(ok);
        JobExecution ko = esecuzione(2L, "SCHEDULED");
        ko.setStatus(BatchStatus.FAILED);
        rec.fineJob(ko);
        rec.registraIntervallo(JOB_NAME, "fixedDelay", Duration.ofHours(2));
        rec.registraSogliaStale(120);

        String scrape = prometheus.scrape();

        // Il long task timer esce come summary: _count sono le esecuzioni attive

        assertTrue(scrape.contains("govpay_batch_schedule_ticks_total{esito_tick=\"CAN_PROCEED\",job_name=\"rtJob\",trigger=\"SCHEDULED\"} 1.0"), scrape);
        assertTrue(scrape.contains("govpay_batch_job_seconds_bucket{esito=\"COMPLETED\",job_name=\"rtJob\",trigger=\"SCHEDULED\",le=\"7200.0\"}"), scrape);
        assertTrue(scrape.contains("govpay_batch_job_seconds_count{esito=\"COMPLETED\",job_name=\"rtJob\",trigger=\"SCHEDULED\"} 1"), scrape);
        assertTrue(scrape.contains("govpay_batch_job_active_seconds_count{job_name=\"rtJob\",trigger=\"SCHEDULED\"} 0"), scrape);
        assertTrue(scrape.contains("govpay_batch_job_last_success_timestamp_seconds{job_name=\"rtJob\"}"), scrape);
        assertTrue(scrape.contains("govpay_batch_job_last_failure_timestamp_seconds{job_name=\"rtJob\"}"), scrape);
        assertTrue(scrape.contains("govpay_batch_schedule_interval_seconds{job_name=\"rtJob\",tipo=\"fixedDelay\"} 7200.0"), scrape);
        assertTrue(scrape.contains("govpay_batch_stale_threshold_seconds 7200.0"), scrape);
    }
}
