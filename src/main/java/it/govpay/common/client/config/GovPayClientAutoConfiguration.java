package it.govpay.common.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@Configuration
@ComponentScan(basePackages = "it.govpay.common.client")
@EnableJpaRepositories(basePackages = "it.govpay.common.client.repository")
@EntityScan(basePackages = "it.govpay.common.client.entity")
public class GovPayClientAutoConfiguration {

    public GovPayClientAutoConfiguration() {
        log.info("GovPay Client Commons inizializzato");
    }
}
