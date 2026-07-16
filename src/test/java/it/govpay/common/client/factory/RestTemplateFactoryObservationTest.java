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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import tools.jackson.databind.ObjectMapper;

import it.govpay.common.client.model.Connettore;
import it.govpay.common.client.oauth2.Oauth2ClientCredentialsManager;
import it.govpay.common.entity.TipoAutenticazione;

class RestTemplateFactoryObservationTest {

    @Test
    void observedTemplatePublishesHttpClientRequestsWithConnettoreTag() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig()
                .observationHandler(new DefaultMeterObservationHandler(meterRegistry));

        RestTemplateFactory factory = newFactory();
        factory.setObservationRegistry(provider(observationRegistry));

        RestTemplate restTemplate = factory.createRestTemplate(connettore("FDR_TEST"));

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://backend.test/flussi"))
                .andRespond(withSuccess());
        restTemplate.getForEntity("http://backend.test/flussi", String.class);
        server.verify();

        assertThat(meterRegistry.find("http.client.requests")
                .tag("connettore", "FDR_TEST")
                .timer()).isNotNull();
    }

    @Test
    void withoutObservationRegistryTemplateWorksUninstrumented() {
        RestTemplateFactory factory = newFactory();
        factory.setObservationRegistry(provider(null));

        RestTemplate restTemplate = factory.createRestTemplate(connettore("FDR_TEST"));

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://backend.test/flussi"))
                .andRespond(withSuccess());
        restTemplate.getForEntity("http://backend.test/flussi", String.class);
        server.verify();
    }

    private static RestTemplateFactory newFactory() {
        return new RestTemplateFactory(mock(Oauth2ClientCredentialsManager.class), new ObjectMapper());
    }

    private static Connettore connettore(String id) {
        return Connettore.builder()
                .idConnettore(id)
                .url("http://backend.test")
                .tipoAutenticazione(TipoAutenticazione.NONE)
                .connectionTimeout(5000)
                .readTimeout(30000)
                .build();
    }

    private static ObjectProvider<ObservationRegistry> provider(ObservationRegistry registry) {
        @SuppressWarnings("unchecked")
        ObjectProvider<ObservationRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(registry);
        return provider;
    }
}
