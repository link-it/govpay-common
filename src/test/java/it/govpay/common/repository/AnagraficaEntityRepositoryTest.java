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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import it.govpay.common.entity.IbanAccreditoEntity;
import it.govpay.common.entity.TipoContabilita;
import it.govpay.common.entity.TipoTributoEntity;
import it.govpay.common.entity.TipoVersamentoDominioEntity;
import it.govpay.common.entity.TipoVersamentoEntity;
import it.govpay.common.entity.TributoEntity;
import it.govpay.common.entity.UnitaOperativaEntity;

/**
 * Verifica le 6 entità/repository di anagrafica aggiunte da
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>, con
 * {@code spring.jpa.hibernate.ddl-auto=validate} contro lo schema reale copiato in
 * {@code anagrafica-schema.sql} — non contro uno schema generato da Hibernate (con
 * {@code create-drop}, come nel resto del modulo, un nome di colonna sbagliato passerebbe
 * inosservato): un errore di mappatura fa fallire qui il bootstrap del contesto, non solo
 * un'asserzione. Contesto isolato da {@code TestApplication} — vedi Javadoc di
 * {@link AnagraficaTestApplication} per il perché.
 */
@SpringBootTest(classes = AnagraficaTestApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:anagraficatestdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.defer-datasource-initialization=false",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:anagrafica-schema.sql",
        "spring.sql.init.data-locations=classpath:anagrafica-data.sql",
        "spring.h2.console.enabled=false"
})
@Transactional
class AnagraficaEntityRepositoryTest {

    @Autowired
    private UnitaOperativaRepository unitaOperativaRepository;

    @Autowired
    private IbanAccreditoRepository ibanAccreditoRepository;

    @Autowired
    private TipoTributoRepository tipoTributoRepository;

    @Autowired
    private TributoRepository tributoRepository;

    @Autowired
    private TipoVersamentoRepository tipoVersamentoRepository;

    @Autowired
    private TipoVersamentoDominioRepository tipoVersamentoDominioRepository;

    @Test
    void findByCodUoAndDominioId_presente() {
        Optional<UnitaOperativaEntity> result = unitaOperativaRepository.findByCodUoAndDominioId("UO-TEST", 1L);

        assertTrue(result.isPresent());
        assertEquals("Ufficio Tributi", result.get().getUoDenominazione());
        assertEquals(1L, result.get().getDominio().getId());
    }

    @Test
    void findByCodUoAndDominioId_assente() {
        assertFalse(unitaOperativaRepository.findByCodUoAndDominioId("UO-TEST", 999L).isPresent());
    }

    @Test
    void findByCodIbanAndDominioId_presente() {
        Optional<IbanAccreditoEntity> result = ibanAccreditoRepository
                .findByCodIbanAndDominioId("IT60X0542811101000000123456", 1L);

        assertTrue(result.isPresent());
        assertEquals("Conto principale", result.get().getDescrizione());
    }

    @Test
    void findByCodTributo_presente() {
        Optional<TipoTributoEntity> result = tipoTributoRepository.findByCodTributo("TARI");

        assertTrue(result.isPresent());
        assertEquals(TipoContabilita.SIOPE, result.get().getTipoContabilita());
    }

    @Test
    void findByCodTributo_assente() {
        assertFalse(tipoTributoRepository.findByCodTributo("INESISTENTE").isPresent());
    }

    @Test
    void findByDominioIdAndTipoTributoId_presente() {
        Optional<TributoEntity> result = tributoRepository.findByDominioIdAndTipoTributoId(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(TipoContabilita.SIOPE, result.get().getTipoContabilita());
        assertEquals("IT60X0542811101000000123456", result.get().getIbanAccredito().getCodIban());
    }

    @Test
    void findByCodTipoVersamento_presente() {
        Optional<TipoVersamentoEntity> result = tipoVersamentoRepository.findByCodTipoVersamento("IMU");

        assertTrue(result.isPresent());
        assertEquals("Imposta Municipale Unica", result.get().getDescrizione());
    }

    /**
     * Bug del lead, 2026-09-28: {@code equals} confrontava {@code that.id} per accesso diretto
     * al campo. Su un proxy Hibernate non inizializzato quel campo resta {@code null} anche se
     * l'identificativo è noto (i proxy lo conoscono fin dalla creazione, ma solo tramite
     * {@code getId()} — il campo della sottoclasse generata resta vuoto finché il proxy non
     * viene inizializzato): {@code proxy.equals(reale)} tornava {@code true} ma
     * {@code reale.equals(proxy)} tornava {@code false}, violando la simmetria richiesta dal
     * contratto di {@code equals}. Riprodotto qui senza toccare Hibernate: una sottoclasse che
     * ridefinisce solo {@code getId()} riproduce esattamente lo scarto fra getter e campo che
     * un proxy reale presenta. Stesso pattern per tutte le 6 entità di questa issue, verificato
     * solo su {@code TipoVersamentoEntity}, la stessa su cui è stato segnalato.
     */
    @Test
    @DisplayName("equals confronta getId(), non il campo id per accesso diretto: un proxy Hibernate "
            + "(getId() noto, campo id ancora null) risulta uguale a un'istanza reale con lo stesso id "
            + "in entrambe le direzioni")
    void equalsUsaIlGetterNonIlCampoDiretto_simmetricoConUnProxy() {
        TipoVersamentoEntity reale = new TipoVersamentoEntity();
        reale.setId(1L);
        reale.setCodTipoVersamento("IMU");

        TipoVersamentoEntity proxySimulato = new TipoVersamentoEntity() {
            @Override
            public Long getId() {
                return 1L;
            }
        };

        assertEquals(reale, proxySimulato);
        assertEquals(proxySimulato, reale);
    }

    @Test
    void findByDominioIdAndTipoVersamentoId_presente() {
        Optional<TipoVersamentoDominioEntity> result = tipoVersamentoDominioRepository
                .findByDominioIdAndTipoVersamentoId(1L, 1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getTipoVersamento().getId());
    }

    @Test
    void findByDominioIdAndTipoVersamentoId_assente() {
        assertFalse(tipoVersamentoDominioRepository.findByDominioIdAndTipoVersamentoId(1L, 999L).isPresent());
    }
}
