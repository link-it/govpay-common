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

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StringValueResolver;

import it.govpay.common.batch.runner.IntervalloSchedulazioneResolver.IntervalloSchedulazione;

class IntervalloSchedulazioneResolverTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T10:15:00Z"), ZoneId.of("Europe/Rome"));

    private static StringValueResolver resolver(String... chiaviValori) {
        MockEnvironment env = new MockEnvironment();
        for (int i = 0; i < chiaviValori.length; i += 2) {
            env.setProperty(chiaviValori[i], chiaviValori[i + 1]);
        }
        return env::resolvePlaceholders;
    }

    private static Optional<IntervalloSchedulazione> risolvi(AbstractScheduledJobRunner runner, StringValueResolver resolver) {
        return IntervalloSchedulazioneResolver.risolvi(runner, resolver, CLOCK);
    }

    private static void verifica(Optional<IntervalloSchedulazione> risultato, String tipo, Duration atteso) {
        IntervalloSchedulazione intervallo = risultato.orElseThrow();
        assertEquals("job", intervallo.jobName());
        assertEquals(tipo, intervallo.tipo());
        assertEquals(atteso, intervallo.intervallo());
    }

    // ============ Runner di prova ============

    abstract static class Runner extends AbstractScheduledJobRunner {
        Runner() {
            super(null, null, "job");
        }
    }

    static class FixedDelayPlaceholder extends Runner {
        @Scheduled(fixedDelayString = "${scheduler.rt.fixedDelay:7200000}")
        public void esegui() { /* schedulato */ }
    }

    static class FixedDelayNumericoInMinuti extends Runner {
        @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
        public void esegui() { /* schedulato */ }
    }

    static class FixedDelayIso extends Runner {
        @Scheduled(fixedDelayString = "PT2H")
        public void esegui() { /* schedulato */ }
    }

    static class FixedDelaySemplice extends Runner {
        @Scheduled(fixedDelayString = "30s")
        public void esegui() { /* schedulato */ }
    }

    static class FixedRate extends Runner {
        @Scheduled(fixedRateString = "5000")
        public void esegui() { /* schedulato */ }
    }

    static class CronOrario extends Runner {
        @Scheduled(cron = "${scheduler.notify.cron:0 0 * * * *}", zone = "Europe/Rome")
        public void esegui() { /* schedulato */ }
    }

    static class CronDisattivato extends Runner {
        @Scheduled(cron = "-")
        public void esegui() { /* schedulato */ }
    }

    static class CronSenzaProssimaEsecuzione extends Runner {
        // 30 febbraio: l'espressione e' valida ma non scatta mai
        @Scheduled(cron = "0 0 0 30 2 *")
        public void esegui() { /* schedulato */ }
    }

    static class CronNonValido extends Runner {
        @Scheduled(cron = "ogni tanto")
        public void esegui() { /* schedulato */ }
    }

    static class ZonaNonValida extends Runner {
        @Scheduled(cron = "0 0 * * * *", zone = "Europa/Cagliari")
        public void esegui() { /* schedulato */ }
    }

    static class DurataNonValida extends Runner {
        @Scheduled(fixedDelayString = "due ore")
        public void esegui() { /* schedulato */ }
    }

    static class SoloInitialDelay extends Runner {
        @Scheduled(initialDelay = 1000)
        public void esegui() { /* schedulato */ }
    }

    static class SenzaScheduled extends Runner {
        public void esegui() { /* non schedulato */ }
    }

    static class DueSchedulazioni extends Runner {
        @Scheduled(fixedDelay = 1000)
        public void bPrimaInOrdine() { /* schedulato */ }

        @Scheduled(fixedDelay = 2000)
        public void aPrimaPerNome() { /* schedulato */ }
    }

    // ============ Test ============

    @Test
    @DisplayName("fixedDelayString: il placeholder e' risolto dalla configurazione")
    void fixedDelayPlaceholderConfigurato() {
        verifica(risolvi(new FixedDelayPlaceholder(), resolver("scheduler.rt.fixedDelay", "60000")),
                IntervalloSchedulazioneResolver.TIPO_FIXED_DELAY, Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("fixedDelayString: senza configurazione vale il default del placeholder")
    void fixedDelayPlaceholderDefault() {
        verifica(risolvi(new FixedDelayPlaceholder(), resolver()),
                IntervalloSchedulazioneResolver.TIPO_FIXED_DELAY, Duration.ofHours(2));
    }

    @Test
    @DisplayName("fixedDelay numerico rispetta timeUnit")
    void fixedDelayNumerico() {
        verifica(risolvi(new FixedDelayNumericoInMinuti(), null),
                IntervalloSchedulazioneResolver.TIPO_FIXED_DELAY, Duration.ofMinutes(10));
    }

    @Test
    @DisplayName("fixedDelayString accetta il formato ISO-8601 e quello semplice")
    void fixedDelayFormati() {
        verifica(risolvi(new FixedDelayIso(), resolver()),
                IntervalloSchedulazioneResolver.TIPO_FIXED_DELAY, Duration.ofHours(2));
        verifica(risolvi(new FixedDelaySemplice(), resolver()),
                IntervalloSchedulazioneResolver.TIPO_FIXED_DELAY, Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("fixedRateString in millisecondi")
    void fixedRate() {
        verifica(risolvi(new FixedRate(), resolver()),
                IntervalloSchedulazioneResolver.TIPO_FIXED_RATE, Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("cron: l'intervallo e' la distanza tra le due prossime esecuzioni")
    void cron() {
        verifica(risolvi(new CronOrario(), resolver()),
                IntervalloSchedulazioneResolver.TIPO_CRON, Duration.ofHours(1));
        verifica(risolvi(new CronOrario(), resolver("scheduler.notify.cron", "0 */15 * * * *")),
                IntervalloSchedulazioneResolver.TIPO_CRON, Duration.ofMinutes(15));
    }

    @Test
    @DisplayName("Nessun intervallo per cron disattivato, cron che non scatta mai, solo initialDelay o nessun @Scheduled")
    void nessunIntervallo() {
        assertTrue(risolvi(new CronDisattivato(), resolver()).isEmpty());
        assertTrue(risolvi(new CronSenzaProssimaEsecuzione(), resolver()).isEmpty());
        assertTrue(risolvi(new SoloInitialDelay(), resolver()).isEmpty());
        assertTrue(risolvi(new SenzaScheduled(), resolver()).isEmpty());
    }

    @Test
    @DisplayName("Valori non interpretabili: nessun intervallo, nessuna eccezione")
    void valoriNonValidi() {
        assertTrue(risolvi(new CronNonValido(), resolver()).isEmpty());
        assertTrue(risolvi(new ZonaNonValida(), resolver()).isEmpty());
        assertTrue(risolvi(new DurataNonValida(), resolver()).isEmpty());
    }

    @Test
    @DisplayName("Con piu' schedulazioni vale quella del primo metodo in ordine di nome")
    void dueSchedulazioni() {
        verifica(risolvi(new DueSchedulazioni(), resolver()),
                IntervalloSchedulazioneResolver.TIPO_FIXED_DELAY, Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("Il runner dietro un proxy CGLIB e' letto dalla classe target")
    void proxyCglib() {
        ProxyFactory factory = new ProxyFactory(new FixedDelayIso());
        factory.setProxyTargetClass(true);
        AbstractScheduledJobRunner proxy = (AbstractScheduledJobRunner) factory.getProxy();

        verifica(risolvi(proxy, resolver()),
                IntervalloSchedulazioneResolver.TIPO_FIXED_DELAY, Duration.ofHours(2));
    }

    @Test
    @DisplayName("Senza orologio esplicito usa quello di sistema")
    void orologioDiSistema() {
        verifica(IntervalloSchedulazioneResolver.risolvi(new CronOrario(), resolver()),
                IntervalloSchedulazioneResolver.TIPO_CRON, Duration.ofHours(1));
    }
}
