package it.govpay.common.repository;

import it.govpay.common.entity.ConfigurazioneEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfigurazioneRepository extends JpaRepository<ConfigurazioneEntity, Long> {

    Optional<ConfigurazioneEntity> findByNome(String nome);
}
