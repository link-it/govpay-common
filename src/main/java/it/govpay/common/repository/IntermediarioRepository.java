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
