package it.govpay.common.repository;

import it.govpay.common.client.TestApplication;
import it.govpay.common.entity.IntermediarioEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class IntermediarioRepositoryTest {

    @Autowired
    private IntermediarioRepository intermediarioRepository;

    @Test
    void testFindByCodIntermediario_presente() {
        Optional<IntermediarioEntity> result = intermediarioRepository.findByCodIntermediario("12345678901");

        assertTrue(result.isPresent());
        IntermediarioEntity intermediario = result.get();
        assertEquals("12345678901", intermediario.getCodIntermediario());
        assertEquals("Intermediario di Test", intermediario.getDenominazione());
        assertEquals("PRINCIPAL_TEST", intermediario.getPrincipal());
        assertEquals("PRINCIPAL_TEST_ORIG", intermediario.getPrincipalOriginale());
        assertTrue(intermediario.getAbilitato());
        assertEquals("TEST_BASIC", intermediario.getCodConnettorePdd());
        assertEquals("TEST_APIKEY", intermediario.getCodConnettoreRecuperoRt());
        assertEquals("TEST_NONE", intermediario.getCodConnettoreAca());
        assertEquals("TEST_OAUTH2", intermediario.getCodConnettoreGpd());
        assertEquals("TEST_AZURE", intermediario.getCodConnettoreFr());
        assertEquals("TEST_HTTP_HEADER", intermediario.getCodConnettoreBackofficeEc());
        assertEquals("TEST_CUSTOM_HEADERS", intermediario.getCodConnettoreFtp());
    }

    @Test
    void testFindByCodIntermediario_assente() {
        Optional<IntermediarioEntity> result = intermediarioRepository.findByCodIntermediario("00000000000");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByCodIntermediario_disabilitato() {
        Optional<IntermediarioEntity> result = intermediarioRepository.findByCodIntermediario("99999999999");

        assertTrue(result.isPresent());
        IntermediarioEntity intermediario = result.get();
        assertEquals("Intermediario Disabilitato", intermediario.getDenominazione());
        assertFalse(intermediario.getAbilitato());
        assertNull(intermediario.getCodConnettorePdd());
    }

    @Test
    void testFindByCodDominio_presente() {
        Optional<IntermediarioEntity> result = intermediarioRepository.findByCodDominio("01234567890");

        assertTrue(result.isPresent());
        assertEquals("12345678901", result.get().getCodIntermediario());
    }

    @Test
    void testFindByCodDominio_assente() {
        Optional<IntermediarioEntity> result = intermediarioRepository.findByCodDominio("INESISTENTE");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindByCodDominio_senzaStazione() {
        Optional<IntermediarioEntity> result = intermediarioRepository.findByCodDominio("00000000000");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll() {
        List<IntermediarioEntity> result = intermediarioRepository.findAll();

        assertEquals(2, result.size());
    }
}
