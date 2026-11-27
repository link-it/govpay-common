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
package it.govpay.common.persistence.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@DisplayName("Date sul database - proprieta' Hibernate")
class DatabaseTimezoneAutoConfigurationTest {

    private static final String JDBC_TIME_ZONE = "hibernate.jdbc.time_zone";
    private static final String DEFAULT_STORAGE = "hibernate.timezone.default_storage";

    private final Logger logger = (Logger) LoggerFactory.getLogger(DatabaseTimezoneAutoConfiguration.class);
    private final ListAppender<ILoggingEvent> log = new ListAppender<>();
    private Level livello;

    @BeforeEach
    void registraLog() {
        livello = logger.getLevel();
        logger.setLevel(Level.WARN);
        log.start();
        logger.addAppender(log);
    }

    @AfterEach
    void rimuoviLog() {
        logger.detachAppender(log);
        logger.setLevel(livello);
    }

    private static Map<String, Object> applica(Map<String, Object> iniziali) {
        Map<String, Object> properties = new HashMap<>(iniziali);
        new DatabaseTimezoneAutoConfiguration().databaseTimezoneCustomizer().customize(properties);
        return properties;
    }

    private List<String> avvisi() {
        return log.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    @DisplayName("Proprieta' assenti: vengono impostate")
    void impostaLeProprietaAssenti() {
        Map<String, Object> properties = applica(Map.of());

        assertEquals("Europe/Rome", properties.get(JDBC_TIME_ZONE));
        assertEquals("NORMALIZE", properties.get(DEFAULT_STORAGE));
        assertTrue(avvisi().isEmpty());
    }

    static Stream<Object> fusoConforme() {
        return Stream.of("Europe/Rome", ZoneId.of("Europe/Rome"), TimeZone.getTimeZone("Europe/Rome"));
    }

    @ParameterizedTest
    @MethodSource("fusoConforme")
    @DisplayName("Valori gia' conformi: restano invariati senza avvisi")
    void lasciaIValoriConformi(Object fuso) {
        Map<String, Object> properties = applica(Map.of(JDBC_TIME_ZONE, fuso, DEFAULT_STORAGE, "normalize"));

        assertEquals(fuso, properties.get(JDBC_TIME_ZONE));
        assertEquals("normalize", properties.get(DEFAULT_STORAGE));
        assertTrue(avvisi().isEmpty());
    }

    @Test
    @DisplayName("Valori diversi: restano invariati e sono segnalati")
    void segnalaIValoriDiversi() {
        Map<String, Object> properties = applica(Map.of(JDBC_TIME_ZONE, "UTC", DEFAULT_STORAGE, "NORMALIZE_UTC"));

        assertEquals("UTC", properties.get(JDBC_TIME_ZONE));
        assertEquals("NORMALIZE_UTC", properties.get(DEFAULT_STORAGE));
        assertEquals(2, avvisi().size());
        assertTrue(avvisi().get(0).contains(JDBC_TIME_ZONE));
        assertTrue(avvisi().get(1).contains(DEFAULT_STORAGE));
    }
}
