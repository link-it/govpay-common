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

import it.govpay.common.client.TestApplication;
import it.govpay.common.entity.IntermediarioEntity;
import it.govpay.common.entity.StazioneEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
@Transactional
class StazioneRepositoryTest {

    @Autowired
    private StazioneRepository stazioneRepository;

    @Autowired
    private IntermediarioRepository intermediarioRepository;

    @Test
    void testFindByCodStazione_presente() {
        Optional<StazioneEntity> result = stazioneRepository.findByCodStazione("12345678901_01");

        assertTrue(result.isPresent());
        StazioneEntity stazione = result.get();
        assertEquals("12345678901_01", stazione.getCodStazione());
        assertEquals("password01", stazione.getPassword());
        assertTrue(stazione.getAbilitato());
        assertEquals(1, stazione.getApplicationCode());
        assertEquals("2", stazione.getVersione());
        assertNotNull(stazione.getIntermediario());
        assertEquals("12345678901", stazione.getIntermediario().getCodIntermediario());
    }

    @Test
    void testFindByCodStazione_assente() {
        Optional<StazioneEntity> result = stazioneRepository.findByCodStazione("INESISTENTE_01");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByIntermediario() {
        IntermediarioEntity intermediario = intermediarioRepository.findByCodIntermediario("12345678901").orElseThrow();

        List<StazioneEntity> stazioni = stazioneRepository.findByIntermediario(intermediario);

        assertEquals(2, stazioni.size());
        assertTrue(stazioni.stream().anyMatch(s -> "12345678901_01".equals(s.getCodStazione())));
        assertTrue(stazioni.stream().anyMatch(s -> "12345678901_02".equals(s.getCodStazione())));
    }

    @Test
    void testFindByIntermediario_disabilitato() {
        IntermediarioEntity intermediario = intermediarioRepository.findByCodIntermediario("99999999999").orElseThrow();

        List<StazioneEntity> stazioni = stazioneRepository.findByIntermediario(intermediario);

        assertEquals(1, stazioni.size());
        assertEquals("99999999999_01", stazioni.get(0).getCodStazione());
        assertFalse(stazioni.get(0).getAbilitato());
    }

    @Test
    void testFindByCodIntermediarioAndCodStazione_presente() {
        Optional<StazioneEntity> result = stazioneRepository
                .findByIntermediarioCodIntermediarioAndCodStazione("12345678901", "12345678901_01");

        assertTrue(result.isPresent());
        StazioneEntity stazione = result.get();
        assertEquals("12345678901_01", stazione.getCodStazione());
        assertEquals("12345678901", stazione.getIntermediario().getCodIntermediario());
    }

    @Test
    void testFindByCodIntermediarioAndCodStazione_intermediarioErrato() {
        Optional<StazioneEntity> result = stazioneRepository
                .findByIntermediarioCodIntermediarioAndCodStazione("99999999999", "12345678901_01");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByCodIntermediarioAndCodStazione_stazioneErrata() {
        Optional<StazioneEntity> result = stazioneRepository
                .findByIntermediarioCodIntermediarioAndCodStazione("12345678901", "INESISTENTE");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll() {
        List<StazioneEntity> result = stazioneRepository.findAll();

        assertEquals(3, result.size());
    }
}
