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

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

import it.govpay.common.entity.DominioEntity;
import it.govpay.common.entity.IbanAccreditoEntity;
import it.govpay.common.entity.IntermediarioEntity;
import it.govpay.common.entity.StazioneEntity;
import it.govpay.common.entity.TipoTributoEntity;
import it.govpay.common.entity.TipoVersamentoDominioEntity;
import it.govpay.common.entity.TipoVersamentoEntity;
import it.govpay.common.entity.TributoEntity;
import it.govpay.common.entity.UnitaOperativaEntity;

/**
 * Contesto Spring dedicato a {@link AnagraficaEntityRepositoryTest}, isolato da
 * {@code it.govpay.common.client.TestApplication} usato dagli altri test di questo modulo:
 * quello, tramite {@code GovPayClientAutoConfiguration} (component-scan sotto
 * {@code it.govpay.common.client}), mette TUTTE le entita' di {@code it.govpay.common.entity}
 * (batch incluso) nella stessa persistence unit — {@code batch_job_instance}/
 * {@code batch_job_execution} non hanno pero' una tabella reale in {@code gov_pay.sql} (sono
 * gestite a parte da Spring Batch), quindi un {@code ddl-auto=validate} su quel contesto
 * fallirebbe per un motivo estraneo alla issue
 * <a href="https://github.com/link-it/govpay-common/issues/9">govpay-common#9</a>.
 *
 * <p>{@link PersistenceManagedTypes} (non {@code @EntityScan}, che lavora per pacchetto e non
 * permette di escludere singole classi dello stesso pacchetto) elenca esplicitamente solo le
 * 6 entita' di questa issue piu' {@link DominioEntity} e la sua catena di relazioni
 * {@code @ManyToOne} ({@link StazioneEntity}/{@link IntermediarioEntity} — Hibernate richiede
 * che l'intero grafo raggiungibile stia nella stessa persistence unit, non solo l'entita' di
 * partenza): stesso pattern gia' usato in
 * {@code GovPayPendenzeApiApplication#persistenceManagedTypes}.</p>
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "it.govpay.common.repository",
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = { ApplicazioneRepository.class, ConfigurazioneRepository.class,
                        ConnettoreEntityRepository.class, DominioLogoRepository.class,
                        IntermediarioRepository.class, StazioneRepository.class }))
public class AnagraficaTestApplication {

    @Bean
    public PersistenceManagedTypes persistenceManagedTypes() {
        return PersistenceManagedTypes.of(
                List.of(
                        DominioEntity.class.getName(),
                        StazioneEntity.class.getName(),
                        IntermediarioEntity.class.getName(),
                        UnitaOperativaEntity.class.getName(),
                        IbanAccreditoEntity.class.getName(),
                        TipoTributoEntity.class.getName(),
                        TributoEntity.class.getName(),
                        TipoVersamentoEntity.class.getName(),
                        TipoVersamentoDominioEntity.class.getName()),
                List.of());
    }
}
