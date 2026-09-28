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
package it.govpay.common.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import it.govpay.common.repository.IbanAccreditoRepository;
import it.govpay.common.repository.TipoTributoRepository;
import it.govpay.common.repository.TipoVersamentoDominioRepository;
import it.govpay.common.repository.TipoVersamentoRepository;
import it.govpay.common.repository.TributoRepository;
import it.govpay.common.repository.UnitaOperativaRepository;

@Slf4j
@Configuration
@ComponentScan(basePackages = {"it.govpay.common.client", "it.govpay.common.configurazione"})
@EnableJpaRepositories(basePackages = "it.govpay.common.repository",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                IbanAccreditoRepository.class,
                TipoTributoRepository.class,
                TipoVersamentoDominioRepository.class,
                TipoVersamentoRepository.class,
                TributoRepository.class,
                UnitaOperativaRepository.class }))
@EntityScan(basePackages = "it.govpay.common.entity")
public class GovPayClientAutoConfiguration {

    /*
     * I repository di anagrafica aggiunti con l'issue #9 (IBAN, tributi, tipi
     * versamento, unita' operative) sono esclusi dalla registrazione automatica.
     *
     * Questa configurazione e' importata da tutti i componenti che usano il
     * client comune, e registra i repository con il nome di default derivato
     * dall'interfaccia: un repository omonimo nel consumer non puo' piu' essere
     * registrato ("Invalid bean definition ... since there is already ... bound"),
     * e il consumer non ha modo di escludere quelli della libreria.
     *
     * Restano registrati quelli effettivamente usati dai consumer per costruire
     * RestTemplate e leggere configurazione e anagrafica di base. Un consumer che
     * voglia quelli esclusi li dichiara nel proprio @EnableJpaRepositories, come
     * gia' fanno console-api e portal-api per l'intero package.
     */

    public GovPayClientAutoConfiguration() {
        log.info("GovPay Client Commons inizializzato");
    }
}
