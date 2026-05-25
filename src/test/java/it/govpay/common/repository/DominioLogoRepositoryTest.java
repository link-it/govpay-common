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
