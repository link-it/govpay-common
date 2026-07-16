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
package it.govpay.common.client.factory;

import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

/**
 * Convention per le observation dei RestTemplate creati dalla factory:
 * aggiunge a {@code http.client.requests} il tag a bassa cardinalita'
 * {@code connettore} con l'identificativo del connettore GovPay. E' il tag
 * discriminante tra i client: il client OpenAPI generato passa URI gia'
 * espanse, quindi il tag {@code uri} standard degrada spesso a "none".
 */
public class ConnettoreClientObservationConvention extends DefaultClientRequestObservationConvention {

    private static final String UNKNOWN = "unknown";

    private final KeyValue connettoreTag;

    public ConnettoreClientObservationConvention(String idConnettore) {
        this.connettoreTag = KeyValue.of("connettore", idConnettore != null ? idConnettore : UNKNOWN);
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
        return super.getLowCardinalityKeyValues(context).and(connettoreTag);
    }
}
