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
package it.govpay.common.configurazione.service;

import it.govpay.common.client.TestApplication;
import it.govpay.common.configurazione.model.*;
import it.govpay.common.configurazione.model.GdeEvento.DumpEnum;
import it.govpay.common.configurazione.model.GdeEvento.LogEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class ConfigurazioneServiceTest {

    @Autowired
    private ConfigurazioneService configurazioneService;

    // --- getValore ---

    @Test
    void testGetValore_presente() {
        Optional<String> valore = configurazioneService.getValore("hardening");

        assertTrue(valore.isPresent());
        assertTrue(valore.get().contains("\"abilitato\":true"));
    }

    @Test
    void testGetValore_assente() {
        Optional<String> valore = configurazioneService.getValore("chiave.inesistente");

        assertFalse(valore.isPresent());
    }

    // --- getAsMap ---

    @Test
    void testGetAsMap() {
        Map<String, String> mappa = configurazioneService.getAsMap();

        assertNotNull(mappa);
        assertEquals(7, mappa.size());
        assertNotNull(mappa.get("giornale_eventi"));
        assertNotNull(mappa.get("mail_batch"));
    }

    // --- getAsObject ---

    @Test
    void testGetAsObject_assente() {
        Optional<Hardening> result = configurazioneService.getAsObject("chiave.inesistente", Hardening.class);

        assertFalse(result.isPresent());
    }

    @Test
    void testGetAsObject_jsonInvalido() {
        // Un JSON object non e' deserializzabile come Integer
        assertThrows(IllegalArgumentException.class, () ->
            configurazioneService.getAsObject("giornale_eventi", Integer.class)
        );
    }

    // --- Typed getters ---

    @Test
    void testGetGiornale() {
        Optional<Giornale> result = configurazioneService.getGiornale();

        assertTrue(result.isPresent());
        Giornale giornale = result.get();

        assertNotNull(giornale.getApiEnte());
        assertEquals(LogEnum.SEMPRE, giornale.getApiEnte().getLetture().getLog());
        assertEquals(DumpEnum.SEMPRE, giornale.getApiEnte().getLetture().getDump());
        assertEquals(DumpEnum.SOLO_ERRORE, giornale.getApiEnte().getScritture().getDump());

        assertNotNull(giornale.getApiPagamento());
        assertEquals(LogEnum.MAI, giornale.getApiPagamento().getLetture().getLog());

        // Interfacce non presenti nel JSON devono essere null
        assertNull(giornale.getApiRagioneria());
    }

    @Test
    void testGetTracciatoCsv() {
        Optional<TracciatoCsv> result = configurazioneService.getTracciatoCsv();

        assertTrue(result.isPresent());
        TracciatoCsv csv = result.get();
        assertEquals("freemarker", csv.getTipo());
        assertNotNull(csv.getIntestazione());
        assertNotNull(csv.getRichiesta());
        assertNotNull(csv.getRisposta());
    }

    @Test
    void testGetHardening() {
        Optional<Hardening> result = configurazioneService.getHardening();

        assertTrue(result.isPresent());
        Hardening h = result.get();
        assertTrue(h.isAbilitato());

        GoogleCaptcha captcha = h.getGoogleCatpcha();
        assertNotNull(captcha);
        assertEquals("https://www.google.com/recaptcha/api/siteverify", captcha.getServerURL());
        assertEquals("test-site-key", captcha.getSiteKey());
        assertEquals(0.7, captcha.getSoglia(), 0.001);
        assertTrue(captcha.isDenyOnFail());
        assertEquals(5000, captcha.getConnectionTimeout());
    }

    @Test
    void testGetMailBatch() {
        Optional<MailBatch> result = configurazioneService.getMailBatch();

        assertTrue(result.isPresent());
        MailBatch mb = result.get();
        assertTrue(mb.isAbilitato());

        MailServer ms = mb.getMailserver();
        assertNotNull(ms);
        assertEquals("smtp.test.com", ms.getHost());
        assertEquals(587, ms.getPort());
        assertEquals("test@test.com", ms.getUsername());
        assertEquals("secret", ms.getPassword());
        assertEquals("noreply@test.com", ms.getFrom());
        assertEquals(10000, ms.getReadTimeout());
        assertTrue(ms.isStartTls());
    }

    @Test
    void testGetAppIOBatch() {
        Optional<AppIOBatch> result = configurazioneService.getAppIOBatch();

        assertTrue(result.isPresent());
        AppIOBatch aio = result.get();
        assertFalse(aio.isAbilitato());
        assertEquals(new BigDecimal("3600"), aio.getTimeToLive());
        assertEquals("https://api.io.italia.it", aio.getUrl());
    }

    @Test
    void testGetAvvisaturaViaMail() {
        Optional<AvvisaturaViaMail> result = configurazioneService.getAvvisaturaViaMail();

        assertTrue(result.isPresent());
        AvvisaturaViaMail avm = result.get();

        PromemoriaAvviso avviso = avm.getPromemoriaAvviso();
        assertNotNull(avviso);
        assertEquals("freemarker", avviso.getTipo());
        assertEquals("Avviso di pagamento", avviso.getOggetto());
        assertTrue(avviso.isAllegaPdf());

        PromemoriaRicevuta ricevuta = avm.getPromemoriaRicevuta();
        assertNotNull(ricevuta);
        assertTrue(ricevuta.isSoloEseguiti());
        assertFalse(ricevuta.isAllegaPdf());

        PromemoriaScadenza scadenza = avm.getPromemoriaScadenza();
        assertNotNull(scadenza);
        assertEquals(7, scadenza.getPreavviso());
    }

    @Test
    void testGetAvvisaturaViaAppIo() {
        Optional<AvvisaturaViaAppIo> result = configurazioneService.getAvvisaturaViaAppIo();

        assertTrue(result.isPresent());
        AvvisaturaViaAppIo avio = result.get();

        PromemoriaAvvisoBase avviso = avio.getPromemoriaAvviso();
        assertNotNull(avviso);
        assertEquals("Avviso IO", avviso.getOggetto());

        PromemoriaRicevutaBase ricevuta = avio.getPromemoriaRicevuta();
        assertNotNull(ricevuta);
        assertFalse(ricevuta.isSoloEseguiti());

        PromemoriaScadenza scadenza = avio.getPromemoriaScadenza();
        assertNotNull(scadenza);
        assertEquals(3, scadenza.getPreavviso());
    }
}
