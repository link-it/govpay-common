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
