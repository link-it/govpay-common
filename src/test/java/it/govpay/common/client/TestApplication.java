package it.govpay.common.client;

import it.govpay.common.configurazione.config.ConfigurazioneAutoConfiguration;
import it.govpay.common.intermediario.config.IntermediarioAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * Test application for Spring Boot integration tests.
 */
@SpringBootApplication
@Import({ConfigurazioneAutoConfiguration.class, IntermediarioAutoConfiguration.class})
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
