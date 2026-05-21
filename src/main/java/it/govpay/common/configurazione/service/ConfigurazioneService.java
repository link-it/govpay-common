package it.govpay.common.configurazione.service;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import it.govpay.common.client.model.Connettore;
import it.govpay.common.client.service.ConnettoreService;
import it.govpay.common.configurazione.ConfigurazioneKeys;
import it.govpay.common.configurazione.model.AppIOBatch;
import it.govpay.common.configurazione.model.AvvisaturaViaAppIo;
import it.govpay.common.configurazione.model.AvvisaturaViaMail;
import it.govpay.common.configurazione.model.Giornale;
import it.govpay.common.configurazione.model.Hardening;
import it.govpay.common.configurazione.model.MailBatch;
import it.govpay.common.configurazione.model.TracciatoCsv;
import it.govpay.common.entity.ConfigurazioneEntity;
import it.govpay.common.repository.ConfigurazioneRepository;

@Service
@Transactional(readOnly = true)
public class ConfigurazioneService {

    private final ConfigurazioneRepository repository;
    private final ObjectMapper objectMapper;
    private final ConnettoreService connettoreService;

    public ConfigurazioneService(ConfigurazioneRepository repository, ObjectMapper objectMapper,
            ConnettoreService connettoreService) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.connettoreService = connettoreService;
    }

    /**
     * Restituisce il valore grezzo di una configurazione.
     *
     * @param nome il nome della configurazione
     * @return il valore, se presente
     */
    public Optional<String> getValore(String nome) {
        return repository.findByNome(nome)
                .map(ConfigurazioneEntity::getValore);
    }

    /**
     * Restituisce tutte le configurazioni come mappa nome-valore.
     *
     * @return mappa di tutte le configurazioni
     */
    public Map<String, String> getAsMap() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(
                        ConfigurazioneEntity::getNome,
                        e -> e.getValore() != null ? e.getValore() : ""));
    }

    /**
     * Deserializza il valore JSON di una configurazione nel tipo specificato.
     *
     * @param nome il nome della configurazione
     * @param type il tipo target per la deserializzazione
     * @param <T> il tipo generico
     * @return l'oggetto deserializzato, se la configurazione esiste
     * @throws IllegalArgumentException se il valore non e' un JSON valido per il tipo richiesto
     */
    public <T> Optional<T> getAsObject(String nome, Class<T> type) {
        return repository.findByNome(nome)
                .map(entity -> {
                    try {
                        return objectMapper.readValue(entity.getValore(), type);
                    } catch (JacksonException e) {
                        throw new IllegalArgumentException(
                                "Errore nella deserializzazione della configurazione '" + nome + "': " + e.getMessage(), e);
                    }
                });
    }

    // Typed getters

    public Optional<Giornale> getGiornale() {
        return getAsObject(ConfigurazioneKeys.KEY_GIORNALE_EVENTI, Giornale.class);
    }

    public Optional<TracciatoCsv> getTracciatoCsv() {
        return getAsObject(ConfigurazioneKeys.KEY_TRACCIATO_CSV, TracciatoCsv.class);
    }

    public Optional<Hardening> getHardening() {
        return getAsObject(ConfigurazioneKeys.KEY_HARDENING, Hardening.class);
    }

    public Optional<MailBatch> getMailBatch() {
        return getAsObject(ConfigurazioneKeys.KEY_MAIL_BATCH, MailBatch.class);
    }

    public Optional<AppIOBatch> getAppIOBatch() {
        return getAsObject(ConfigurazioneKeys.KEY_APP_IO_BATCH, AppIOBatch.class);
    }

    public Optional<AvvisaturaViaMail> getAvvisaturaViaMail() {
        return getAsObject(ConfigurazioneKeys.KEY_AVVISATURA_MAIL, AvvisaturaViaMail.class);
    }

    public Optional<AvvisaturaViaAppIo> getAvvisaturaViaAppIo() {
        return getAsObject(ConfigurazioneKeys.KEY_AVVISATURA_APP_IO, AvvisaturaViaAppIo.class);
    }

    public Connettore getServizioGDE() {
        return connettoreService.getConnettore(ConfigurazioneKeys.COD_CONNETTORE_GDE);
    }

    public RestTemplate getRestTemplateGDE() {
        return connettoreService.getRestTemplate(ConfigurazioneKeys.COD_CONNETTORE_GDE);
    }

    public boolean isServizioGDEAbilitato() {
        return connettoreService.getConnettore(ConfigurazioneKeys.COD_CONNETTORE_GDE).isAbilitato();
    }
}
