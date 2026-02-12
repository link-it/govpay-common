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
