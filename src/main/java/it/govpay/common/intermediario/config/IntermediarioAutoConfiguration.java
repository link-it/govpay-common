package it.govpay.common.intermediario.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@Configuration
@ComponentScan(basePackages = "it.govpay.common.intermediario")
@EnableJpaRepositories(basePackages = "it.govpay.common.intermediario.repository")
@EntityScan(basePackages = "it.govpay.common.intermediario.entity")
public class IntermediarioAutoConfiguration {

    public IntermediarioAutoConfiguration() {
        log.info("GovPay Intermediario Commons inizializzato");
    }
}
