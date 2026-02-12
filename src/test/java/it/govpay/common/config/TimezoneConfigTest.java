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
