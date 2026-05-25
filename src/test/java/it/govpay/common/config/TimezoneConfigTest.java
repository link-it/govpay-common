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
package it.govpay.common.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TimezoneConfigTest {

    @Test
    @DisplayName("init() imposta il timezone di default della JVM")
    void init_setsDefaultTimezone() {
        TimezoneConfig config = new TimezoneConfig();
        ReflectionTestUtils.setField(config, "timezone", "Europe/Rome");

        config.init();

        assertEquals("Europe/Rome", TimeZone.getDefault().getID());
    }

    @Test
    @DisplayName("applicationZoneId() restituisce il ZoneId corretto")
    void applicationZoneId_returnsCorrectZoneId() {
        TimezoneConfig config = new TimezoneConfig();
        ReflectionTestUtils.setField(config, "timezone", "Europe/Rome");

        ZoneId zoneId = config.applicationZoneId();

        assertEquals(ZoneId.of("Europe/Rome"), zoneId);
    }

    @Test
    @DisplayName("init() con timezone UTC")
    void init_utcTimezone() {
        TimezoneConfig config = new TimezoneConfig();
        ReflectionTestUtils.setField(config, "timezone", "UTC");

        config.init();

        assertEquals("UTC", TimeZone.getDefault().getID());
    }

    @Test
    @DisplayName("getTimezone restituisce il valore configurato")
    void getTimezone() {
        TimezoneConfig config = new TimezoneConfig();
        ReflectionTestUtils.setField(config, "timezone", "America/New_York");

        assertEquals("America/New_York", config.getTimezone());
    }
}
