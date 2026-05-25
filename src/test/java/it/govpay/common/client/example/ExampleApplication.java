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
package it.govpay.common.client.example;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import it.govpay.common.client.service.ConnettoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = "it.govpay.common.client")
@RequiredArgsConstructor
public class ExampleApplication implements CommandLineRunner {

    private final ConnettoreService connettoreService;

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("=== Esempio di utilizzo GovPay Client Commons ===");

        try {
            // Esempio 1: Ottenere un RestTemplate configurato
            log.info("1. Ottenimento RestTemplate per connettore 'ESEMPIO_BASE'");
            connettoreService.getRestTemplate("ESEMPIO_BASE");
            log.info("RestTemplate ottenuto con successo!");

            // Esempio 2: Verifica cache
            log.info("2. Verifica stato cache");
            boolean inCache = connettoreService.isInCache("ESEMPIO_BASE");
            log.info("Connettore in cache: {}", inCache);
            log.info("Dimensione cache: {}", connettoreService.getCacheSize());

            // Esempio 3: Invalidazione cache
            log.info("3. Invalidazione cache per connettore specifico");
            connettoreService.invalidateCache("ESEMPIO_BASE");
            log.info("Cache invalidata");

            // Esempio 4: Ricaricamento connettore
            log.info("4. Ricaricamento connettore");
            connettoreService.reloadConnettore("ESEMPIO_BASE");
            log.info("Connettore ricaricato");

            // Esempio 5: Gestione errori
            log.info("5. Tentativo di accesso a connettore inesistente");
            try {
                connettoreService.getRestTemplate("CONNETTORE_NON_ESISTENTE");
            } catch (IllegalArgumentException e) {
                log.warn("Errore atteso: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("Errore durante l'esecuzione dell'esempio", e);
        }

        log.info("=== Fine esempio ===");
    }
}
