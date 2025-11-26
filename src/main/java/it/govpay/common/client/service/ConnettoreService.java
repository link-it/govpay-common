package it.govpay.common.client.service;

import it.govpay.common.client.async.AsyncRestTemplateWrapper;
import it.govpay.common.client.converter.ConnettoreConverter;
import it.govpay.common.client.entity.ConnettoreEntity;
import it.govpay.common.client.model.Connettore;
import it.govpay.common.client.factory.RestTemplateFactory;
import it.govpay.common.client.repository.ConnettoreEntityRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
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

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
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
     * Restituisce la lista di tutti i connettori configurati nel database.
     *
     * @return Lista di tutti i connettori (abilitati e non)
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
