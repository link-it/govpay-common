package it.govpay.common.intermediario.repository;

import it.govpay.common.intermediario.entity.DominioEntity;
import it.govpay.common.intermediario.entity.StazioneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DominioRepository extends JpaRepository<DominioEntity, Long> {

    Optional<DominioEntity> findByCodDominio(String codDominio);

    List<DominioEntity> findByStazione(StazioneEntity stazione);
}
