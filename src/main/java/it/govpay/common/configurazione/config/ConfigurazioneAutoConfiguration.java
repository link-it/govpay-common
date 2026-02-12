package it.govpay.common.configurazione.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ComponentScan(basePackages = "it.govpay.common.configurazione")
public class ConfigurazioneAutoConfiguration {

    public ConfigurazioneAutoConfiguration() {
        log.info("GovPay Configurazione Commons inizializzato");
    }
}
