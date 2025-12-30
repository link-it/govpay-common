/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2025 Link.it srl (http://www.link.it).
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

import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Configurazione del timezone dell'applicazione.
 * <p>
 * Imposta il timezone di default della JVM leggendo dalla property {@code spring.jackson.time-zone}.
 * Questa configurazione ha priorita' alta per assicurarsi che il timezone sia impostato
 * prima che altri componenti vengano inizializzati.
 * <p>
 * Espone anche un bean {@code ZoneId} per l'uso in altri componenti.
 * <p>
 * Configuration property: {@code spring.jackson.time-zone} (default: Europe/Rome)
 */
@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TimezoneConfig {

    @Getter
    @Value("${spring.jackson.time-zone:Europe/Rome}")
    private String timezone;

    /**
     * Imposta il timezone di default della JVM all'avvio.
     */
    @PostConstruct
    public void init() {
        TimeZone timeZone = TimeZone.getTimeZone(timezone);
        TimeZone.setDefault(timeZone);
        log.info("Timezone di default impostato a: {}", timeZone.getID());
    }

    /**
     * Bean che espone lo ZoneId configurato per utilizzo in altri componenti.
     *
     * @return ZoneId corrispondente al timezone configurato
     */
    @Bean
    public ZoneId applicationZoneId() {
        return ZoneId.of(timezone);
    }
}
