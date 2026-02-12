package it.govpay.common.repository;

import it.govpay.common.client.TestApplication;
import it.govpay.common.entity.DominioEntity;
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
class DominioRepositoryTest {

    @Autowired
    private DominioRepository dominioRepository;

    @Autowired
    private StazioneRepository stazioneRepository;

    @Test
    void testFindByCodDominio_presente() {
        Optional<DominioEntity> result = dominioRepository.findByCodDominio("01234567890");

        assertTrue(result.isPresent());
        DominioEntity dominio = result.get();
        assertEquals("01234567890", dominio.getCodDominio());
        assertEquals("Comune di Test", dominio.getRagioneSociale());
        assertTrue(dominio.getAbilitato());
        assertEquals(3, dominio.getAuxDigit());
        assertEquals("TST", dominio.getIuvPrefix());
        assertEquals(1, dominio.getSegregationCode());
        assertEquals("ABCDE", dominio.getCbill());
        assertTrue(dominio.getIntermediato());
        assertEquals("PagoPa_01", dominio.getTassonomiaPagoPa());
        assertTrue(dominio.getScaricaFr());
        assertNotNull(dominio.getStazione());
        assertEquals("12345678901_01", dominio.getStazione().getCodStazione());
    }

    @Test
    void testFindByCodDominio_assente() {
        Optional<DominioEntity> result = dominioRepository.findByCodDominio("INESISTENTE");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByCodDominio_disabilitato() {
        Optional<DominioEntity> result = dominioRepository.findByCodDominio("00000000000");

        assertTrue(result.isPresent());
        DominioEntity dominio = result.get();
        assertFalse(dominio.getAbilitato());
        assertEquals("Ente Disabilitato", dominio.getRagioneSociale());
        assertNull(dominio.getStazione());
    }

    @Test
    void testFindByCodDominio_senzaConnettori() {
        Optional<DominioEntity> result = dominioRepository.findByCodDominio("09876543210");

        assertTrue(result.isPresent());
        DominioEntity dominio = result.get();
        assertEquals("Provincia di Test", dominio.getRagioneSociale());
        assertFalse(dominio.getIntermediato());
        assertFalse(dominio.getScaricaFr());
        assertNull(dominio.getCbill());
        assertNotNull(dominio.getStazione());
        assertEquals("12345678901_02", dominio.getStazione().getCodStazione());
    }

    @Test
    void testFindByStazione() {
        StazioneEntity stazione = stazioneRepository.findByCodStazione("12345678901_01").orElseThrow();

        List<DominioEntity> domini = dominioRepository.findByStazione(stazione);

        assertEquals(2, domini.size());
    }

    @Test
    void testFindAll() {
        List<DominioEntity> result = dominioRepository.findAll();

        assertEquals(4, result.size());
    }
}
