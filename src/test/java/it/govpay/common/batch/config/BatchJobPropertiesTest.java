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

import static org.junit.jupiter.api.Assertions.*;

import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BatchJobPropertiesTest {

    private BatchJobProperties properties;

    @BeforeEach
    void setUp() {
        properties = new BatchJobProperties();
    }

    @Test
    @DisplayName("Valori di default")
    void defaultValues() {
        assertEquals("GovPay-Batch", properties.getClusterId());
        assertEquals(120, properties.getStaleThresholdMinutes());
        assertEquals("Europe/Rome", properties.getTimeZone());
        assertEquals(600000L, properties.getSchedulerIntervalMillis());
        assertEquals(1L, properties.getInitialDelayMillis());
    }

    @Test
    @DisplayName("getZoneId con timezone di default")
    void getZoneId_default() {
        ZoneId zoneId = properties.getZoneId();

        assertEquals(ZoneId.of("Europe/Rome"), zoneId);
    }

    @Test
    @DisplayName("getZoneId con timezone custom")
    void getZoneId_custom() {
        properties.setTimeZone("America/New_York");

        ZoneId zoneId = properties.getZoneId();

        assertEquals(ZoneId.of("America/New_York"), zoneId);
    }

    @Test
    @DisplayName("Setters modificano i valori")
    void setters() {
        properties.setClusterId("my-cluster");
        properties.setStaleThresholdMinutes(60);
        properties.setTimeZone("UTC");
        properties.setSchedulerIntervalMillis(300000L);
        properties.setInitialDelayMillis(5000L);

        assertEquals("my-cluster", properties.getClusterId());
        assertEquals(60, properties.getStaleThresholdMinutes());
        assertEquals("UTC", properties.getTimeZone());
        assertEquals(300000L, properties.getSchedulerIntervalMillis());
        assertEquals(5000L, properties.getInitialDelayMillis());
    }
}
