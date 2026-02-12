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
