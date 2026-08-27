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
package it.govpay.common.utils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Calcolo di durate tra istanti espressi come {@link LocalDateTime}.
 * <p>
 * {@code LocalDateTime} rappresenta una data/ora "da calendario", priva di riferimento a un
 * istante assoluto sulla linea del tempo: la differenza tra due valori calcolata con
 * {@code Duration.between(LocalDateTime, LocalDateTime)} assume che ogni giorno duri
 * esattamente 24 ore. L'assunzione e' falsa due volte l'anno in {@code Europe/Rome}, alle
 * transizioni di ora legale, dove produce durate sbagliate di un'ora e - nella notte del
 * passaggio a ora solare, quando la stessa ora locale si ripete - anche durate negative.
 * <p>
 * Questi metodi ancorano ogni valore a una {@link ZoneId} <strong>esplicita</strong> prima di
 * calcolare la durata, cosi' che il risultato sia la distanza reale tra i due istanti. Vanno
 * usati ogni volta che gli istanti provengono da una sorgente che espone {@code LocalDateTime}
 * e non e' modificabile - tipicamente i metadati di Spring Batch
 * ({@code JobExecution#getStartTime()}, {@code StepExecution#getEndTime()}, ...).
 * <p>
 * Per il codice nuovo resta preferibile tipizzare l'istante come {@link java.time.Instant}
 * (misure tecniche) oppure {@link ZonedDateTime}/{@link java.time.OffsetDateTime} (date/ora
 * civili) direttamente alla sorgente, evitando la conversione.
 * <p>
 * Nota sulle ore ambigue: alla transizione verso l'ora solare la stessa ora locale esiste due
 * volte. {@link LocalDateTime#atZone(ZoneId)} risolve l'ambiguita' scegliendo in modo
 * deterministico l'offset precedente (quello dell'ora legale).
 *
 * @see java.time.Duration
 */
public final class DurationUtils {

    private static final String MSG_START_NULL = "L'istante iniziale (start) non può essere null";
    private static final String MSG_END_NULL = "L'istante finale (end) non può essere null";
    private static final String MSG_ZONE_NULL = "La zona (zone) non può essere null";
    private static final String MSG_CLOCK_NULL = "L'orologio (clock) non può essere null";

    private DurationUtils() {
        // Utility class
    }

    /**
     * Calcola la durata tra due istanti locali ancorandoli alla zona indicata.
     *
     * @param start istante iniziale, non null
     * @param end   istante finale, non null
     * @param zone  zona con cui interpretare i due istanti, non null
     * @return la durata reale tra i due istanti
     * @throws NullPointerException se uno dei parametri e' null
     */
    public static Duration between(LocalDateTime start, LocalDateTime end, ZoneId zone) {
        Objects.requireNonNull(start, MSG_START_NULL);
        Objects.requireNonNull(end, MSG_END_NULL);
        Objects.requireNonNull(zone, MSG_ZONE_NULL);
        return Duration.between(start.atZone(zone), end.atZone(zone));
    }

    /**
     * Variante null-safe di {@link #between(LocalDateTime, LocalDateTime, ZoneId)} che
     * restituisce i secondi trascorsi.
     *
     * @param start istante iniziale, eventualmente null
     * @param end   istante finale, eventualmente null
     * @param zone  zona con cui interpretare i due istanti, non null
     * @return i secondi tra i due istanti, oppure null se almeno uno dei due non e' valorizzato
     */
    public static Long secondsBetween(LocalDateTime start, LocalDateTime end, ZoneId zone) {
        if (start == null || end == null) {
            return null;
        }
        return between(start, end, zone).getSeconds();
    }

    /**
     * Variante null-safe di {@link #between(LocalDateTime, LocalDateTime, ZoneId)} che
     * restituisce i millisecondi trascorsi, azzerandosi quando la durata non e' calcolabile.
     * <p>
     * Pensata per le statistiche aggregate, dove un valore assente non deve interrompere
     * la somma.
     *
     * @param start istante iniziale, eventualmente null
     * @param end   istante finale, eventualmente null
     * @param zone  zona con cui interpretare i due istanti, non null
     * @return i millisecondi tra i due istanti, oppure 0 se almeno uno dei due non e' valorizzato
     */
    public static long millisBetweenOrZero(LocalDateTime start, LocalDateTime end, ZoneId zone) {
        if (start == null || end == null) {
            return 0L;
        }
        return between(start, end, zone).toMillis();
    }

    /**
     * Calcola la durata trascorsa da un istante locale a ora, nella zona indicata.
     *
     * @param start istante iniziale, non null
     * @param zone  zona con cui interpretare l'istante iniziale e leggere l'ora corrente, non null
     * @return la durata reale tra l'istante indicato e adesso
     * @throws NullPointerException se uno dei parametri e' null
     */
    public static Duration since(LocalDateTime start, ZoneId zone) {
        Objects.requireNonNull(start, MSG_START_NULL);
        Objects.requireNonNull(zone, MSG_ZONE_NULL);
        return Duration.between(start.atZone(zone), ZonedDateTime.now(zone));
    }

    /**
     * Calcola la durata trascorsa da un istante locale all'ora letta dal {@link Clock} indicato.
     * <p>
     * L'istante iniziale viene interpretato nella zona del clock, quindi la sorgente dell'ora
     * corrente e quella della conversione sono la stessa: e' la forma da preferire nel codice
     * che prende decisioni sulla base di una durata, perche' rende il tempo iniettabile e
     * quindi verificabile con {@link Clock#fixed(java.time.Instant, ZoneId)}.
     *
     * @param start istante iniziale, non null
     * @param clock sorgente dell'ora corrente e della zona, non null
     * @return la durata reale tra l'istante indicato e l'ora del clock
     * @throws NullPointerException se uno dei parametri e' null
     */
    public static Duration since(LocalDateTime start, Clock clock) {
        Objects.requireNonNull(start, MSG_START_NULL);
        Objects.requireNonNull(clock, MSG_CLOCK_NULL);
        return Duration.between(start.atZone(clock.getZone()).toInstant(), clock.instant());
    }
}
