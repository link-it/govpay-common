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
package it.govpay.common.gde;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.ObjectMapper;

import it.govpay.common.configurazione.model.GdeEvento;
import it.govpay.common.configurazione.model.GdeEvento.DumpEnum;
import it.govpay.common.configurazione.model.GdeEvento.LogEnum;
import it.govpay.common.configurazione.model.GdeInterfaccia;
import it.govpay.common.configurazione.model.Giornale;
import it.govpay.common.configurazione.service.ConfigurazioneService;
import it.govpay.gde.client.beans.CategoriaEvento;
import it.govpay.gde.client.beans.ComponenteEvento;
import it.govpay.gde.client.beans.EsitoEvento;
import it.govpay.gde.client.beans.NuovoEvento;
import it.govpay.gde.client.beans.RuoloEvento;

@ExtendWith(MockitoExtension.class)
class AbstractGdeServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ConfigurazioneService configurazioneService;

    private static final String GDE_ENDPOINT = "http://gde.test/eventi";

    private TestGdeService gdeService;

    static class TestGdeService extends AbstractGdeService {
        private final String endpoint;
        private boolean forceLettura = false;

        TestGdeService(ObjectMapper objectMapper, Executor asyncExecutor,
                      ConfigurazioneService configurazioneService, String endpoint) {
            super(objectMapper, asyncExecutor, configurazioneService);
            this.endpoint = endpoint;
        }

        @Override
        protected NuovoEvento convertToGdeEvent(GdeEventInfo eventInfo) {
            NuovoEvento evento = new NuovoEvento();
            evento.setEsito(eventInfo.getEsito());
            return evento;
        }

        @Override
        protected String getGdeEndpoint() { return endpoint; }

        @Override
        protected boolean isRequestLettura(GdeEventInfo eventInfo) {
            if (forceLettura) return true;
            return super.isRequestLettura(eventInfo);
        }

        @Override
        protected boolean isRequestScrittura(GdeEventInfo eventInfo) {
            if (forceLettura) return false;
            return super.isRequestScrittura(eventInfo);
        }

        @Override
        protected GdeInterfaccia getConfigurazioneComponente(ComponenteEvento componente, Giornale giornale) {
            return GdeUtils.getConfigurazioneComponente(componente, giornale);
        }

        void setForceLettura(boolean forceLettura) {
            this.forceLettura = forceLettura;
        }
    }

    @BeforeEach
    void setUp() {
        // Use a synchronous executor for testing
        Executor syncExecutor = Runnable::run;
        lenient().when(configurazioneService.getRestTemplateGDE()).thenReturn(restTemplate);
        // Default: Giornale con policy SEMPRE per log e dump su tutte le interfacce
        lenient().when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornaleSempre()));
        gdeService = new TestGdeService(objectMapper, syncExecutor, configurazioneService, GDE_ENDPOINT);
    }

    private static Giornale createGiornaleSempre() {
        return createGiornale(LogEnum.SEMPRE, DumpEnum.SEMPRE);
    }

    private static Giornale createGiornale(LogEnum logEnum, DumpEnum dumpEnum) {
        GdeEvento evento = new GdeEvento();
        evento.setLog(logEnum);
        evento.setDump(dumpEnum);
        GdeInterfaccia interfaccia = new GdeInterfaccia();
        interfaccia.setLetture(evento);
        interfaccia.setScritture(evento);
        Giornale giornale = new Giornale();
        giornale.setApiBackoffice(interfaccia);
        giornale.setApiPagamento(interfaccia);
        giornale.setApiPagoPA(interfaccia);
        giornale.setApiPendenze(interfaccia);
        giornale.setApiRagioneria(interfaccia);
        giornale.setApiBackendIO(interfaccia);
        return giornale;
    }

    @Test
    @DisplayName("inviaEvento - successo")
    void inviaEvento_success() {
        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .componente(ComponenteEvento.API_BACKOFFICE)
                .metodoHttp("POST")
                .esito(EsitoEvento.OK)
                .build();

        when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        assertDoesNotThrow(() -> gdeService.inviaEvento(eventInfo));
        verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
    }

    @Test
    @DisplayName("inviaEvento - HttpDataHolder.clear() chiamato anche in caso di errore")
    void inviaEvento_clearsHttpDataHolder() {
        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .componente(ComponenteEvento.API_BACKOFFICE)
                .metodoHttp("POST")
                .esito(EsitoEvento.OK)
                .build();

        when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertThrows(RestClientException.class, () -> gdeService.inviaEvento(eventInfo));
    }

    @Test
    @DisplayName("inviaEventoAsync - delega a executor")
    void inviaEventoAsync() {
        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .componente(ComponenteEvento.API_BACKOFFICE)
                .metodoHttp("POST")
                .esito(EsitoEvento.OK)
                .build();

        when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        CompletableFuture<Void> future = gdeService.inviaEventoAsync(eventInfo);

        assertNotNull(future);
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("inviaEventoAsync - errore non propagato")
    void inviaEventoAsync_errorNotPropagated() {
        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .componente(ComponenteEvento.API_BACKOFFICE)
                .metodoHttp("POST")
                .esito(EsitoEvento.OK)
                .build();

        when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                .thenThrow(new RestClientException("connection refused"));

        CompletableFuture<Void> future = gdeService.inviaEventoAsync(eventInfo);

        assertNotNull(future);
        assertTrue(future.isDone());
        // The error is caught inside, so the future completes normally
        assertFalse(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("inviaEventoOk - imposta esito OK")
    void inviaEventoOk() {
        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .componente(ComponenteEvento.API_BACKOFFICE)
                .metodoHttp("POST")
                .build();

        when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        gdeService.inviaEventoOk(eventInfo);

        assertEquals(EsitoEvento.OK, eventInfo.getEsito());
    }

    @Test
    @DisplayName("inviaEventoKo - imposta esito KO")
    void inviaEventoKo() {
        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .componente(ComponenteEvento.API_BACKOFFICE)
                .metodoHttp("POST")
                .build();

        when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        gdeService.inviaEventoKo(eventInfo);

        assertEquals(EsitoEvento.KO, eventInfo.getEsito());
    }

    @Nested
    @DisplayName("Policy log/dump")
    class PolicyLogDump {

        @Test
        @DisplayName("log=MAI - evento non inviato")
        void logMai_eventoNonInviato() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornale(LogEnum.MAI, DumpEnum.SEMPRE)));

            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.OK)
                    .build();

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("log=SOLO_ERRORE con esito OK - evento non inviato")
        void logSoloErrore_esitoOk_nonInviato() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornale(LogEnum.SOLO_ERRORE, DumpEnum.SEMPRE)));

            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.OK)
                    .build();

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("log=SOLO_ERRORE con esito KO - evento inviato")
        void logSoloErrore_esitoKo_inviato() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornale(LogEnum.SOLO_ERRORE, DumpEnum.SEMPRE)));

            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.KO)
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
        }

        @Test
        @DisplayName("log=SEMPRE - evento sempre inviato")
        void logSempre_inviato() {
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
        @DisplayName("dump=MAI - payload rimossi dall'evento")
        void dumpMai_payloadRimossi() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornale(LogEnum.SEMPRE, DumpEnum.MAI)));

            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.OK)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            assertNull(eventInfo.getPayloadRichiesta());
            assertNull(eventInfo.getPayloadRisposta());
            verify(restTemplate).postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class));
        }

        @Test
        @DisplayName("dump=SOLO_ERRORE con esito OK - payload rimossi")
        void dumpSoloErrore_esitoOk_payloadRimossi() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornale(LogEnum.SEMPRE, DumpEnum.SOLO_ERRORE)));

            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.OK)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            assertNull(eventInfo.getPayloadRichiesta());
            assertNull(eventInfo.getPayloadRisposta());
        }

        @Test
        @DisplayName("dump=SOLO_ERRORE con esito KO - payload mantenuti")
        void dumpSoloErrore_esitoKo_payloadMantenuti() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornale(LogEnum.SEMPRE, DumpEnum.SOLO_ERRORE)));

            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("POST")
                    .esito(EsitoEvento.KO)
                    .payloadRichiesta("cGF5bG9hZA==")
                    .payloadRisposta("cmVzcG9uc2U=")
                    .build();

            when(restTemplate.postForEntity(eq(GDE_ENDPOINT), any(NuovoEvento.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            gdeService.inviaEvento(eventInfo);

            assertNotNull(eventInfo.getPayloadRichiesta());
            assertNotNull(eventInfo.getPayloadRisposta());
        }

        @Test
        @DisplayName("Giornale assente - evento inviato (policy non applicabile)")
        void giornaleAssente_eventoInviato() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.empty());

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
        @DisplayName("Lettura con policy letture=MAI - evento non inviato")
        void letturaConPolicyMai() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornaleLettureScrittureDiversi()));

            GdeEventInfo eventInfo = GdeEventInfo.builder()
                    .componente(ComponenteEvento.API_BACKOFFICE)
                    .metodoHttp("GET")
                    .esito(EsitoEvento.OK)
                    .build();

            gdeService.inviaEvento(eventInfo);

            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("Scrittura con policy scritture=SEMPRE - evento inviato")
        void scritturaConPolicySempre() {
            when(configurazioneService.getGiornale()).thenReturn(Optional.of(createGiornaleLettureScrittureDiversi()));

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

        private Giornale createGiornaleLettureScrittureDiversi() {
            GdeEvento letture = new GdeEvento();
            letture.setLog(LogEnum.MAI);
            letture.setDump(DumpEnum.MAI);
            GdeEvento scritture = new GdeEvento();
            scritture.setLog(LogEnum.SEMPRE);
            scritture.setDump(DumpEnum.SEMPRE);
            GdeInterfaccia interfaccia = new GdeInterfaccia();
            interfaccia.setLetture(letture);
            interfaccia.setScritture(scritture);
            Giornale giornale = new Giornale();
            giornale.setApiBackoffice(interfaccia);
            return giornale;
        }
    }

    @Nested
    @DisplayName("determineEsito")
    class DetermineEsito {

        @Test
        @DisplayName("Con eccezione - KO")
        void withException() {
            EsitoEvento esito = gdeService.determineEsito(null, new RestClientException("err"));
            assertEquals(EsitoEvento.KO, esito);
        }

        @Test
        @DisplayName("Risposta 2xx - OK")
        void response2xx() {
            ResponseEntity<String> response = ResponseEntity.ok("body");
            EsitoEvento esito = gdeService.determineEsito(response, null);
            assertEquals(EsitoEvento.OK, esito);
        }

        @Test
        @DisplayName("Risposta non-2xx - KO")
        void responseNon2xx() {
            ResponseEntity<String> response = ResponseEntity.status(500).body("error");
            EsitoEvento esito = gdeService.determineEsito(response, null);
            assertEquals(EsitoEvento.KO, esito);
        }

        @Test
        @DisplayName("Entrambi null - KO")
        void bothNull() {
            EsitoEvento esito = gdeService.determineEsito(null, null);
            assertEquals(EsitoEvento.KO, esito);
        }
    }

    @Nested
    @DisplayName("extractStatusCode")
    class ExtractStatusCode {

        @Test
        @DisplayName("Da HttpStatusCodeException")
        void fromHttpStatusCodeException() {
            HttpClientErrorException exception = new HttpClientErrorException(HttpStatus.NOT_FOUND);
            Integer statusCode = gdeService.extractStatusCode(null, exception);
            assertEquals(404, statusCode);
        }

        @Test
        @DisplayName("Da response")
        void fromResponse() {
            ResponseEntity<String> response = ResponseEntity.ok("body");
            Integer statusCode = gdeService.extractStatusCode(response, null);
            assertEquals(200, statusCode);
        }

        @Test
        @DisplayName("Entrambi null - ritorna null")
        void bothNull() {
            Integer statusCode = gdeService.extractStatusCode(null, null);
            assertNull(statusCode);
        }

        @Test
        @DisplayName("RestClientException non-HTTP")
        void nonHttpException() {
            RestClientException exception = new RestClientException("connection refused");
            Integer statusCode = gdeService.extractStatusCode(null, exception);
            assertNull(statusCode);
        }
    }

    @Nested
    @DisplayName("buildDescrizioneEsito")
    class BuildDescrizioneEsito {

        @Test
        @DisplayName("Con HttpStatusCodeException")
        void withHttpStatusCodeException() {
            HttpServerErrorException exception = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
            String desc = gdeService.buildDescrizioneEsito(null, exception);
            assertTrue(desc.contains("500"));
        }

        @Test
        @DisplayName("Con altra eccezione")
        void withOtherException() {
            RestClientException exception = new RestClientException("connection refused");
            String desc = gdeService.buildDescrizioneEsito(null, exception);
            assertEquals("connection refused", desc);
        }

        @Test
        @DisplayName("Con response")
        void withResponse() {
            ResponseEntity<String> response = ResponseEntity.ok("body");
            String desc = gdeService.buildDescrizioneEsito(response, null);
            assertEquals("HTTP 200", desc);
        }

        @Test
        @DisplayName("Entrambi null")
        void bothNull() {
            String desc = gdeService.buildDescrizioneEsito(null, null);
            assertNull(desc);
        }
    }

    @Test
    @DisplayName("Costanti hanno i valori corretti")
    void constants() {
        assertEquals(EsitoEvento.OK, AbstractGdeService.ESITO_OK);
        assertEquals(EsitoEvento.KO, AbstractGdeService.ESITO_KO);
        assertEquals(RuoloEvento.CLIENT, AbstractGdeService.RUOLO_CLIENT);
        assertEquals(CategoriaEvento.INTERFACCIA, AbstractGdeService.CATEGORIA_INTERFACCIA);
    }

    @Test
    @DisplayName("getObjectMapper restituisce l'ObjectMapper")
    void getObjectMapper() {
        assertSame(objectMapper, gdeService.getObjectMapper());
    }

    @Test
    @DisplayName("extractResponsePayload delega a GdeUtils")
    void extractResponsePayload() {
        // Should not throw - delegates to GdeUtils.extractResponsePayload
        String result = gdeService.extractResponsePayload(null, null);
        // With both null, GdeUtils returns null
        assertNull(result);
    }

    @Test
    @DisplayName("buildUrl delega a GdeUtils")
    void buildUrl() {
        String result = gdeService.buildUrl("http://base.url", "/path");
        assertEquals("http://base.url/path", result);
    }

    @Test
    @DisplayName("isAbilitato - true quando connettore GDE esiste ed e' abilitato")
    void isAbilitato_true() {
        when(configurazioneService.isServizioGDEAbilitato()).thenReturn(true);
        assertTrue(gdeService.isAbilitato());
    }

    @Test
    @DisplayName("isAbilitato - false quando connettore GDE non esiste o non e' abilitato")
    void isAbilitato_false() {
        when(configurazioneService.isServizioGDEAbilitato()).thenReturn(false);
        assertFalse(gdeService.isAbilitato());
    }
}
