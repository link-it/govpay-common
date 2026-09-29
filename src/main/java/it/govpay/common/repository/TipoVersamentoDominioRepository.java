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

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.govpay.common.entity.TipoVersamentoDominioEntity;

@Repository
public interface TipoVersamentoDominioRepository extends JpaRepository<TipoVersamentoDominioEntity, Long> {

    Optional<TipoVersamentoDominioEntity> findByDominioIdAndTipoVersamentoId(Long dominioId, Long tipoVersamentoId);

    /**
     * Come {@link #findByDominioIdAndTipoVersamentoId}, ma a partire dal codice pubblico
     * (non l'id numerico) di {@link it.govpay.common.entity.TipoVersamentoEntity}, con
     * {@code tipoVersamento} caricato eagerly ({@code join fetch}): usata da chi risolve
     * {@code idTipoPendenza} dello YAML v3 di {@code govpay-pendenze-api} per costruire una
     * risposta REST fuori dalla transazione di scrittura originale — senza il fetch,
     * {@code getTipoVersamento()} resta un proxy LAZY che solleva
     * {@code LazyInitializationException} non appena letto a sessione chiusa.
     *
     * @param codTipoVersamento codice della tipologia (campo pubblico {@code idTipoPendenza})
     * @param dominioId         id del dominio
     * @return l'override per quel dominio, se esiste
     */
    @Query("select tvd from TipoVersamentoDominioEntity tvd join fetch tvd.tipoVersamento tv "
            + "where tv.codTipoVersamento = :codTipoVersamento and tvd.dominio.id = :dominioId")
    Optional<TipoVersamentoDominioEntity> findByCodTipoVersamentoAndDominioId(
            @Param("codTipoVersamento") String codTipoVersamento, @Param("dominioId") Long dominioId);

    /**
     * Come {@link #findById(Long)} ma con {@code tipoVersamento} caricato eagerly
     * ({@code join fetch}) — stessa ragione di {@link #findByCodTipoVersamentoAndDominioId}.
     *
     * @param id id dell'override
     * @return l'override, con il suo {@code TipoVersamentoEntity} gia' caricato, se esiste
     */
    @Query("select tvd from TipoVersamentoDominioEntity tvd join fetch tvd.tipoVersamento where tvd.id = :id")
    Optional<TipoVersamentoDominioEntity> findByIdFetchTipoVersamento(@Param("id") Long id);
}
