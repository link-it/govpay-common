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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Valore predefinito del {@code cluster-id} di un batch.
 * <p>
 * Il {@code cluster-id} identifica in modo univoco il <b>nodo</b> che esegue il
 * batch: nelle installazioni multinodo, che sono la norma, deve essere diverso su
 * ogni nodo, altrimenti un job in esecuzione su un altro nodo viene classificato
 * come in esecuzione su quello corrente (#36). Per questo il default non e' una
 * costante ma l'identificativo dell'host:
 * <ol>
 *   <li>la variabile d'ambiente {@code HOSTNAME} (in Kubernetes e' il nome del pod);</li>
 *   <li>il nome dell'host restituito da {@link InetAddress#getLocalHost()};</li>
 *   <li>{@value #FALLBACK}, solo se nessuno dei due e' disponibile.</li>
 * </ol>
 * Il valore e' calcolato una volta sola.
 */
@Slf4j
public final class ClusterIdPredefinito {

    /** Ultima scelta, uguale su tutti i nodi: va evitata nelle installazioni multinodo */
    public static final String FALLBACK = "GovPay-Batch";

    private static final class Valore {
        private static final String CLUSTER_ID = risolvi(System::getenv, ClusterIdPredefinito::hostnameLocale);
    }

    private ClusterIdPredefinito() {
        // Utility class
    }

    /**
     * @return il {@code cluster-id} predefinito di questo nodo
     */
    public static String valore() {
        return Valore.CLUSTER_ID;
    }

    /**
     * Risolve il {@code cluster-id} predefinito dalle sorgenti indicate.
     *
     * @param ambiente Lettura delle variabili d'ambiente
     * @param hostname Nome dell'host; puo' restituire null o lanciare eccezione
     * @return il primo valore disponibile, o {@value #FALLBACK}
     */
    static String risolvi(Function<String, String> ambiente, Supplier<String> hostname) {
        String daAmbiente = ambiente.apply("HOSTNAME");
        if (StringUtils.hasText(daAmbiente)) {
            return daAmbiente.trim();
        }
        try {
            String host = hostname.get();
            if (StringUtils.hasText(host)) {
                return host.trim();
            }
        } catch (RuntimeException e) {
            log.warn("Nome dell'host non disponibile: {}", e.getMessage());
        }
        log.warn("Impossibile ricavare un cluster-id specifico del nodo: si usa '{}', uguale su tutti i nodi", FALLBACK);
        return FALLBACK;
    }

    private static String hostnameLocale() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
