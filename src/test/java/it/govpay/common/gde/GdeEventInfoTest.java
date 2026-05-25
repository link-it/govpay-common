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

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import it.govpay.gde.client.beans.CategoriaEvento;
import it.govpay.gde.client.beans.ComponenteEvento;
import it.govpay.gde.client.beans.EsitoEvento;
import it.govpay.gde.client.beans.Header;
import it.govpay.gde.client.beans.RuoloEvento;

class GdeEventInfoTest {

    @Test
    @DisplayName("Builder con tutti i campi")
    void builder_allFields() {
        OffsetDateTime now = OffsetDateTime.now();
        Header header = new Header();
        header.setNome("Content-Type");
        header.setValore("application/json");

        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .componente(ComponenteEvento.GOVPAY)
                .categoriaEvento(CategoriaEvento.INTERFACCIA)
                .tipoEvento("invioNotifica")
                .ruolo(RuoloEvento.CLIENT)
                .sottotipoEvento("esito")
                .dataEvento(now)
                .esito(EsitoEvento.OK)
                .descrizioneEsito("HTTP 200")
                .idTransazione("txn-123")
                .idDominio("12345678901")
                .stazione("stazione01")
                .urlRichiesta("https://api.example.com/notifiche")
                .metodoHttp("POST")
                .payloadRichiesta("base64payload")
                .headersRichiesta(List.of(header))
                .statusCodeRisposta(200)
                .payloadRisposta("base64response")
                .headersRisposta(List.of(header))
                .build();

        assertEquals(ComponenteEvento.GOVPAY, eventInfo.getComponente());
        assertEquals(CategoriaEvento.INTERFACCIA, eventInfo.getCategoriaEvento());
        assertEquals("invioNotifica", eventInfo.getTipoEvento());
        assertEquals(RuoloEvento.CLIENT, eventInfo.getRuolo());
        assertEquals("esito", eventInfo.getSottotipoEvento());
        assertEquals(now, eventInfo.getDataEvento());
        assertEquals(EsitoEvento.OK, eventInfo.getEsito());
        assertEquals("HTTP 200", eventInfo.getDescrizioneEsito());
        assertEquals("txn-123", eventInfo.getIdTransazione());
        assertEquals("12345678901", eventInfo.getIdDominio());
        assertEquals("stazione01", eventInfo.getStazione());
        assertEquals("https://api.example.com/notifiche", eventInfo.getUrlRichiesta());
        assertEquals("POST", eventInfo.getMetodoHttp());
        assertEquals("base64payload", eventInfo.getPayloadRichiesta());
        assertEquals(1, eventInfo.getHeadersRichiesta().size());
        assertEquals(200, eventInfo.getStatusCodeRisposta());
        assertEquals("base64response", eventInfo.getPayloadRisposta());
        assertEquals(1, eventInfo.getHeadersRisposta().size());
    }

    @Test
    @DisplayName("isSuccess - OK restituisce true")
    void isSuccess_ok() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(EsitoEvento.OK).build();
        assertTrue(eventInfo.isSuccess());
    }

    @Test
    @DisplayName("isSuccess - KO restituisce false")
    void isSuccess_ko() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(EsitoEvento.KO).build();
        assertFalse(eventInfo.isSuccess());
    }

    @Test
    @DisplayName("isSuccess - FAIL restituisce false")
    void isSuccess_fail() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(EsitoEvento.FAIL).build();
        assertFalse(eventInfo.isSuccess());
    }

    @Test
    @DisplayName("isSuccess - null restituisce false")
    void isSuccess_null() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(null).build();
        assertFalse(eventInfo.isSuccess());
    }

    @Test
    @DisplayName("isError - KO restituisce true")
    void isError_ko() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(EsitoEvento.KO).build();
        assertTrue(eventInfo.isError());
    }

    @Test
    @DisplayName("isError - FAIL restituisce true")
    void isError_fail() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(EsitoEvento.FAIL).build();
        assertTrue(eventInfo.isError());
    }

    @Test
    @DisplayName("isError - OK restituisce false")
    void isError_ok() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(EsitoEvento.OK).build();
        assertFalse(eventInfo.isError());
    }

    @Test
    @DisplayName("isError - null restituisce false")
    void isError_null() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().esito(null).build();
        assertFalse(eventInfo.isError());
    }

    @Test
    @DisplayName("Campi transient con @Builder.Default sono null")
    void transientFields_defaults() {
        GdeEventInfo eventInfo = GdeEventInfo.builder().build();

        assertNull(eventInfo.getResponse());
        assertNull(eventInfo.getException());
        assertNull(eventInfo.getRequestObject());
    }

    @Test
    @DisplayName("Campi transient possono essere impostati")
    void transientFields_setValues() {
        ResponseEntity<String> response = ResponseEntity.ok("body");
        RestClientException exception = new RestClientException("errore");
        Object requestObj = "request";

        GdeEventInfo eventInfo = GdeEventInfo.builder()
                .response(response)
                .exception(exception)
                .requestObject(requestObj)
                .build();

        assertSame(response, eventInfo.getResponse());
        assertSame(exception, eventInfo.getException());
        assertSame(requestObj, eventInfo.getRequestObject());
    }
}
