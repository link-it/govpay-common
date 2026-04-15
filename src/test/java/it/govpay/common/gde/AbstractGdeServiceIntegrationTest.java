package it.govpay.common.gde;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.ObjectMapper;

import it.govpay.common.client.TestApplication;
import it.govpay.common.configurazione.model.GdeInterfaccia;
import it.govpay.common.configurazione.model.Giornale;
import it.govpay.common.configurazione.service.ConfigurazioneService;
import it.govpay.gde.client.beans.ComponenteEvento;
import it.govpay.gde.client.beans.EsitoEvento;
import it.govpay.gde.client.beans.NuovoEvento;

/**
 * Test di integrazione per {@link AbstractGdeService} con configurazione Giornale letta dal DB.
 * <p>
 * Usa il database H2 precaricato con data.sql che contiene:
 * <pre>
 * giornale_eventi = {
 *   "apiEnte": { "letture": {"log":"SEMPRE","dump":"SEMPRE"}, "scritture": {"log":"SEMPRE","dump":"SOLO_ERRORE"} },
 *   "apiPagamento": { "letture": {"log":"MAI","dump":"MAI"}, "scritture": {"log":"SEMPRE","dump":"SEMPRE"} }
 * }
 * </pre>
 */
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class AbstractGdeServiceIntegrationTest {

    private static final String GDE_ENDPOINT = "http://gde.test/eventi";

    @Autowired
    private ConfigurazioneService configurazioneService;

    @Autowired
    private ObjectMapper objectMapper;

    private RestTemplate restTemplate;
    private TestGdeService gdeService;

    /**
     * Implementazione concreta per i test che delega a GdeUtils per il mapping delle componenti
     * e usa il metodo HTTP per classificare letture/scritture.
     */
    static class TestGdeService extends AbstractGdeService {
        private final String endpoint;
        private final RestTemplate restTemplate;

        TestGdeService(ObjectMapper objectMapper, Executor asyncExecutor,
                      ConfigurazioneService configurazioneService, String endpoint,
                      RestTemplate restTemplate) {
            super(objectMapper, asyncExecutor, configurazioneService);
            this.endpoint = endpoint;
            this.restTemplate = restTemplate;
        }

        @Override
        protected NuovoEvento convertToGdeEvent(GdeEventInfo eventInfo) {
            NuovoEvento evento = new NuovoEvento();
            evento.setEsito(eventInfo.getEsito());
            evento.setComponente(eventInfo.getComponente());
            return evento;
        }

        @Override
        protected String getGdeEndpoint() { return endpoint; }

        @Override
        protected RestTemplate getGdeRestTemplate() { return restTemplate; }

        @Override
        protected GdeInterfaccia getConfigurazioneComponente(ComponenteEvento componente, Giornale giornale) {
            return GdeUtils.getConfigurazioneComponente(componente, giornale);
        }
    }

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        Executor syncExecutor = Runnable::run;
        gdeService = new TestGdeService(objectMapper, syncExecutor, configurazioneService,
                GDE_ENDPOINT, restTemplate);
    }

    // ==================== apiEnte: letture=SEMPRE/SEMPRE, scritture=SEMPRE/SOLO_ERRORE ====================

    @Nested
    @DisplayName("apiEnte - configurazione dal DB")
    class ApiEnte {

        @Test
        @DisplayName("Lettura OK - log=SEMPRE: evento inviato con payload (dump=SEMPRE)")
        void letturaOk_logSempre_dumpSempre() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_ENTE)
                    .metodoHttp("GET")
                    .esito(EsitoEvento.OK)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
            assertNotNull(eventInfo.getPayloadRichiesta(), "dump=SEMPRE: payload richiesta deve essere presente");
            assertNotNull(eventInfo.getPayloadRisposta(), "dump=SEMPRE: payload risposta deve essere presente");
        }

        @Test
        @DisplayName("Lettura KO - log=SEMPRE: evento inviato")
        void letturaKo_logSempre() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_ENTE)
                    .metodoHttp("GET")
                    .esito(EsitoEvento.KO)
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
        }

        @Test
        @DisplayName("Scrittura OK - log=SEMPRE, dump=SOLO_ERRORE: evento inviato senza payload")
        void scritturaOk_dumpSoloErrore_payloadRimossi() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_ENTE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.OK)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
            assertNull(eventInfo.getPayloadRichiesta(), "dump=SOLO_ERRORE con esito OK: payload richiesta rimosso");
            assertNull(eventInfo.getPayloadRisposta(), "dump=SOLO_ERRORE con esito OK: payload risposta rimosso");
        }

        @Test
        @DisplayName("Scrittura KO - log=SEMPRE, dump=SOLO_ERRORE: evento inviato con payload")
        void scritturaKo_dumpSoloErrore_payloadMantenuti() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_ENTE)
                    .metodoHttp("PUT")
                    .esito(EsitoEvento.KO)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
            assertNotNull(eventInfo.getPayloadRichiesta(), "dump=SOLO_ERRORE con esito KO: payload richiesta mantenuto");
            assertNotNull(eventInfo.getPayloadRisposta(), "dump=SOLO_ERRORE con esito KO: payload risposta mantenuto");
        }
    }

    // ==================== apiPagamento: letture=MAI/MAI, scritture=SEMPRE/SEMPRE ====================

    @Nested
    @DisplayName("apiPagamento - configurazione dal DB")
    class ApiPagamento {

        @Test
        @DisplayName("Lettura OK - log=MAI: evento NON inviato")
        void letturaOk_logMai_nonInviato() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_PAGAMENTO)
                    .metodoHttp("GET")
                    .esito(EsitoEvento.OK)
                    .build();

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("Lettura KO - log=MAI: evento NON inviato neanche in caso di errore")
        void letturaKo_logMai_nonInviato() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_PAGAMENTO)
                    .metodoHttp("HEAD")
                    .esito(EsitoEvento.KO)
                    .build();

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("Scrittura OK - log=SEMPRE, dump=SEMPRE: evento inviato con payload")
        void scritturaOk_logSempre_dumpSempre() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_PAGAMENTO)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.OK)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
            assertNotNull(eventInfo.getPayloadRichiesta());
            assertNotNull(eventInfo.getPayloadRisposta());
        }

        @Test
        @DisplayName("Scrittura KO - log=SEMPRE, dump=SEMPRE: evento inviato con payload")
        void scritturaKo_logSempre_dumpSempre() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_PAGAMENTO)
                    .metodoHttp("DELETE")
                    .esito(EsitoEvento.KO)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
            assertNotNull(eventInfo.getPayloadRichiesta());
            assertNotNull(eventInfo.getPayloadRisposta());
        }
    }

    // ==================== Componenti non configurate nel DB ====================

    @Nested
    @DisplayName("Componente non configurata nel DB")
    class ComponenteNonConfigurata {

        @Test
        @DisplayName("apiBackoffice non presente nel giornale DB - evento inviato (policy non applicabile)")
        void componenteAssente_eventoInviato() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.OK)
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
        }

        @Test
        @DisplayName("apiRagioneria non presente nel giornale DB - evento inviato con payload intatti")
        void componenteAssente_payloadIntatti() {
            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_RAGIONERIA)
                    .metodoHttp("GET")
                    .esito(EsitoEvento.OK)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
            assertNotNull(eventInfo.getPayloadRichiesta());
            assertNotNull(eventInfo.getPayloadRisposta());
        }
    }
}
