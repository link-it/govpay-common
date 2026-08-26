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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verifica il calcolo delle durate attraverso le due transizioni di ora legale di
 * {@code Europe/Rome}, dove il calcolo naif su {@link LocalDateTime} sbaglia di un'ora.
 * <p>
 * Riferimenti 2026: passaggio a ora legale domenica 29 marzo (02:00 CET -&gt; 03:00 CEST),
 * ritorno a ora solare domenica 25 ottobre (03:00 CEST -&gt; 02:00 CET).
 */
class DurationUtilsTest {

    private static final ZoneId ROMA = ZoneId.of("Europe/Rome");

    @Nested
    @DisplayName("between - transizioni DST")
    class Between {

        @Test
        @DisplayName("passaggio a ora legale: un'ora locale in meno di quella apparente")
        void oraLegale() {
            LocalDateTime inizio = LocalDateTime.of(2026, 3, 29, 1, 30);
            LocalDateTime fine = LocalDateTime.of(2026, 3, 29, 3, 30);

            Duration durata = DurationUtils.between(inizio, fine, ROMA);

            // Le lancette saltano da 02:00 a 03:00: tra i due istanti e' passata una sola ora
            assertEquals(Duration.ofHours(1), durata);
            // Il calcolo naif su LocalDateTime ne conterebbe due
            assertNotEquals(Duration.between(inizio, fine), durata);
        }

        @Test
        @DisplayName("ritorno a ora solare: un'ora locale in piu' di quella apparente")
        void oraSolare() {
            LocalDateTime inizio = LocalDateTime.of(2026, 10, 25, 1, 30);
            LocalDateTime fine = LocalDateTime.of(2026, 10, 25, 4, 30);

            Duration durata = DurationUtils.between(inizio, fine, ROMA);

            // Le lancette tornano da 03:00 a 02:00: sono passate quattro ore, non tre
            assertEquals(Duration.ofHours(4), durata);
            assertEquals(Duration.ofHours(3), Duration.between(inizio, fine));
        }

        @Test
        @DisplayName("ora ambigua risolta sull'offset precedente, durata mai negativa")
        void oraAmbigua() {
            // 02:30 del 25 ottobre esiste due volte: atZone sceglie l'offset dell'ora legale
            LocalDateTime inizio = LocalDateTime.of(2026, 10, 25, 2, 30);
            LocalDateTime fine = LocalDateTime.of(2026, 10, 25, 2, 45);

            Duration durata = DurationUtils.between(inizio, fine, ROMA);

            assertEquals(Duration.ofMinutes(15), durata);
            assertTrue(durata.isPositive() || durata.isZero());
        }

        @Test
        @DisplayName("intervallo senza transizioni: identico al calcolo naif")
        void giornoNormale() {
            LocalDateTime inizio = LocalDateTime.of(2026, 6, 15, 10, 0);
            LocalDateTime fine = LocalDateTime.of(2026, 6, 15, 10, 5);

            assertEquals(Duration.ofMinutes(5), DurationUtils.between(inizio, fine, ROMA));
        }

        @Test
        @DisplayName("parametri null rifiutati")
        void parametriNull() {
            LocalDateTime istante = LocalDateTime.of(2026, 6, 15, 10, 0);

            assertThrows(NullPointerException.class, () -> DurationUtils.between(null, istante, ROMA));
            assertThrows(NullPointerException.class, () -> DurationUtils.between(istante, null, ROMA));
            assertThrows(NullPointerException.class, () -> DurationUtils.between(istante, istante, null));
        }
    }

    @Nested
    @DisplayName("varianti null-safe")
    class NullSafe {

        @Test
        @DisplayName("secondsBetween restituisce null se un estremo manca")
        void secondsBetween() {
            LocalDateTime inizio = LocalDateTime.of(2026, 10, 25, 1, 30);
            LocalDateTime fine = LocalDateTime.of(2026, 10, 25, 4, 30);

            assertEquals(4 * 3600L, DurationUtils.secondsBetween(inizio, fine, ROMA));
            assertNull(DurationUtils.secondsBetween(null, fine, ROMA));
            assertNull(DurationUtils.secondsBetween(inizio, null, ROMA));
        }

        @Test
        @DisplayName("millisBetweenOrZero azzera se un estremo manca")
        void millisBetweenOrZero() {
            LocalDateTime inizio = LocalDateTime.of(2026, 3, 29, 1, 30);
            LocalDateTime fine = LocalDateTime.of(2026, 3, 29, 3, 30);

            assertEquals(3600_000L, DurationUtils.millisBetweenOrZero(inizio, fine, ROMA));
            assertEquals(0L, DurationUtils.millisBetweenOrZero(null, fine, ROMA));
            assertEquals(0L, DurationUtils.millisBetweenOrZero(inizio, null, ROMA));
        }
    }

    @Nested
    @DisplayName("since")
    class Since {

        @Test
        @DisplayName("con Clock fisso a cavallo del ritorno all'ora solare")
        void sinceConClockFisso() {
            // Il clock e' fermo alle 02:00 CET del 25 ottobre 2026, cioe' 01:00 UTC
            Clock clock = Clock.fixed(Instant.parse("2026-10-25T01:00:00Z"), ROMA);
            // Ultimo aggiornamento alle 01:30 ora locale, ancora in ora legale: 23:30 UTC del 24
            LocalDateTime ultimoAggiornamento = LocalDateTime.of(2026, 10, 25, 1, 30);

            Duration durata = DurationUtils.since(ultimoAggiornamento, clock);

            // Sono passati 90 minuti reali, non 30 come suggerirebbe la differenza delle ore locali
            assertEquals(90, durata.toMinutes());
        }

        @Test
        @DisplayName("con ZoneId esplicita restituisce una durata non negativa")
        void sinceConZone() {
            Duration durata = DurationUtils.since(LocalDateTime.now(ROMA).minusMinutes(5), ROMA);

            assertTrue(durata.toMinutes() >= 4 && durata.toMinutes() <= 6,
                    "durata inattesa: " + durata);
        }

        @Test
        @DisplayName("parametri null rifiutati")
        void parametriNull() {
            Clock clock = Clock.fixed(Instant.parse("2026-10-25T01:00:00Z"), ROMA);

            assertThrows(NullPointerException.class, () -> DurationUtils.since(null, clock));
            assertThrows(NullPointerException.class,
                    () -> DurationUtils.since(LocalDateTime.now(ROMA), (Clock) null));
            assertThrows(NullPointerException.class,
                    () -> DurationUtils.since(LocalDateTime.now(ROMA), (ZoneId) null));
        }
    }
}
