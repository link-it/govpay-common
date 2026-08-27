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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica il calcolo delle durate attraverso le due transizioni di ora legale di
 * {@code Europe/Rome}, dove il calcolo naif su {@link LocalDateTime} sbaglia di un'ora.
 * <p>
 * Riferimenti 2026: passaggio a ora legale domenica 29 marzo (02:00 CET -&gt; 03:00 CEST),
 * ritorno a ora solare domenica 25 ottobre (03:00 CEST -&gt; 02:00 CET).
 * <p>
 * I valori attesi sono scritti come costanti esplicite: calcolarli con
 * {@code Duration.between} sugli stessi {@code LocalDateTime} li confronterebbe con un'altra
 * istanza dello stesso errore, e i test passerebbero anche con l'utility sbagliata.
 */
class DurationUtilsTest {

    private static final ZoneId ROMA = ZoneId.of("Europe/Rome");

    // Passaggio a ora legale: le lancette saltano da 02:00 a 03:00.
    // Tra i due istanti locali e' passata un'ora sola; il calcolo naif ne conterebbe due.
    private static final LocalDateTime ORA_LEGALE_INIZIO = LocalDateTime.of(2026, 3, 29, 1, 30);
    private static final LocalDateTime ORA_LEGALE_FINE = LocalDateTime.of(2026, 3, 29, 3, 30);
    private static final Duration ORA_LEGALE_DURATA_REALE = Duration.ofHours(1);

    // Ritorno a ora solare: le lancette tornano da 03:00 a 02:00.
    // Tra i due istanti locali sono passate quattro ore; il calcolo naif ne conterebbe tre.
    private static final LocalDateTime ORA_SOLARE_INIZIO = LocalDateTime.of(2026, 10, 25, 1, 30);
    private static final LocalDateTime ORA_SOLARE_FINE = LocalDateTime.of(2026, 10, 25, 4, 30);
    private static final Duration ORA_SOLARE_DURATA_REALE = Duration.ofHours(4);

    @Test
    @DisplayName("between - passaggio a ora legale: un'ora reale, non due")
    void betweenAttraversoOraLegale() {
        assertEquals(ORA_LEGALE_DURATA_REALE,
                DurationUtils.between(ORA_LEGALE_INIZIO, ORA_LEGALE_FINE, ROMA));
    }

    @Test
    @DisplayName("between - ritorno a ora solare: quattro ore reali, non tre")
    void betweenAttraversoOraSolare() {
        assertEquals(ORA_SOLARE_DURATA_REALE,
                DurationUtils.between(ORA_SOLARE_INIZIO, ORA_SOLARE_FINE, ROMA));
    }

    @Test
    @DisplayName("between - ora ambigua risolta sull'offset precedente, durata mai negativa")
    void betweenOraAmbigua() {
        // 02:30 del 25 ottobre esiste due volte: atZone sceglie l'offset dell'ora legale
        Duration durata = DurationUtils.between(
                LocalDateTime.of(2026, 10, 25, 2, 30),
                LocalDateTime.of(2026, 10, 25, 2, 45),
                ROMA);

        assertEquals(Duration.ofMinutes(15), durata);
        assertTrue(durata.isPositive());
    }

    @Test
    @DisplayName("between - intervallo senza transizioni")
    void betweenGiornoNormale() {
        assertEquals(Duration.ofMinutes(5), DurationUtils.between(
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 10, 5),
                ROMA));
    }

    @Test
    @DisplayName("between - parametri null rifiutati")
    void betweenParametriNull() {
        LocalDateTime istante = LocalDateTime.of(2026, 6, 15, 10, 0);

        assertThrows(NullPointerException.class, () -> DurationUtils.between(null, istante, ROMA));
        assertThrows(NullPointerException.class, () -> DurationUtils.between(istante, null, ROMA));
        assertThrows(NullPointerException.class, () -> DurationUtils.between(istante, istante, null));
    }

    @Test
    @DisplayName("secondsBetween - durata reale attraverso la transizione, null se un estremo manca")
    void secondsBetween() {
        assertEquals(ORA_SOLARE_DURATA_REALE.getSeconds(),
                DurationUtils.secondsBetween(ORA_SOLARE_INIZIO, ORA_SOLARE_FINE, ROMA));
        assertNull(DurationUtils.secondsBetween(null, ORA_SOLARE_FINE, ROMA));
        assertNull(DurationUtils.secondsBetween(ORA_SOLARE_INIZIO, null, ROMA));
    }

    @Test
    @DisplayName("millisBetweenOrZero - durata reale attraverso la transizione, zero se un estremo manca")
    void millisBetweenOrZero() {
        assertEquals(ORA_LEGALE_DURATA_REALE.toMillis(),
                DurationUtils.millisBetweenOrZero(ORA_LEGALE_INIZIO, ORA_LEGALE_FINE, ROMA));
        assertEquals(0L, DurationUtils.millisBetweenOrZero(null, ORA_LEGALE_FINE, ROMA));
        assertEquals(0L, DurationUtils.millisBetweenOrZero(ORA_LEGALE_INIZIO, null, ROMA));
    }

    @Test
    @DisplayName("since con Clock fisso a cavallo del ritorno all'ora solare")
    void sinceConClockFisso() {
        // Clock fermo alle 02:00 CET del 25 ottobre 2026, cioe' 01:00 UTC
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T01:00:00Z"), ROMA);

        // Ultimo aggiornamento alle 01:30 ora locale, ancora in ora legale: 23:30 UTC del 24.
        // Sono passati 90 minuti reali, non i 30 della differenza fra le due ore locali.
        assertEquals(90, DurationUtils.since(ORA_SOLARE_INIZIO, clock).toMinutes());
    }

    @Test
    @DisplayName("since con Clock fisso a cavallo del passaggio a ora legale")
    void sinceConClockFissoOraLegale() {
        // Clock fermo alle 03:30 CEST del 29 marzo 2026, cioe' 01:30 UTC
        Clock clock = Clock.fixed(Instant.parse("2026-03-29T01:30:00Z"), ROMA);

        // Inizio alle 01:30 ora locale (CET, 00:30 UTC): un'ora reale, non due
        assertEquals(60, DurationUtils.since(ORA_LEGALE_INIZIO, clock).toMinutes());
    }

    @Test
    @DisplayName("since con ZoneId esplicita")
    void sinceConZone() {
        Duration durata = DurationUtils.since(LocalDateTime.now(ROMA).minusMinutes(5), ROMA);

        assertTrue(durata.toMinutes() >= 4 && durata.toMinutes() <= 6, "durata inattesa: " + durata);
    }

    @Test
    @DisplayName("since - parametri null rifiutati")
    void sinceParametriNull() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-25T01:00:00Z"), ROMA);

        assertThrows(NullPointerException.class, () -> DurationUtils.since(null, clock));
        assertThrows(NullPointerException.class,
                () -> DurationUtils.since(LocalDateTime.now(ROMA), (Clock) null));
        assertThrows(NullPointerException.class,
                () -> DurationUtils.since(LocalDateTime.now(ROMA), (ZoneId) null));
    }
}
