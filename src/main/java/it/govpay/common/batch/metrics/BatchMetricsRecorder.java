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

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.JobRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import it.govpay.common.batch.EsitoTick;
import it.govpay.common.batch.JobTickObserver;
import it.govpay.common.batch.TriggerType;
import it.govpay.common.batch.runner.JobExecutionHelper;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra le metriche di esecuzione dei job batch (#32).
 * <p>
 * Nomi Micrometer e serie Prometheus corrispondenti:
 * <ul>
 *   <li>{@value #METRICA_JOB_ATTIVO} ({@code govpay_batch_job_active_seconds_*}): esecuzioni in corso;</li>
 *   <li>{@value #METRICA_JOB} ({@code govpay_batch_job_seconds_*}): durata ed esito delle esecuzioni concluse;</li>
 *   <li>{@value #METRICA_ULTIMO_SUCCESSO} e {@value #METRICA_ULTIMO_FALLIMENTO}
 *       ({@code govpay_batch_job_last_*_timestamp_seconds}): istante dell'ultimo esito;</li>
 *   <li>{@value #METRICA_TICK} ({@code govpay_batch_schedule_ticks_total}): tentativi di avvio;</li>
 *   <li>{@value #METRICA_INTERVALLO} ({@code govpay_batch_schedule_interval_seconds}): intervallo previsto;</li>
 *   <li>{@value #METRICA_SOGLIA_STALE} ({@code govpay_batch_stale_threshold_seconds}): soglia di stale.</li>
 * </ul>
 * Il tag del nome del job e' {@value #TAG_JOB_NAME} e non {@code job}: Prometheus
 * aggiunge a ogni target l'etichetta {@code job} e rinominerebbe la nostra in
 * {@code exported_job}.
 */
@Slf4j
public class BatchMetricsRecorder implements JobTickObserver {

    public static final String METRICA_JOB = "govpay.batch.job";
    public static final String METRICA_JOB_ATTIVO = "govpay.batch.job.active";
    public static final String METRICA_ULTIMO_SUCCESSO = "govpay.batch.job.last.success.timestamp";
    public static final String METRICA_ULTIMO_FALLIMENTO = "govpay.batch.job.last.failure.timestamp";
    public static final String METRICA_TICK = "govpay.batch.schedule.ticks";
    public static final String METRICA_INTERVALLO = "govpay.batch.schedule.interval";
    public static final String METRICA_SOGLIA_STALE = "govpay.batch.stale.threshold";

    public static final String TAG_JOB_NAME = "job_name";
    public static final String TAG_TRIGGER = "trigger";
    public static final String TAG_ESITO = "esito";
    public static final String TAG_ESITO_TICK = "esito_tick";
    public static final String TAG_TIPO = "tipo";

    /** Valore del tag trigger per i job avviati senza passare da {@link JobExecutionHelper} */
    public static final String TRIGGER_SCONOSCIUTO = "UNKNOWN";

    private static final String UNITA_SECONDI = "seconds";

    private final MeterRegistry registry;
    private final Duration[] bucketDurata;
    private final Clock clock;

    private final Map<Long, EsecuzioneAttiva> attive = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ultimiSuccessi = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> ultimiFallimenti = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> intervalli = new ConcurrentHashMap<>();
    private final AtomicLong sogliaStale = new AtomicLong(-1);

    private record EsecuzioneAttiva(LongTaskTimer.Sample attivo, Timer.Sample durata, String trigger) { }

    /**
     * @param registry Registry su cui pubblicare le metriche
     * @param bucketDurata Bucket dell'istogramma della durata (service level objectives)
     */
    public BatchMetricsRecorder(MeterRegistry registry, List<Duration> bucketDurata) {
        this(registry, bucketDurata, Clock.systemDefaultZone());
    }

    /**
     * @param registry Registry su cui pubblicare le metriche
     * @param bucketDurata Bucket dell'istogramma della durata (service level objectives)
     * @param clock Orologio per gli istanti degli esiti; la sua zona interpreta i
     *              {@link LocalDateTime} di Spring Batch letti dal {@link JobRepository}
     */
    public BatchMetricsRecorder(MeterRegistry registry, List<Duration> bucketDurata, Clock clock) {
        this.registry = registry;
        this.bucketDurata = bucketDurata.toArray(Duration[]::new);
        this.clock = clock;
    }

    // ============ TICK ============

    @Override
    public void onTick(String jobName, TriggerType trigger, EsitoTick esito) {
        Counter.builder(METRICA_TICK)
                .description("Tentativi di avvio dei job batch, per esito del controllo pre-esecuzione")
                .tags(TAG_JOB_NAME, jobName, TAG_TRIGGER, trigger.name(), TAG_ESITO_TICK, esito.name())
                .register(registry)
                .increment();
    }

    // ============ ESECUZIONI ============

    /**
     * Registra l'inizio di un'esecuzione: la conta tra quelle attive e ne avvia la misura.
     *
     * @param jobExecution L'esecuzione che sta per iniziare
     */
    public void inizioJob(JobExecution jobExecution) {
        String jobName = jobName(jobExecution);
        String trigger = trigger(jobExecution);
        LongTaskTimer.Sample attivo = LongTaskTimer.builder(METRICA_JOB_ATTIVO)
                .description("Esecuzioni dei job batch in corso")
                .tags(TAG_JOB_NAME, jobName, TAG_TRIGGER, trigger)
                .register(registry)
                .start();
        attive.put(jobExecution.getId(), new EsecuzioneAttiva(attivo, Timer.start(registry), trigger));
    }

    /**
     * Registra la fine di un'esecuzione: durata ed esito, e istante dell'ultimo
     * successo o fallimento.
     *
     * @param jobExecution L'esecuzione appena terminata
     */
    public void fineJob(JobExecution jobExecution) {
        String jobName = jobName(jobExecution);
        BatchStatus status = jobExecution.getStatus();
        EsecuzioneAttiva attiva = attive.remove(jobExecution.getId());

        if (attiva != null) {
            attiva.attivo().stop();
            attiva.durata().stop(timer(jobName, attiva.trigger(), status));
        } else {
            // Esecuzione iniziata prima che il listener fosse registrato: si misura
            // dalle date dell'esecuzione, se ci sono.
            Duration durata = durata(jobExecution);
            if (durata != null) {
                timer(jobName, trigger(jobExecution), status).record(durata);
            }
        }

        if (status == BatchStatus.COMPLETED) {
            aggiornaIstante(ultimiSuccessi, METRICA_ULTIMO_SUCCESSO, jobName, clock.millis());
        } else if (status == BatchStatus.FAILED) {
            aggiornaIstante(ultimiFallimenti, METRICA_ULTIMO_FALLIMENTO, jobName, clock.millis());
        }
    }

    /**
     * Inizializza l'ultimo successo e l'ultimo fallimento di un job dalle sue
     * esecuzioni piu' recenti, cosi' che dopo un riavvio i gauge non ripartano vuoti.
     * <p>
     * Se nelle istanze esaminate non c'e' un esito, il gauge corrispondente non
     * viene registrato. Un errore nella lettura e' registrato a log e ignorato.
     *
     * @param jobName Nome del job
     * @param jobRepository Repository dei metadati Spring Batch
     * @param istanze Numero di istanze recenti da esaminare
     */
    public void inizializzaUltimiEsiti(String jobName, JobRepository jobRepository, int istanze) {
        try {
            for (JobInstance instance : jobRepository.getJobInstances(jobName, 0, istanze)) {
                for (JobExecution execution : jobRepository.getJobExecutions(instance)) {
                    inizializzaDa(jobName, execution);
                }
            }
        } catch (RuntimeException e) {
            log.warn("Impossibile leggere gli ultimi esiti del job {}: {}", jobName, e.getMessage(), e);
        }
    }

    private void inizializzaDa(String jobName, JobExecution execution) {
        LocalDateTime fine = execution.getEndTime();
        if (fine == null) {
            return;
        }
        long istante = fine.atZone(clock.getZone()).toInstant().toEpochMilli();
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            aggiornaIstante(ultimiSuccessi, METRICA_ULTIMO_SUCCESSO, jobName, istante);
        } else if (execution.getStatus() == BatchStatus.FAILED) {
            aggiornaIstante(ultimiFallimenti, METRICA_ULTIMO_FALLIMENTO, jobName, istante);
        }
    }

    // ============ CONFIGURAZIONE ============

    /**
     * Pubblica l'intervallo di schedulazione previsto per un job.
     *
     * @param jobName Nome del job
     * @param tipo Tipo di schedulazione ({@code fixedDelay}, {@code fixedRate}, {@code cron})
     * @param intervallo Intervallo previsto
     */
    public void registraIntervallo(String jobName, String tipo, Duration intervallo) {
        intervalli.computeIfAbsent(jobName + '|' + tipo, k -> {
            AtomicLong valore = new AtomicLong();
            Gauge.builder(METRICA_INTERVALLO, valore, BatchMetricsRecorder::secondi)
                    .description("Intervallo di schedulazione previsto dei job batch")
                    .baseUnit(UNITA_SECONDI)
                    .tags(TAG_JOB_NAME, jobName, TAG_TIPO, tipo)
                    .register(registry);
            return valore;
        }).set(intervallo.toMillis());
    }

    /**
     * Pubblica la soglia oltre la quale un'esecuzione e' considerata stale.
     *
     * @param minuti Soglia in minuti
     */
    public void registraSogliaStale(int minuti) {
        if (sogliaStale.getAndSet(Duration.ofMinutes(minuti).toMillis()) < 0) {
            Gauge.builder(METRICA_SOGLIA_STALE, sogliaStale, BatchMetricsRecorder::secondi)
                    .description("Soglia oltre la quale un'esecuzione dei job batch e' considerata stale")
                    .baseUnit(UNITA_SECONDI)
                    .register(registry);
        }
    }

    // ============ UTILITA' ============

    private Timer timer(String jobName, String trigger, BatchStatus status) {
        return Timer.builder(METRICA_JOB)
                .description("Durata delle esecuzioni dei job batch, per esito")
                .tags(Tags.of(TAG_JOB_NAME, jobName, TAG_TRIGGER, trigger, TAG_ESITO, status.name()))
                .serviceLevelObjectives(bucketDurata)
                .register(registry);
    }

    private void aggiornaIstante(Map<String, AtomicLong> istanti, String metrica, String jobName, long epochMillis) {
        // Il gauge nasce gia' col primo valore: uno scrape non deve mai leggere un istante fittizio
        istanti.computeIfAbsent(jobName, k -> {
            AtomicLong valore = new AtomicLong(epochMillis);
            Gauge.builder(metrica, valore, BatchMetricsRecorder::secondi)
                    .description("Istante dell'ultimo esito dei job batch")
                    .baseUnit(UNITA_SECONDI)
                    .tags(TAG_JOB_NAME, jobName)
                    .register(registry);
            return valore;
        }).accumulateAndGet(epochMillis, Math::max);
    }

    private static double secondi(AtomicLong millis) {
        return millis.get() / 1000.0;
    }

    private static String jobName(JobExecution jobExecution) {
        return jobExecution.getJobInstance().getJobName();
    }

    /**
     * Il trigger e' letto dal parametro del job; un valore assente o non
     * riconosciuto diventa {@value #TRIGGER_SCONOSCIUTO}, cosi' che la
     * cardinalita' del tag resti chiusa.
     */
    private static String trigger(JobExecution jobExecution) {
        String valore = jobExecution.getJobParameters().getString(JobExecutionHelper.JOB_PARAM_TRIGGER_TYPE);
        for (TriggerType tipo : TriggerType.values()) {
            if (tipo.name().equals(valore)) {
                return valore;
            }
        }
        return TRIGGER_SCONOSCIUTO;
    }

    /**
     * Le date di Spring Batch sono {@link LocalDateTime} nella zona di sistema: vanno
     * riportate a istanti, altrimenti un'esecuzione a cavallo del cambio dell'ora
     * risulterebbe piu' lunga o piu' corta di un'ora.
     */
    private Duration durata(JobExecution jobExecution) {
        LocalDateTime inizio = jobExecution.getStartTime();
        LocalDateTime fine = jobExecution.getEndTime();
        if (inizio == null || fine == null) {
            return null;
        }
        return Duration.between(inizio.atZone(clock.getZone()), fine.atZone(clock.getZone()));
    }
}
