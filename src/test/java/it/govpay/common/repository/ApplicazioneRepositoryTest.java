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
package it.govpay.common.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import it.govpay.common.client.TestApplication;
import it.govpay.common.entity.ApplicazioneEntity;
import it.govpay.common.entity.ConnettoreEntity;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class ApplicazioneRepositoryTest {

    @Autowired
    private ApplicazioneRepository applicazioneRepository;

    // ==================== findByCodApplicazione ====================

    @Test
    void findByCodApplicazione_presente() {
        Optional<ApplicazioneEntity> result =
                applicazioneRepository.findByCodApplicazione("APP_CON_CONNETTORE");

        assertTrue(result.isPresent());
        ApplicazioneEntity app = result.get();
        assertEquals("APP_CON_CONNETTORE", app.getCodApplicazione());
        assertTrue(app.getAutoIuv());
        assertTrue(app.getTrusted());
        assertEquals("N", app.getFirmaRicevuta());
        assertEquals("TEST_BASIC", app.getCodConnettoreIntegrazione());
        assertEquals("ACN", app.getCodApplicazioneIuv());
    }

    @Test
    void findByCodApplicazione_senzaConnettore() {
        Optional<ApplicazioneEntity> result =
                applicazioneRepository.findByCodApplicazione("APP_SENZA_CONNETTORE");

        assertTrue(result.isPresent());
        ApplicazioneEntity app = result.get();
        assertFalse(app.getAutoIuv());
        assertFalse(app.getTrusted());
        assertNull(app.getCodConnettoreIntegrazione());
        assertNull(app.getCodApplicazioneIuv());
    }

    @Test
    void findByCodApplicazione_assente() {
        Optional<ApplicazioneEntity> result =
                applicazioneRepository.findByCodApplicazione("INESISTENTE");

        assertFalse(result.isPresent());
    }

    @Test
    void findAll_restituisceEntrambe() {
        List<ApplicazioneEntity> result = applicazioneRepository.findAll();

        assertEquals(2, result.size());
    }

    // ==================== findConnettoreIntegrazione ====================

    @Test
    void findConnettoreIntegrazione_presente() {
        List<ConnettoreEntity> rows =
                applicazioneRepository.findConnettoreIntegrazione("APP_CON_CONNETTORE");

        assertFalse(rows.isEmpty());
        assertTrue(rows.stream().allMatch(c -> "TEST_BASIC".equals(c.getCodConnettore())));
        assertTrue(rows.stream().anyMatch(c ->
                "URL".equals(c.getCodProprieta()) &&
                "https://api.test-basic.com".equals(c.getValore())));
        assertTrue(rows.stream().anyMatch(c ->
                "TIPOAUTENTICAZIONE".equals(c.getCodProprieta()) &&
                "HTTPBasic".equals(c.getValore())));
    }

    @Test
    void findConnettoreIntegrazione_senzaConnettore() {
        List<ConnettoreEntity> rows =
                applicazioneRepository.findConnettoreIntegrazione("APP_SENZA_CONNETTORE");

        assertTrue(rows.isEmpty());
    }

    @Test
    void findConnettoreIntegrazione_applicazioneAssente() {
        List<ConnettoreEntity> rows =
                applicazioneRepository.findConnettoreIntegrazione("INESISTENTE");

        assertTrue(rows.isEmpty());
    }
}
