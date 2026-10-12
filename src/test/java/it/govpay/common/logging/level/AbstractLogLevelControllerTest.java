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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import it.govpay.common.batch.dto.Problem;

/**
 * Verifica il contratto REST della gestione dei livelli di log (BP-LOG-1):
 * cosa viene delegato al servizio, quali codici di stato escono e che gli
 * errori siano resi in {@code application/problem+json} (BP-API-1).
 */
@ExtendWith(MockitoExtension.class)
class AbstractLogLevelControllerTest {

    private static final String LOGGER = "it.govpay.common.logging";

    @Mock
    private DynamicLogLevelService service;

    /** Sottoclasse minima: la classe base non e' annotata, i mapping stanno nel consumer. */
    private static class ControllerDiProva extends AbstractLogLevelController {
        ControllerDiProva(DynamicLogLevelService service) {
            super(service);
        }
    }

    private AbstractLogLevelController controller() {
        return new ControllerDiProva(service);
    }

    @Test
    @DisplayName("getLoggers restituisce l'elenco dei logger gestibili")
    void elencoLoggerGestibili() {
        List<LivelloLoggerInfo> atteso = List.of(LivelloLoggerInfo.builder()
                .logger(LOGGER).livelloEffettivo("INFO").build());
        when(service.statoLogger()).thenReturn(atteso);

        ResponseEntity<List<LivelloLoggerInfo>> risposta = controller().getLoggers();

        assertEquals(HttpStatus.OK, risposta.getStatusCode());
        assertSame(atteso, risposta.getBody());
    }

    @Test
    @DisplayName("getLivelliDinamici restituisce i soli livelli imposti")
    void livelliDinamici() {
        when(service.livelliCorrenti()).thenReturn(Map.of(LOGGER, "DEBUG"));

        ResponseEntity<Map<String, String>> risposta = controller().getLivelliDinamici();

        assertEquals(HttpStatus.OK, risposta.getStatusCode());
        assertEquals(Map.of(LOGGER, "DEBUG"), risposta.getBody());
    }

    @Test
    @DisplayName("setLivello delega al servizio e risponde 204")
    void impostaLivello() {
        ResponseEntity<Object> risposta =
                controller().setLivello(LOGGER, new LivelloLogRequest("DEBUG"));

        verify(service).setLivello(LOGGER, "DEBUG");
        assertEquals(HttpStatus.NO_CONTENT, risposta.getStatusCode());
        assertNull(risposta.getBody());
    }

    @Test
    @DisplayName("setLivello con corpo assente non chiama il servizio con un livello valido")
    void impostaLivelloSenzaCorpo() {
        doThrow(new IllegalArgumentException("Livello di log non specificato"))
                .when(service).setLivello(anyString(), isNull());

        ResponseEntity<Object> risposta = controller().setLivello(LOGGER, null);

        assertEquals(HttpStatus.BAD_REQUEST, risposta.getStatusCode());
    }

    @Test
    @DisplayName("Un argomento non valido diventa un problem+json 400")
    void argomentoNonValidoDiventaProblem() {
        doThrow(new IllegalArgumentException("Il logger 'org.hibernate' non e' gestibile dinamicamente"))
                .when(service).setLivello(anyString(), anyString());

        ResponseEntity<Object> risposta =
                controller().setLivello("org.hibernate", new LivelloLogRequest("DEBUG"));

        assertEquals(HttpStatus.BAD_REQUEST, risposta.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, risposta.getHeaders().getContentType());
        assertTrue(risposta.getBody() instanceof Problem);
        Problem problem = (Problem) risposta.getBody();
        assertEquals(400, problem.getStatus());
        assertTrue(problem.getDetail().contains("org.hibernate"));
    }

    @Test
    @DisplayName("rimuoviLivello delega al servizio e risponde 204")
    void rimuoveLivello() {
        ResponseEntity<Object> risposta = controller().rimuoviLivello(LOGGER);

        verify(service).rimuoviLivello(LOGGER);
        assertEquals(HttpStatus.NO_CONTENT, risposta.getStatusCode());
    }

    @Test
    @DisplayName("rimuoviLivello fuori ambito risponde 400 senza ripristinare nulla")
    void rimuoveLivelloFuoriAmbito() {
        doThrow(new IllegalArgumentException("fuori ambito"))
                .when(service).rimuoviLivello(anyString());

        ResponseEntity<Object> risposta = controller().rimuoviLivello("org.hibernate");

        assertEquals(HttpStatus.BAD_REQUEST, risposta.getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, risposta.getHeaders().getContentType());
    }

    @Test
    @DisplayName("refreshLivelli restituisce i livelli applicati dopo la rilettura")
    void refresh() {
        when(service.refresh()).thenReturn(Map.of(LOGGER, "TRACE"));

        ResponseEntity<Map<String, String>> risposta = controller().refreshLivelli();

        assertEquals(HttpStatus.OK, risposta.getStatusCode());
        assertEquals(Map.of(LOGGER, "TRACE"), risposta.getBody());
    }

    @Test
    @DisplayName("resetLivelli ripristina tutto e risponde 204")
    void reset() {
        ResponseEntity<Object> risposta = controller().resetLivelli();

        verify(service).reset();
        verify(service, never()).refresh();
        assertEquals(HttpStatus.NO_CONTENT, risposta.getStatusCode());
    }

    private static String isNull() {
        return org.mockito.ArgumentMatchers.isNull();
    }
}
