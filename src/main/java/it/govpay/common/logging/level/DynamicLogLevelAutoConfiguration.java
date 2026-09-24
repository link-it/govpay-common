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
package it.govpay.common.logging.level;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;

import it.govpay.common.configurazione.service.ConfigurazioneService;

/**
 * Auto-configurazione della gestione dinamica dei livelli di log (BP-LOG-1).
 *
 * <p>Si attiva solo se il consumer espone un {@link ConfigurazioneService} — ossia
 * se ha importato {@code ConfigurazioneAutoConfiguration} e dispone della tabella
 * {@code configurazione} — perche' e' li' che risiede la configurazione condivisa
 * fra i nodi. Nelle applicazioni che non la usano non viene registrato nulla.
 *
 * <p>Si disattiva con {@code govpay.logging.livelli-dinamici.enabled=false}.
 */
@AutoConfiguration
@EnableConfigurationProperties(DynamicLogLevelProperties.class)
@ConditionalOnBean(ConfigurazioneService.class)
@ConditionalOnProperty(prefix = "govpay.logging.livelli-dinamici", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class DynamicLogLevelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(LoggingSystem.class)
    public LoggingSystem loggingSystem() {
        return LoggingSystem.get(DynamicLogLevelAutoConfiguration.class.getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean(DynamicLogLevelService.class)
    public DynamicLogLevelService dynamicLogLevelService(ConfigurazioneService configurazioneService,
            LoggingSystem loggingSystem, DynamicLogLevelProperties properties) {
        return new DynamicLogLevelService(configurazioneService, loggingSystem, properties);
    }
}
