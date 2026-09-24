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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;

import it.govpay.common.configurazione.model.LogLevelConfig;
import it.govpay.common.configurazione.service.ConfigurazioneService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Gestione dinamica dei livelli di log (BP-LOG-1).
 *
 * <p>La sorgente di verita' e' la riga {@code log_level} della tabella
 * {@code configurazione}: una modifica scritta li' raggiunge <i>tutti</i> i nodi
 * del cluster, che la applicano entro l'intervallo di refresh configurato. Questo
 * e' il motivo per cui la gestione non si esaurisce nell'endpoint
 * {@code /actuator/loggers} di Spring Boot, che agisce solo sul nodo interrogato.
 *
 * <p>L'ambito e' limitato ai logger sotto i prefissi di
 * {@link DynamicLogLevelProperties#getLoggerGestiti()}: BP-LOG-1 richiede che la
 * gestione dinamica riguardi i log critici per l'operativita', non l'intera
 * applicazione. Una richiesta fuori ambito viene rifiutata con
 * {@link IllegalArgumentException}.
 *
 * <p>All'avvio viene memorizzato il livello configurato di ciascun logger gestito:
 * e' il livello a cui si torna quando una voce viene rimossa dalla configurazione
 * o quando si invoca {@link #reset()}. In produzione la disattivazione rapida di
 * un livello verboso e' critica quanto l'attivazione.
 */
@Slf4j
public class DynamicLogLevelService {

    private static final String LIVELLO_EREDITATO = null;

    private final ConfigurazioneService configurazioneService;
    private final LoggingSystem loggingSystem;
    private final DynamicLogLevelProperties properties;

    /** Livelli configurati all'avvio, per logger: usati come valore di ripristino. */
    private final Map<String, LogLevel> livelliIniziali = new ConcurrentHashMap<>();

    /** Livelli attualmente applicati dalla configurazione dinamica. */
    private final Map<String, LogLevel> livelliApplicati = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    public DynamicLogLevelService(ConfigurazioneService configurazioneService,
            LoggingSystem loggingSystem, DynamicLogLevelProperties properties) {
        this.configurazioneService = configurazioneService;
        this.loggingSystem = loggingSystem;
        this.properties = properties;
    }

    @PostConstruct
    void avvia() {
        memorizzaLivelliIniziali();
        // Il primo allineamento non deve poter impedire l'avvio dell'applicazione:
        // una configurazione illeggibile e' un'anomalia da segnalare, non un
        // motivo per non partire (BP-LOG-2).
        refreshSilenzioso();
        avviaScheduler();
    }

    @PreDestroy
    void arresta() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void memorizzaLivelliIniziali() {
        for (LoggerConfiguration configurazione : loggingSystem.getLoggerConfigurations()) {
            if (isGestito(configurazione.getName()) && configurazione.getConfiguredLevel() != null) {
                livelliIniziali.put(configurazione.getName(), configurazione.getConfiguredLevel());
            }
        }
        log.debug("Livelli di log iniziali memorizzati per {} logger gestiti", livelliIniziali.size());
    }

    private void avviaScheduler() {
        Duration intervallo = properties.getIntervalloRefresh();
        if (intervallo == null || intervallo.isZero() || intervallo.isNegative()) {
            log.info("Refresh periodico dei livelli di log disattivato: la configurazione e' applicata"
                    + " all'avvio e sulle richieste esplicite di refresh");
            return;
        }
        Duration ritardoIniziale = properties.getRitardoIniziale() != null
                ? properties.getRitardoIniziale()
                : intervallo;
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "govpay-loglevel-refresh");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::refreshSilenzioso,
                ritardoIniziale.toMillis(), intervallo.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Refresh periodico dei livelli di log attivo ogni {}", intervallo);
    }

    private void refreshSilenzioso() {
        try {
            refresh();
        } catch (Exception e) {
            // Il refresh e' un'attivita' di supporto: un errore non deve fermare
            // lo scheduler ne' l'applicazione (BP-LOG-2: anomalia gestita = WARN).
            log.warn("Refresh dei livelli di log non riuscito: {}", e.getMessage(), e);
        }
    }

    /**
     * Rilegge la configurazione da database e allinea i livelli dei logger gestiti.
     * <p>
     * I logger presenti in configurazione vengono portati al livello indicato;
     * quelli che erano stati modificati e non compaiono piu' vengono riportati al
     * livello che avevano all'avvio.
     *
     * @return i livelli applicati dopo l'allineamento, per logger
     */
    public synchronized Map<String, String> refresh() {
        Map<String, LogLevel> desiderati = leggiConfigurazione();

        for (Map.Entry<String, LogLevel> voce : desiderati.entrySet()) {
            applica(voce.getKey(), voce.getValue());
        }

        List<String> daRipristinare = new ArrayList<>(livelliApplicati.keySet());
        daRipristinare.removeAll(desiderati.keySet());
        for (String logger : daRipristinare) {
            ripristina(logger);
        }

        return livelliCorrenti();
    }

    private Map<String, LogLevel> leggiConfigurazione() {
        Optional<LogLevelConfig> config = configurazioneService.getLogLevel();
        if (config.isEmpty() || config.get().getLoggers() == null) {
            return Map.of();
        }
        Map<String, LogLevel> desiderati = new LinkedHashMap<>();
        for (Map.Entry<String, String> voce : config.get().getLoggers().entrySet()) {
            String logger = voce.getKey();
            if (!isGestito(logger)) {
                log.warn("Livello di log ignorato per il logger '{}': fuori dall'ambito gestito {}",
                        logger, properties.getLoggerGestiti());
                continue;
            }
            try {
                desiderati.put(logger, parseLivello(voce.getValue()));
            } catch (IllegalArgumentException e) {
                log.warn("Livello di log ignorato per il logger '{}': valore '{}' non valido",
                        logger, voce.getValue());
            }
        }
        return desiderati;
    }

    private void applica(String logger, LogLevel livello) {
        if (livello.equals(livelliApplicati.get(logger))) {
            return;
        }
        loggingSystem.setLogLevel(logger, livello);
        livelliApplicati.put(logger, livello);
        log.info("Livello di log del logger '{}' impostato a {}", logger, livello);
    }

    private void ripristina(String logger) {
        LogLevel iniziale = livelliIniziali.get(logger);
        loggingSystem.setLogLevel(logger, iniziale);
        livelliApplicati.remove(logger);
        log.info("Livello di log del logger '{}' ripristinato a {}", logger,
                iniziale != null ? iniziale : "ereditato");
    }

    /**
     * Imposta il livello di un logger, applicandolo subito su questo nodo e
     * persistendolo su database perche' gli altri nodi lo applichino al proprio
     * refresh.
     *
     * @param logger il nome del logger, che deve ricadere nell'ambito gestito
     * @param livello il livello da impostare ({@code TRACE}, {@code DEBUG}, {@code INFO},
     *        {@code WARN}, {@code ERROR}, {@code FATAL}, {@code OFF})
     * @throws IllegalArgumentException se il logger e' fuori ambito o il livello non e' valido
     */
    public synchronized void setLivello(String logger, String livello) {
        verificaGestito(logger);
        LogLevel parsed = parseLivello(livello);

        LogLevelConfig config = configurazioneCorrente();
        config.getLoggers().put(logger, parsed.name());
        configurazioneService.salvaLogLevel(config);

        applica(logger, parsed);
    }

    /**
     * Rimuove la gestione dinamica di un logger, riportandolo al livello che aveva
     * all'avvio dell'applicazione e aggiornando il database.
     *
     * @param logger il nome del logger, che deve ricadere nell'ambito gestito
     * @throws IllegalArgumentException se il logger e' fuori ambito
     */
    public synchronized void rimuoviLivello(String logger) {
        verificaGestito(logger);

        LogLevelConfig config = configurazioneCorrente();
        config.getLoggers().remove(logger);
        configurazioneService.salvaLogLevel(config);

        ripristina(logger);
    }

    /**
     * Riporta tutti i logger gestiti ai livelli che avevano all'avvio e svuota la
     * configurazione su database.
     */
    public synchronized void reset() {
        configurazioneService.salvaLogLevel(new LogLevelConfig());
        for (String logger : new ArrayList<>(livelliApplicati.keySet())) {
            ripristina(logger);
        }
    }

    /**
     * Restituisce i livelli attualmente applicati dalla configurazione dinamica.
     *
     * @return mappa logger-livello, vuota se nessun livello e' gestito dinamicamente
     */
    public Map<String, String> livelliCorrenti() {
        Map<String, String> risultato = new LinkedHashMap<>();
        livelliApplicati.forEach((logger, livello) -> risultato.put(logger, livello.name()));
        return Collections.unmodifiableMap(risultato);
    }

    /**
     * Restituisce lo stato di tutti i logger che ricadono nell'ambito gestito,
     * inclusi quelli non ancora modificati dinamicamente.
     *
     * @return l'elenco dei logger gestibili, ordinato per nome
     */
    public List<LivelloLoggerInfo> statoLogger() {
        List<LivelloLoggerInfo> risultato = new ArrayList<>();
        for (LoggerConfiguration configurazione : loggingSystem.getLoggerConfigurations()) {
            String nome = configurazione.getName();
            if (!isGestito(nome)) {
                continue;
            }
            LogLevel iniziale = livelliIniziali.get(nome);
            risultato.add(LivelloLoggerInfo.builder()
                    .logger(nome)
                    .livelloConfigurato(nome(configurazione.getConfiguredLevel()))
                    .livelloEffettivo(nome(configurazione.getEffectiveLevel()))
                    .livelloIniziale(nome(iniziale))
                    .gestitoDinamicamente(livelliApplicati.containsKey(nome))
                    .build());
        }
        risultato.sort((a, b) -> a.getLogger().compareTo(b.getLogger()));
        return risultato;
    }

    /**
     * Restituisce i prefissi dei logger gestibili dinamicamente.
     *
     * @return l'elenco dei prefissi configurati
     */
    public List<String> loggerGestiti() {
        return List.copyOf(properties.getLoggerGestiti());
    }

    private LogLevelConfig configurazioneCorrente() {
        LogLevelConfig config = configurazioneService.getLogLevel().orElseGet(LogLevelConfig::new);
        if (config.getLoggers() == null) {
            config.setLoggers(new LinkedHashMap<>());
        }
        return config;
    }

    private void verificaGestito(String logger) {
        if (!isGestito(logger)) {
            throw new IllegalArgumentException("Il logger '" + logger
                    + "' non e' gestibile dinamicamente. Logger gestiti: " + properties.getLoggerGestiti());
        }
    }

    private boolean isGestito(String logger) {
        if (logger == null || logger.isBlank()) {
            return false;
        }
        for (String prefisso : properties.getLoggerGestiti()) {
            if (logger.equals(prefisso) || logger.startsWith(prefisso + ".")) {
                return true;
            }
        }
        return false;
    }

    private static LogLevel parseLivello(String livello) {
        if (livello == null || livello.isBlank()) {
            throw new IllegalArgumentException("Livello di log non specificato");
        }
        return LogLevel.valueOf(livello.trim().toUpperCase(Locale.ROOT));
    }

    private static String nome(LogLevel livello) {
        return livello != null ? livello.name() : LIVELLO_EREDITATO;
    }
}
