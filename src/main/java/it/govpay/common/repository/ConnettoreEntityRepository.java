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

import it.govpay.common.entity.ConnettoreEntity;
import it.govpay.common.client.model.Connettore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnettoreEntityRepository extends JpaRepository<ConnettoreEntity, Long> {

    List<ConnettoreEntity> findByCodConnettore(String codConnettore);

    @Query("SELECT DISTINCT c.codConnettore FROM ConnettoreEntity c")
    List<String> findAllCodiciConnettore();

    @Query("SELECT c FROM ConnettoreEntity c WHERE c.codConnettore IN " +
           "(SELECT DISTINCT c2.codConnettore FROM ConnettoreEntity c2 " +
           "WHERE c2.codProprieta = '" + Connettore.P_ABILITATO + "' AND c2.valore = 'true')")
    List<ConnettoreEntity> findAllAbilitati();

    @Query("SELECT c FROM ConnettoreEntity c WHERE c.codConnettore = :codConnettore " +
           "AND EXISTS (SELECT 1 FROM ConnettoreEntity c2 " +
           "WHERE c2.codConnettore = :codConnettore " +
           "AND c2.codProprieta = '" + Connettore.P_ABILITATO + "' " +
           "AND c2.valore = 'true')")
    List<ConnettoreEntity> findByCodConnettoreAndAbilitato(@Param("codConnettore") String codConnettore);
}
