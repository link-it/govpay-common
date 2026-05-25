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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntermediarioRepository extends JpaRepository<IntermediarioEntity, Long> {

    Optional<IntermediarioEntity> findByCodIntermediario(String codIntermediario);

    @Query("SELECT i FROM IntermediarioEntity i " +
           "JOIN StazioneEntity s ON s.intermediario = i " +
           "JOIN DominioEntity d ON d.stazione = s " +
           "WHERE d.codDominio = :codDominio")
    Optional<IntermediarioEntity> findByCodDominio(@Param("codDominio") String codDominio);
}
