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
package it.govpay.common.batch.runner;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.format.annotation.DurationFormat;
import org.springframework.format.datetime.standard.DurationFormatterUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import lombok.extern.slf4j.Slf4j;

/**
 * Ricava l'intervallo di schedulazione previsto di un
 * {@link AbstractScheduledJobRunner} dalla sua annotazione {@link Scheduled}.
 * <p>
 * Non si usa {@code ScheduledTaskHolder}: in Spring 7 il runnable dei task e'
 * avvolto in un {@code Task.OutcomeTrackingRunnable} privato e il bean che lo
 * dichiara non e' raggiungibile. Leggere l'annotazione con le stesse regole
 * dello scheduler (placeholder, {@code timeUnit}, formati di durata) da' lo
 * stesso risultato senza dipendere da dettagli interni.
 * <p>
 * Sta in questo package perche' il nome del job del runner e' {@code protected}.
 */
@Slf4j
public final class IntervalloSchedulazioneResolver {

    public static final String TIPO_FIXED_DELAY = "fixedDelay";
    public static final String TIPO_FIXED_RATE = "fixedRate";
    public static final String TIPO_CRON = "cron";

    /**
     * Intervallo previsto di un job schedulato.
     *
     * @param jobName Nome del job
     * @param tipo Tipo di schedulazione ({@value #TIPO_FIXED_DELAY}, {@value #TIPO_FIXED_RATE}, {@value #TIPO_CRON})
     * @param intervallo Intervallo previsto; per {@value #TIPO_CRON} e' la distanza tra le due prossime esecuzioni
     */
    public record IntervalloSchedulazione(String jobName, String tipo, Duration intervallo) { }

    private IntervalloSchedulazioneResolver() {
        // Utility class
    }

    /**
     * Ricava l'intervallo previsto del runner.
     *
     * @param runner Il runner schedulato (anche proxy)
     * @param resolver Risolutore dei placeholder dei valori dell'annotazione
     * @return l'intervallo, o vuoto se il runner non ha un metodo {@code @Scheduled}
     *         utilizzabile
     */
    public static Optional<IntervalloSchedulazione> risolvi(AbstractScheduledJobRunner runner, StringValueResolver resolver) {
        return risolvi(runner, resolver, Clock.systemDefaultZone());
    }

    /**
     * Come {@link #risolvi(AbstractScheduledJobRunner, StringValueResolver)}, con
     * l'orologio da cui calcolare le prossime esecuzioni di un {@code cron}.
     *
     * @param runner Il runner schedulato (anche proxy)
     * @param resolver Risolutore dei placeholder dei valori dell'annotazione
     * @param clock Orologio e zona di default per le espressioni cron
     * @return l'intervallo, o vuoto se il runner non ha un metodo {@code @Scheduled}
     *         utilizzabile
     */
    public static Optional<IntervalloSchedulazione> risolvi(AbstractScheduledJobRunner runner,
            StringValueResolver resolver, Clock clock) {
        String jobName = runner.getJobName();
        List<Scheduled> annotazioni = annotazioni(AopUtils.getTargetClass(runner));

        if (annotazioni.isEmpty()) {
            log.debug("Il runner del job {} non ha metodi @Scheduled: intervallo non pubblicato", jobName);
            return Optional.empty();
        }
        if (annotazioni.size() > 1) {
            log.warn("Il runner del job {} ha {} schedulazioni: per l'intervallo si usa la prima",
                    jobName, annotazioni.size());
        }

        try {
            return intervallo(annotazioni.get(0), resolver, clock)
                    .map(tipoEDurata -> new IntervalloSchedulazione(jobName, tipoEDurata.tipo(), tipoEDurata.durata()));
        } catch (IllegalArgumentException | IllegalStateException | DateTimeException e) {
            log.warn("Intervallo di schedulazione del job {} non interpretabile: {}", jobName, e.getMessage());
            return Optional.empty();
        }
    }

    private record TipoEDurata(String tipo, Duration durata) { }

    /** Annotazioni dei metodi della classe, in ordine di nome del metodo per un risultato stabile. */
    private static List<Scheduled> annotazioni(Class<?> classe) {
        Map<Method, Set<Scheduled>> metodi = MethodIntrospector.selectMethods(classe,
                (MethodIntrospector.MetadataLookup<Set<Scheduled>>) metodo -> {
                    Set<Scheduled> trovate = AnnotatedElementUtils.getMergedRepeatableAnnotations(
                            metodo, Scheduled.class, Schedules.class);
                    return trovate.isEmpty() ? null : trovate;
                });
        return metodi.entrySet().stream()
                .sorted(Comparator.comparing(voce -> voce.getKey().getName()))
                .flatMap(voce -> voce.getValue().stream())
                .toList();
    }

    private static Optional<TipoEDurata> intervallo(Scheduled scheduled, StringValueResolver resolver, Clock clock) {
        String cron = valore(scheduled.cron(), resolver);
        if (StringUtils.hasText(cron) && !Scheduled.CRON_DISABLED.equals(cron)) {
            return intervalloCron(cron, valore(scheduled.zone(), resolver), clock);
        }

        Duration delay = durata(scheduled.fixedDelay(), scheduled.fixedDelayString(), scheduled.timeUnit(), resolver);
        if (delay != null) {
            return Optional.of(new TipoEDurata(TIPO_FIXED_DELAY, delay));
        }

        Duration rate = durata(scheduled.fixedRate(), scheduled.fixedRateString(), scheduled.timeUnit(), resolver);
        if (rate != null) {
            return Optional.of(new TipoEDurata(TIPO_FIXED_RATE, rate));
        }

        // Solo initialDelay (esecuzione singola) o cron disattivato
        return Optional.empty();
    }

    private static Optional<TipoEDurata> intervalloCron(String cron, String zona, Clock clock) {
        ZoneId zoneId = StringUtils.hasText(zona) ? ZoneId.of(zona) : clock.getZone();
        CronExpression espressione = CronExpression.parse(cron);
        ZonedDateTime prima = espressione.next(ZonedDateTime.now(clock.withZone(zoneId)));
        ZonedDateTime seconda = (prima != null) ? espressione.next(prima) : null;
        if (seconda == null) {
            return Optional.empty();
        }
        return Optional.of(new TipoEDurata(TIPO_CRON, Duration.between(prima, seconda)));
    }

    /** Stesse regole di {@code ScheduledAnnotationBeanPostProcessor}: il valore numerico prevale se impostato. */
    private static Duration durata(long valore, String valoreStringa, TimeUnit unita, StringValueResolver resolver) {
        if (valore >= 0) {
            return Duration.of(valore, unita.toChronoUnit());
        }
        String testo = valore(valoreStringa, resolver);
        if (!StringUtils.hasText(testo)) {
            return null;
        }
        return DurationFormatterUtils.detectAndParse(testo.trim(),
                DurationFormat.Unit.fromChronoUnit(unita.toChronoUnit()));
    }

    private static String valore(String testo, StringValueResolver resolver) {
        return (resolver != null && StringUtils.hasText(testo)) ? resolver.resolveStringValue(testo) : testo;
    }
}
