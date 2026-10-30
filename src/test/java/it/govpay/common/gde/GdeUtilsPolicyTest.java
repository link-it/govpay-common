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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import it.govpay.common.client.gde.HttpDataHolder;
import it.govpay.common.configurazione.model.GdeEvento;
import it.govpay.common.configurazione.model.GdeInterfaccia;
import it.govpay.common.configurazione.model.Giornale;
import it.govpay.gde.client.beans.ComponenteEvento;
import it.govpay.gde.client.beans.EsitoEvento;
import it.govpay.gde.client.beans.Header;

/**
 * Copre la classificazione delle operazioni, la valutazione delle policy di
 * log e dump, la risoluzione della configurazione per componente e i costruttori
 * di header tipizzati GDE.
 */
@DisplayName("Test GdeUtils: policy, classificazione operazioni e header")
class GdeUtilsPolicyTest {

    @AfterEach
    void pulisciContesto() {
        HttpDataHolder.clear();
    }

    @Nested
    @DisplayName("Classificazione operazioni")
    class ClassificazioneOperazioni {

        /**
         * L'elenco completo, non un campione: se una di queste operazioni smettesse
         * di essere classificata come scrittura, il giornale degli eventi cambierebbe
         * comportamento in silenzio per quel flusso.
         */
        @ParameterizedTest
        @ValueSource(strings = { "nodoChiediCopiaRT", "nodoChiediStatoRPT", "nodoInviaRPT",
                "nodoInviaCarrelloRPT", "nodoInviaRichiestaStorno", "nodoInviaRispostaRevoca",
                "paaVerificaRPT", "paaAttivaRPT", "paaInviaEsitoStorno", "paaInviaRichiestaRevoca",
                "paaInviaRT", "paVerifyPaymentNotice", "paGetPayment", "paGetPaymentV2", "paSendRT",
                "paSendRTV2" })
        @DisplayName("Tutte le operazioni pagoPA di scrittura sono riconosciute")
        void operazioniDiScrittura(String operazione) {
            assertTrue(GdeUtils.isOperazioneScrittura(operazione));
            // e di riflesso non sono letture per il componente API_PAGOPA
            assertFalse(GdeUtils.isRequestLettura("GET", ComponenteEvento.API_PAGOPA, operazione));
        }

        @Test
        @DisplayName("Un'operazione sconosciuta o nulla non e' di scrittura")
        void operazioneSconosciuta() {
            assertFalse(GdeUtils.isOperazioneScrittura("operazioneInesistente"));
            assertFalse(GdeUtils.isOperazioneScrittura(null));
        }

        /** Anche qui l'elenco e' completo, per la stessa ragione. */
        @ParameterizedTest
        @ValueSource(strings = { "pivotInviaTracciatoEmail", "pivotInviaTracciatoFileSystem",
                "pivotSILInviaFlusso", "secimInviaTracciatoEmail", "secimInviaTracciatoFileSystem",
                "govpayInviaTracciatoEmail", "govpayInviaTracciatoFileSystem",
                "govpayInviaTracciatoRest", "inviaFlussoRendicontazione", "inviaRpp",
                "inviaSintesiFlussiRendicontazione", "inviaSintesiPagamenti",
                "hyperSicAPKappaInviaTracciatoEmail", "hyperSicAPKappaInviaTracciatoFileSystem" })
        @DisplayName("Tutte le spedizioni di tracciati sono operazioni di scrittura")
        void spedizioniTracciati(String operazione) {
            assertTrue(GdeUtils.isOperazioneScritturaTracciatiNotificaPagamenti(operazione));
            assertTrue(GdeUtils.isRequestScrittura("GET", ComponenteEvento.API_GOVPAY, operazione));
        }

        @Test
        @DisplayName("Un'operazione fuori elenco non e' spedizione di tracciato")
        void tracciatoSconosciuto() {
            assertFalse(GdeUtils.isOperazioneScritturaTracciatiNotificaPagamenti("altro"));
            assertFalse(GdeUtils.isOperazioneScritturaTracciatiNotificaPagamenti(null));
        }
    }

    @Nested
    @DisplayName("Lettura e scrittura per componente")
    class LetturaScritturaPerComponente {

        @Test
        @DisplayName("Su API_PAGOPA conta l'operazione, non il metodo HTTP")
        void pagoPaGuardaLOperazione() {
            // Il metodo e' GET ma l'operazione e' di scrittura: vince l'operazione
            assertFalse(GdeUtils.isRequestLettura("GET", ComponenteEvento.API_PAGOPA, "nodoInviaRPT"));
            assertTrue(GdeUtils.isRequestScrittura("GET", ComponenteEvento.API_PAGOPA, "nodoInviaRPT"));

            assertTrue(GdeUtils.isRequestLettura("POST", ComponenteEvento.API_PAGOPA, "operazioneDiLettura"));
            assertFalse(GdeUtils.isRequestScrittura("POST", ComponenteEvento.API_PAGOPA, "operazioneDiLettura"));
        }

        @Test
        @DisplayName("Operazione nulla su API_PAGOPA non e' lettura")
        void pagoPaSenzaOperazione() {
            assertFalse(GdeUtils.isRequestLettura("GET", ComponenteEvento.API_PAGOPA, null));
        }

        @Test
        @DisplayName("Sui componenti di notifica pagamenti conta la spedizione del tracciato")
        void componentiDiNotifica() {
            assertFalse(GdeUtils.isRequestLettura("GET", ComponenteEvento.API_GOVPAY,
                    "govpayInviaTracciatoEmail"));
            assertTrue(GdeUtils.isRequestScrittura("GET", ComponenteEvento.API_GOVPAY,
                    "govpayInviaTracciatoEmail"));
            assertTrue(GdeUtils.isRequestLettura("POST", ComponenteEvento.API_MYPIVOT, "altraOperazione"));
        }

        @Test
        @DisplayName("Sugli altri componenti decide il metodo HTTP")
        void altriComponentiGuardanoIlMetodo() {
            assertTrue(GdeUtils.isRequestLettura("GET", ComponenteEvento.API_PENDENZE, "qualunque"));
            assertFalse(GdeUtils.isRequestLettura("POST", ComponenteEvento.API_PENDENZE, "qualunque"));
            assertTrue(GdeUtils.isRequestScrittura("POST", ComponenteEvento.API_PENDENZE, "qualunque"));
        }
    }

    @Nested
    @DisplayName("Policy di log e dump")
    class PolicyLogDump {

        private GdeEvento evento(GdeEvento.LogEnum log, GdeEvento.DumpEnum dump) {
            GdeEvento e = new GdeEvento();
            e.setLog(log);
            e.setDump(dump);
            return e;
        }

        @Test
        @DisplayName("SEMPRE e MAI non dipendono dall'esito")
        void sempreEMai() {
            assertTrue(GdeUtils.logEvento(evento(GdeEvento.LogEnum.SEMPRE, null), EsitoEvento.KO));
            assertTrue(GdeUtils.logEvento(evento(GdeEvento.LogEnum.SEMPRE, null), EsitoEvento.OK));
            assertFalse(GdeUtils.logEvento(evento(GdeEvento.LogEnum.MAI, null), EsitoEvento.KO));
        }

        @Test
        @DisplayName("SOLO_ERRORE logga tutto tranne l'esito OK")
        void soloErroreConEsito() {
            GdeEvento e = evento(GdeEvento.LogEnum.SOLO_ERRORE, GdeEvento.DumpEnum.SOLO_ERRORE);
            assertFalse(GdeUtils.logEvento(e, EsitoEvento.OK));
            assertTrue(GdeUtils.logEvento(e, EsitoEvento.KO));
            assertTrue(GdeUtils.logEvento(e, EsitoEvento.FAIL));
            assertFalse(GdeUtils.dumpEvento(e, EsitoEvento.OK));
            assertTrue(GdeUtils.dumpEvento(e, EsitoEvento.KO));
        }

        @Test
        @DisplayName("SOLO_ERRORE sullo status code usa la soglia 399")
        void soloErroreConStatusCode() {
            GdeEvento e = evento(GdeEvento.LogEnum.SOLO_ERRORE, GdeEvento.DumpEnum.SOLO_ERRORE);
            assertFalse(GdeUtils.logEvento(e, 200));
            assertFalse(GdeUtils.logEvento(e, 399));
            assertTrue(GdeUtils.logEvento(e, 400));
            assertTrue(GdeUtils.dumpEvento(e, 500));
            assertFalse(GdeUtils.dumpEvento(e, 204));
        }

        @Test
        @DisplayName("Evento nullo o policy non impostata: non si logga")
        void eventoOPolicyAssenti() {
            assertFalse(GdeUtils.logEvento(null, EsitoEvento.KO));
            assertFalse(GdeUtils.dumpEvento(null, EsitoEvento.KO));
            assertFalse(GdeUtils.logEvento(null, 500));
            assertFalse(GdeUtils.dumpEvento(null, 500));
            assertFalse(GdeUtils.logEvento(evento(null, null), EsitoEvento.KO));
            assertFalse(GdeUtils.dumpEvento(evento(null, null), EsitoEvento.KO));
            assertFalse(GdeUtils.logEvento(evento(null, null), 500));
            assertFalse(GdeUtils.dumpEvento(evento(null, null), 500));
        }
    }

    @Nested
    @DisplayName("Configurazione per componente")
    class ConfigurazionePerComponente {

        @Test
        @DisplayName("Ogni componente mappato risolve la propria interfaccia")
        void componenteMappato() {
            Giornale giornale = new Giornale();
            GdeInterfaccia pendenze = new GdeInterfaccia();
            GdeInterfaccia pagoPA = new GdeInterfaccia();
            giornale.setApiPendenze(pendenze);
            giornale.setApiPagoPA(pagoPA);

            assertSame(pendenze, GdeUtils.getConfigurazioneComponente(ComponenteEvento.API_PENDENZE, giornale));
            assertSame(pagoPA, GdeUtils.getConfigurazioneComponente(ComponenteEvento.API_PAGOPA, giornale));
        }

        @Test
        @DisplayName("Componente nullo o non mappato: nessuna configurazione")
        void componenteNonMappato() {
            assertNull(GdeUtils.getConfigurazioneComponente(null, new Giornale()));
            assertNull(GdeUtils.getConfigurazioneComponente(ComponenteEvento.GOVPAY, new Giornale()));
        }
    }

    @Nested
    @DisplayName("Header tipizzati GDE")
    class HeaderTipizzati {

        @Test
        @DisplayName("createGdeHeader costruisce nome e valore")
        void creaHeader() {
            Header h = GdeUtils.createGdeHeader("X-Custom", "valore");
            assertEquals("X-Custom", h.getNome());
            assertEquals("valore", h.getValore());
        }

        @Test
        @DisplayName("Gli header standard di una GET non dichiarano il content type")
        void headerStandardGet() {
            List<Header> get = GdeUtils.createStandardRequestGdeHeaders(true);
            assertTrue(get.stream().anyMatch(h -> HttpHeaders.ACCEPT.equalsIgnoreCase(h.getNome())));
            assertFalse(get.stream().anyMatch(h -> HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(h.getNome())));

            List<Header> post = GdeUtils.createStandardRequestGdeHeaders(false);
            assertTrue(post.stream().anyMatch(h -> HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(h.getNome())));
        }

        @Test
        @DisplayName("addXRequestIdGdeHeader aggiunge l'header solo se c'e' un valore")
        void aggiungeXRequestId() {
            List<Header> headers = new ArrayList<>();
            GdeUtils.addXRequestIdGdeHeader(headers, "abc-123");
            assertEquals(1, headers.size());
            assertEquals(GdeUtils.HEADER_X_REQUEST_ID, headers.get(0).getNome());
            assertEquals("abc-123", headers.get(0).getValore());
        }

        @Test
        @DisplayName("Gli header catturati sono resi come header GDE")
        void headerCatturati() {
            HttpHeaders catturati = new HttpHeaders();
            catturati.add(HttpHeaders.ACCEPT, "application/json");
            catturati.add("X-Custom", "primo");
            catturati.add("X-Custom", "secondo");
            HttpDataHolder.setRequestHeaders(catturati);

            List<Header> headers = GdeUtils.getCapturedRequestHeadersAsGdeHeaders();

            assertEquals(2, headers.size());
            Header accept = headers.stream()
                    .filter(h -> HttpHeaders.ACCEPT.equalsIgnoreCase(h.getNome()))
                    .findFirst().orElseThrow();
            assertEquals("application/json", accept.getValore());
            // I valori multipli dello stesso header sono uniti con virgola
            Header custom = headers.stream()
                    .filter(h -> "X-Custom".equalsIgnoreCase(h.getNome()))
                    .findFirst().orElseThrow();
            assertEquals("primo, secondo", custom.getValore());
        }

        @Test
        @DisplayName("Senza header catturati la lista e' vuota")
        void nessunHeaderCatturato() {
            assertTrue(GdeUtils.getCapturedRequestHeadersAsGdeHeaders().isEmpty());
        }
    }
}
