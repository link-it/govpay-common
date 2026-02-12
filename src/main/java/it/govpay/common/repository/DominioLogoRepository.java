package it.govpay.common.repository;

import it.govpay.common.entity.DominioLogoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DominioLogoRepository extends JpaRepository<DominioLogoEntity, Long> {

    @Query("SELECT dl.logo FROM DominioLogoEntity dl WHERE dl.codDominio = :codDominio")
    Optional<byte[]> findLogoByCodDominio(@Param("codDominio") String codDominio);
}
