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

import it.govpay.common.entity.IntermediarioEntity;
import it.govpay.common.entity.StazioneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StazioneRepository extends JpaRepository<StazioneEntity, Long> {

    Optional<StazioneEntity> findByCodStazione(String codStazione);

    List<StazioneEntity> findByIntermediario(IntermediarioEntity intermediario);

    Optional<StazioneEntity> findByIntermediarioCodIntermediarioAndCodStazione(String codIntermediario, String codStazione);
}
