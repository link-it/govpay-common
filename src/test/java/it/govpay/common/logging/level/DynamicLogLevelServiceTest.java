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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;

import it.govpay.common.configurazione.model.LogLevelConfig;
import it.govpay.common.configurazione.service.ConfigurazioneService;

/**
 * Verifica BP-LOG-1 sulla gestione dinamica dei livelli: applicazione della
 * configurazione letta da database, limitazione ai soli logger in ambito,
 * persistenza delle modifiche perche' raggiungano gli altri nodi, e ripristino
 * dei livelli iniziali alla rimozione.
 */
@ExtendWith(MockitoExtension.class)
class DynamicLogLevelServiceTest {

    @Mock
    private ConfigurazioneService configurazioneService;

    private LoggingSystemStub loggingSystem;
    private DynamicLogLevelProperties properties;
    private DynamicLogLevelService service;

    @BeforeEach
    void setUp() {
        loggingSystem = new LoggingSystemStub();
        loggingSystem.configura("it.govpay", LogLevel.INFO);
        loggingSystem.configura("it.govpay.common.client", LogLevel.INFO);
        loggingSystem.configura("org.hibernate", LogLevel.WARN);

        properties = new DynamicLogLevelProperties();
        // Nessun polling: i test pilotano il refresh esplicitamente
        properties.setIntervalloRefresh(Duration.ZERO);

        service = new DynamicLogLevelService(configurazioneService, loggingSystem, properties);
    }

    @Test
    @DisplayName("All'avvio applica i livelli presenti in configurazione")
    void applicaLaConfigurazioneAllAvvio() {
        when(configurazioneService.getLogLevel())
                .thenReturn(Optional.of(config(Map.of("it.govpay.common.client", "DEBUG"))));

        service.avvia();

        assertEquals(LogLevel.DEBUG, loggingSystem.livelloDi("it.govpay.common.client"));
        assertEquals(Map.of("it.govpay.common.client", "DEBUG"), service.livelliCorrenti());
    }

    @Test
    @DisplayName("Ignora i logger fuori dall'ambito gestito")
    void ignoraLoggerFuoriAmbito() {
        when(configurazioneService.getLogLevel())
                .thenReturn(Optional.of(config(Map.of("org.hibernate", "DEBUG"))));

        service.avvia();

        assertEquals(LogLevel.WARN, loggingSystem.livelloDi("org.hibernate"));
        assertTrue(service.livelliCorrenti().isEmpty());
    }

    @Test
    @DisplayName("Ignora un livello non riconosciuto senza fallire")
    void ignoraLivelloNonValido() {
        when(configurazioneService.getLogLevel())
                .thenReturn(Optional.of(config(Map.of("it.govpay.common.client", "VERBOSE"))));

        service.avvia();

        assertEquals(LogLevel.INFO, loggingSystem.livelloDi("it.govpay.common.client"));
        assertTrue(service.livelliCorrenti().isEmpty());
    }

    @Test
    @DisplayName("Il refresh ripristina i livelli iniziali dei logger rimossi dalla configurazione")
    void ripristinaIlivelliRimossi() {
        when(configurazioneService.getLogLevel())
                .thenReturn(Optional.of(config(Map.of("it.govpay.common.client", "TRACE"))))
                .thenReturn(Optional.of(config(Map.of())));

        service.avvia();
        assertEquals(LogLevel.TRACE, loggingSystem.livelloDi("it.govpay.common.client"));

        service.refresh();

        assertEquals(LogLevel.INFO, loggingSystem.livelloDi("it.govpay.common.client"));
        assertTrue(service.livelliCorrenti().isEmpty());
    }

    @Test
    @DisplayName("setLivello applica subito e persiste la configurazione per gli altri nodi")
    void setLivelloApplicaEPersiste() {
        when(configurazioneService.getLogLevel()).thenReturn(Optional.empty());

        service.avvia();
        service.setLivello("it.govpay.common.client", "debug");

        assertEquals(LogLevel.DEBUG, loggingSystem.livelloDi("it.govpay.common.client"));

        LogLevelConfig salvata = catturaConfigurazioneSalvata();
        assertEquals("DEBUG", salvata.getLoggers().get("it.govpay.common.client"));
    }

    @Test
    @DisplayName("setLivello rifiuta un logger fuori ambito")
    void setLivelloRifiutaFuoriAmbito() {
        when(configurazioneService.getLogLevel()).thenReturn(Optional.empty());
        service.avvia();

        assertThrows(IllegalArgumentException.class,
                () -> service.setLivello("org.hibernate", "DEBUG"));
        assertEquals(LogLevel.WARN, loggingSystem.livelloDi("org.hibernate"));
    }

    @Test
    @DisplayName("setLivello rifiuta un livello non riconosciuto")
    void setLivelloRifiutaLivelloNonValido() {
        when(configurazioneService.getLogLevel()).thenReturn(Optional.empty());
        service.avvia();

        assertThrows(IllegalArgumentException.class,
                () -> service.setLivello("it.govpay.common.client", "VERBOSE"));
    }

    @Test
    @DisplayName("reset riporta tutti i logger ai livelli iniziali e svuota la configurazione")
    void resetRipristinaTutto() {
        when(configurazioneService.getLogLevel())
                .thenReturn(Optional.of(config(Map.of("it.govpay.common.client", "TRACE"))));

        service.avvia();
        service.reset();

        assertEquals(LogLevel.INFO, loggingSystem.livelloDi("it.govpay.common.client"));
        assertTrue(service.livelliCorrenti().isEmpty());
        verify(configurazioneService).salvaLogLevel(any(LogLevelConfig.class));
    }

    @Test
    @DisplayName("Un errore di lettura della configurazione non impedisce l'avvio")
    void erroreInLetturaNonImpedisceLAvvio() {
        when(configurazioneService.getLogLevel())
                .thenThrow(new IllegalStateException("tabella configurazione non raggiungibile"));

        assertDoesNotThrow(() -> service.avvia());
        assertTrue(service.livelliCorrenti().isEmpty());
    }

    @Test
    @DisplayName("statoLogger elenca solo i logger in ambito e segnala quelli gestiti dinamicamente")
    void statoLoggerElencaSoloIGestibili() {
        when(configurazioneService.getLogLevel())
                .thenReturn(Optional.of(config(Map.of("it.govpay.common.client", "DEBUG"))));

        service.avvia();
        List<LivelloLoggerInfo> stato = service.statoLogger();

        assertEquals(List.of("it.govpay", "it.govpay.common.client"),
                stato.stream().map(LivelloLoggerInfo::getLogger).toList());

        LivelloLoggerInfo client = stato.get(1);
        assertTrue(client.isGestitoDinamicamente());
        assertEquals("DEBUG", client.getLivelloConfigurato());
        assertEquals("INFO", client.getLivelloIniziale());

        assertFalse(stato.get(0).isGestitoDinamicamente());
    }

    private LogLevelConfig catturaConfigurazioneSalvata() {
        ArgumentCaptor<LogLevelConfig> captor = ArgumentCaptor.forClass(LogLevelConfig.class);
        verify(configurazioneService).salvaLogLevel(captor.capture());
        return captor.getValue();
    }

    private static LogLevelConfig config(Map<String, String> loggers) {
        LogLevelConfig config = new LogLevelConfig();
        config.setLoggers(new LinkedHashMap<>(loggers));
        return config;
    }

    /**
     * {@link LoggingSystem} minimale in memoria: registra i livelli impostati e li
     * ripropone attraverso {@code getLoggerConfigurations()}.
     */
    private static class LoggingSystemStub extends LoggingSystem {

        private final Map<String, LogLevel> livelli = new LinkedHashMap<>();

        void configura(String logger, LogLevel livello) {
            livelli.put(logger, livello);
        }

        LogLevel livelloDi(String logger) {
            return livelli.get(logger);
        }

        @Override
        public void beforeInitialize() {
            // niente da fare
        }

        @Override
        public void setLogLevel(String loggerName, LogLevel level) {
            if (level == null) {
                livelli.remove(loggerName);
            } else {
                livelli.put(loggerName, level);
            }
        }

        @Override
        public List<LoggerConfiguration> getLoggerConfigurations() {
            List<LoggerConfiguration> configurazioni = new ArrayList<>();
            livelli.forEach((nome, livello) ->
                    configurazioni.add(new LoggerConfiguration(nome, livello, livello)));
            return configurazioni;
        }

        @Override
        public LoggerConfiguration getLoggerConfiguration(String loggerName) {
            LogLevel livello = livelli.get(loggerName);
            return livello == null ? null : new LoggerConfiguration(loggerName, livello, livello);
        }
    }
}
