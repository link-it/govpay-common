package it.govpay.common.repository;

import it.govpay.common.client.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class DominioLogoRepositoryTest {

    @Autowired
    private DominioLogoRepository dominioLogoRepository;

    @Test
    void testFindLogoByCodDominio_presente() {
        Optional<byte[]> result = dominioLogoRepository.findLogoByCodDominio("11111111111");

        assertTrue(result.isPresent());
        byte[] logo = result.get();
        assertNotNull(logo);
        assertTrue(logo.length > 0);
        // Verifica header PNG (0x89504E47)
        assertEquals((byte) 0x89, logo[0]);
        assertEquals((byte) 0x50, logo[1]);
        assertEquals((byte) 0x4E, logo[2]);
        assertEquals((byte) 0x47, logo[3]);
    }

    @Test
    void testFindLogoByCodDominio_assente() {
        Optional<byte[]> result = dominioLogoRepository.findLogoByCodDominio("INESISTENTE");

        assertFalse(result.isPresent());
    }

    @Test
    void testFindLogoByCodDominio_senzaLogo() {
        Optional<byte[]> result = dominioLogoRepository.findLogoByCodDominio("01234567890");

        assertTrue(result.isEmpty() || result.get() == null);
    }
}
