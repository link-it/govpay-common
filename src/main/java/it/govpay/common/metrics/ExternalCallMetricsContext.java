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
package it.govpay.common.metrics;

/**
 * Accumulatore request-scoped dei tempi spesi in chiamate verso servizi
 * esterni. Registrato come bean {@code @RequestScope} da
 * {@link GovpayMetricsAutoConfiguration}.
 */
public class ExternalCallMetricsContext {

    private long externalNanos;

    /**
     * Somma il tempo speso in una chiamata esterna a quello gia' accumulato per la request.
     * <p>
     * Il metodo si chiamava {@code record}, identificatore riservato del linguaggio
     * (SonarCloud java:S6213). La classe e' usata solo all'interno di govpay-common.
     *
     * @param elapsedNanos nanosecondi da sommare
     */
    public void addExternalNanos(long elapsedNanos) {
        externalNanos += elapsedNanos;
    }

    public long externalNanos() {
        return externalNanos;
    }

}
