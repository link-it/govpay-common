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

import java.time.ZoneId;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

import it.govpay.common.utils.DateTimePatterns;
import lombok.extern.slf4j.Slf4j;

/**
 * Fuso delle date sul database: le colonne {@code TIMESTAMP} senza time zone contengono
 * l'ora locale {@value DateTimePatterns#DEFAULT_TIME_ZONE}.
 *
 * <p>Imposta le proprieta' Hibernate, per l'intera persistence unit:
 * <ul>
 *   <li>{@code hibernate.jdbc.time_zone}: {@value DateTimePatterns#DEFAULT_TIME_ZONE};</li>
 *   <li>{@code hibernate.timezone.default_storage}: {@code NORMALIZE}, il valore e'
 *       convertito nel fuso JDBC e l'offset non e' salvato.</li>
 * </ul>
 *
 * <p>Un valore gia' impostato dall'applicazione resta invariato; se e' diverso viene
 * segnalato nel log.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(HibernatePropertiesCustomizer.class)
public class DatabaseTimezoneAutoConfiguration {

    private static final String STORAGE_NORMALIZE = "NORMALIZE";

    @Bean
    public HibernatePropertiesCustomizer databaseTimezoneCustomizer() {
        return properties -> {
            impostaSeAssente(properties, AvailableSettings.JDBC_TIME_ZONE, DateTimePatterns.DEFAULT_TIME_ZONE);
            impostaSeAssente(properties, AvailableSettings.TIMEZONE_DEFAULT_STORAGE, STORAGE_NORMALIZE);
        };
    }

    private static void impostaSeAssente(Map<String, Object> properties, String nome, String valore) {
        Object impostato = properties.putIfAbsent(nome, valore);
        if (impostato != null && !valore.equalsIgnoreCase(comeTesto(impostato))) {
            log.warn("{} impostato a [{}] invece di [{}]: le date sul database non seguono la convenzione di GovPay",
                    nome, comeTesto(impostato), valore);
        }
    }

    private static String comeTesto(Object valore) {
        if (valore instanceof ZoneId zona) {
            return zona.getId();
        }
        if (valore instanceof TimeZone fuso) {
            return fuso.getID();
        }
        return valore.toString().trim();
    }
}
