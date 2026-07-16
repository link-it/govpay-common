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
package it.govpay.common.client.service;

import it.govpay.common.client.async.AsyncRestTemplateWrapper;
import it.govpay.common.client.converter.ConnettoreConverter;
import it.govpay.common.entity.ConnettoreEntity;
import it.govpay.common.client.model.Connettore;
import it.govpay.common.client.factory.RestTemplateFactory;
import it.govpay.common.repository.ConnettoreEntityRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
@Transactional(readOnly = true)
public class ConnettoreService {

    private final ConnettoreEntityRepository connettoreEntityRepository;
    private final RestTemplateFactory restTemplateFactory;

    @Qualifier("asyncHttpExecutor")
    private final Executor asyncHttpExecutor;

    @Value("${govpay.client.cache.enabled:false}")
    private boolean cacheEnabled;

    private final Map<String, RestTemplate> restTemplateCache = new ConcurrentHashMap<>();
    private final Map<String, Connettore> connettoreCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Inizializzazione ConnettoreService - Cache abilitata: {}", cacheEnabled);
        if (cacheEnabled) {
            preloadConnettori();
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("Distruzione ConnettoreService e pulizia cache");
        clearCache();
    }

    public void preloadConnettori() {
        log.info("Precaricamento connettori abilitati");
        List<ConnettoreEntity> entities = connettoreEntityRepository.findAllAbilitati();

        Map<String, List<ConnettoreEntity>> groupedByCode = entities.stream()
                .collect(java.util.stream.Collectors.groupingBy(ConnettoreEntity::getCodConnettore));

        groupedByCode.forEach((codice, entityList) -> {
            Connettore connettore = ConnettoreConverter.toModel(entityList);
            if (connettore != null && connettore.isAbilitato()) {
                connettoreCache.put(codice, connettore);
                log.debug("Connettore precaricato: {}", codice);
            }
        });
        log.info("Precaricati {} connettori", connettoreCache.size());
    }

    public RestTemplate getRestTemplate(String codiceConnettore) {
        log.debug("Richiesta RestTemplate per connettore: {}", codiceConnettore);

        if (!cacheEnabled) {
            log.debug("Cache disabilitata, creazione RestTemplate senza caching");
            Connettore connettore = getConnettore(codiceConnettore);
            return restTemplateFactory.createRestTemplate(connettore);
        }

        return restTemplateCache.computeIfAbsent(codiceConnettore, code -> {
            log.info("RestTemplate non in cache, creazione per connettore: {}", code);
            Connettore connettore = getConnettore(code);
            return restTemplateFactory.createRestTemplate(connettore);
        });
    }

    public Connettore getConnettore(String codiceConnettore) {
        Connettore connettore = null;

        if (cacheEnabled) {
            connettore = connettoreCache.get(codiceConnettore);
        }

        if (connettore == null) {
            log.debug("Connettore non in cache, caricamento da database: {}", codiceConnettore);
            List<ConnettoreEntity> entities = connettoreEntityRepository.findByCodConnettoreAndAbilitato(codiceConnettore);

            if (entities.isEmpty()) {
                log.error("Connettore non trovato o non abilitato: {}", codiceConnettore);
                throw new IllegalArgumentException("Connettore non trovato o non abilitato: " + codiceConnettore);
            }

            connettore = ConnettoreConverter.toModel(entities);
            if (connettore == null || !connettore.isAbilitato()) {
                log.error("Connettore non abilitato: {}", codiceConnettore);
                throw new IllegalArgumentException("Connettore non abilitato: " + codiceConnettore);
            }

            if (cacheEnabled) {
                connettoreCache.put(codiceConnettore, connettore);
            }
        }

        return connettore;
    }

    /**
     * Verifica se un connettore esiste ed e' abilitato, senza sollevare
     * eccezioni ne' loggare a livello ERROR: a differenza di
     * {@link #getConnettore(String)}, qui l'assenza del connettore e' un
     * esito normale del controllo (es. servizio opzionale non configurato in
     * questo ambiente), non una condizione d'errore.
     *
     * @param codiceConnettore codice del connettore
     * @return true se il connettore esiste ed e' abilitato
     */
    public boolean isAbilitato(String codiceConnettore) {
        if (cacheEnabled) {
            Connettore cached = connettoreCache.get(codiceConnettore);
            if (cached != null) {
                return cached.isAbilitato();
            }
        }
        return !connettoreEntityRepository.findByCodConnettoreAndAbilitato(codiceConnettore).isEmpty();
    }

    public Optional<RestTemplate> getRestTemplateIfExists(String codiceConnettore) {
        try {
            return Optional.of(getRestTemplate(codiceConnettore));
        } catch (IllegalArgumentException e) {
            log.warn("Connettore non trovato: {}", codiceConnettore);
            return Optional.empty();
        }
    }

    /**
     * Restituisce un AsyncRestTemplateWrapper per eseguire chiamate HTTP asincrone.
     *
     * <p>Il wrapper utilizza il RestTemplate configurato per il connettore specificato
     * e un ExecutorService per gestire l'esecuzione asincrona.
     *
     * <p>Esempio d'uso:
     * <pre>{@code
     * AsyncRestTemplateWrapper asyncClient = connettoreService.getAsyncRestTemplate("MYPIVOT");
     *
     * // GET asincrono
     * CompletableFuture<ResponseEntity<FlussiDTO>> future =
     *     asyncClient.getForEntityAsync("/api/flussi", FlussiDTO.class);
     *
     * future.thenAccept(response ->
     *     log.info("Ricevuti {} flussi", response.getBody().size())
     * );
     *
     * // POST asincrono
     * CompletableFuture<ResponseEntity<RispostaDTO>> postFuture =
     *     asyncClient.postForEntityAsync("/api/create", request, RispostaDTO.class);
     * }</pre>
     *
     * @param codiceConnettore Codice identificativo del connettore
     * @return AsyncRestTemplateWrapper configurato per chiamate asincrone
     * @throws IllegalArgumentException se il connettore non esiste o non è abilitato
     */
    public AsyncRestTemplateWrapper getAsyncRestTemplate(String codiceConnettore) {
        log.debug("Richiesta AsyncRestTemplate per connettore: {}", codiceConnettore);
        RestTemplate restTemplate = getRestTemplate(codiceConnettore);
        return new AsyncRestTemplateWrapper(restTemplate, asyncHttpExecutor);
    }

    /**
     * Restituisce un Optional con AsyncRestTemplateWrapper se il connettore esiste.
     *
     * <p>Versione "safe" che non lancia eccezioni se il connettore non esiste.
     *
     * @param codiceConnettore Codice identificativo del connettore
     * @return Optional con AsyncRestTemplateWrapper, vuoto se il connettore non esiste
     */
    public Optional<AsyncRestTemplateWrapper> getAsyncRestTemplateIfExists(String codiceConnettore) {
        try {
            return Optional.of(getAsyncRestTemplate(codiceConnettore));
        } catch (IllegalArgumentException e) {
            log.warn("Connettore non trovato per async: {}", codiceConnettore);
            return Optional.empty();
        }
    }

    public void invalidateCache(String codiceConnettore) {
        if (!cacheEnabled) {
            log.debug("Cache disabilitata, operazione invalidateCache ignorata");
            return;
        }
        log.info("Invalidazione cache per connettore: {}", codiceConnettore);
        restTemplateCache.remove(codiceConnettore);
        connettoreCache.remove(codiceConnettore);
    }

    public void reloadConnettore(String codiceConnettore) {
        log.info("Ricaricamento connettore: {}", codiceConnettore);
        if (cacheEnabled) {
            invalidateCache(codiceConnettore);
        }
        getRestTemplate(codiceConnettore);
    }

    public void clearCache() {
        if (!cacheEnabled) {
            log.debug("Cache disabilitata, operazione clearCache ignorata");
            return;
        }
        log.info("Pulizia completa cache");
        restTemplateCache.clear();
        connettoreCache.clear();
    }

    public void refreshCache() {
        if (!cacheEnabled) {
            log.debug("Cache disabilitata, operazione refreshCache ignorata");
            return;
        }
        log.info("Refresh completo cache");
        clearCache();
        preloadConnettori();
    }

    public int getCacheSize() {
        return cacheEnabled ? connettoreCache.size() : 0;
    }

    public boolean isInCache(String codiceConnettore) {
        return cacheEnabled && connettoreCache.containsKey(codiceConnettore);
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Restituisce le proprieta' di un connettore come mappa chiave-valore.
     *
     * <p>Utile per connettori con proprieta' variabili (es. notifica pagamento)
     * che non rientrano nel modello fisso {@link Connettore}.
     *
     * @param codiceConnettore Codice identificativo del connettore
     * @return Mappa delle proprieta' (cod_proprieta &rarr; valore)
     * @throws IllegalArgumentException se il connettore non esiste
     */
    public Map<String, String> getConnettoreAsMap(String codiceConnettore) {
        log.debug("Caricamento connettore come mappa: {}", codiceConnettore);
        List<ConnettoreEntity> entities = connettoreEntityRepository.findByCodConnettore(codiceConnettore);

        if (entities.isEmpty()) {
            throw new IllegalArgumentException("Connettore non trovato: " + codiceConnettore);
        }

        return entities.stream()
                .collect(Collectors.toMap(
                        ConnettoreEntity::getCodProprieta,
                        ConnettoreEntity::getValore,
                        (v1, v2) -> v2,
                        LinkedHashMap::new));
    }

    /**
     * Restituisce la lista di tutti i connettori configurati nel database.
     *
     * @return Lista di tutti i connettori (abilitati e non)
     */
    public List<Connettore> getAllConnettori() {
        log.debug("Caricamento di tutti i connettori");
        List<ConnettoreEntity> entities = connettoreEntityRepository.findAll();

        Map<String, List<ConnettoreEntity>> groupedByCode = entities.stream()
                .collect(java.util.stream.Collectors.groupingBy(ConnettoreEntity::getCodConnettore));

        return groupedByCode.entrySet().stream()
                .map(entry -> ConnettoreConverter.toModel(entry.getValue()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Restituisce la lista di tutti i connettori abilitati.
     *
     * @return Lista dei connettori con ABILITATO=true
     */
    public List<Connettore> getAllAbilitati() {
        log.debug("Caricamento connettori abilitati");
        List<ConnettoreEntity> entities = connettoreEntityRepository.findAllAbilitati();

        Map<String, List<ConnettoreEntity>> groupedByCode = entities.stream()
                .collect(java.util.stream.Collectors.groupingBy(ConnettoreEntity::getCodConnettore));

        return groupedByCode.entrySet().stream()
                .map(entry -> ConnettoreConverter.toModel(entry.getValue()))
                .filter(java.util.Objects::nonNull)
                .filter(Connettore::isAbilitato)
                .toList();
    }
}
