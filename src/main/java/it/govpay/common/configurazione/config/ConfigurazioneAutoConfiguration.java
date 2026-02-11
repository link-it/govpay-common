package it.govpay.common.configurazione.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@Configuration
@ComponentScan(basePackages = "it.govpay.common.configurazione")
@EnableJpaRepositories(basePackages = "it.govpay.common.configurazione.repository")
@EntityScan(basePackages = "it.govpay.common.configurazione.entity")
public class ConfigurazioneAutoConfiguration {

    public ConfigurazioneAutoConfiguration() {
        log.info("GovPay Configurazione Commons inizializzato");
    }
}
