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
package it.govpay.common.batch.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClusterIdPredefinitoTest {

    @Test
    @DisplayName("HOSTNAME dell'ambiente ha la precedenza (in Kubernetes e' il nome del pod)")
    void daVariabileHostname() {
        assertEquals("govpay-rt-batch-7c9f",
                ClusterIdPredefinito.risolvi(Map.of("HOSTNAME", " govpay-rt-batch-7c9f ")::get, () -> "host-vm"));
    }

    @Test
    @DisplayName("Senza HOSTNAME si usa il nome dell'host")
    void daNomeHost() {
        assertEquals("host-vm", ClusterIdPredefinito.risolvi(Map.of("HOSTNAME", " ")::get, () -> "host-vm"));
        assertEquals("host-vm", ClusterIdPredefinito.risolvi(variabile -> null, () -> "host-vm"));
    }

    @Test
    @DisplayName("Senza HOSTNAME ne' nome dell'host si ricade sulla costante")
    void fallback() {
        assertEquals(ClusterIdPredefinito.FALLBACK, ClusterIdPredefinito.risolvi(variabile -> null, () -> null));
        assertEquals(ClusterIdPredefinito.FALLBACK, ClusterIdPredefinito.risolvi(variabile -> null, () -> {
            throw new IllegalStateException("host sconosciuto");
        }));
    }

    @Test
    @DisplayName("Il valore di questo processo e' valorizzato e stabile")
    void valoreDelProcesso() {
        String valore = ClusterIdPredefinito.valore();

        assertFalse(valore.isBlank());
        assertSame(valore, ClusterIdPredefinito.valore());
    }
}
