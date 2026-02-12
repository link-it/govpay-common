package it.govpay.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "configurazione")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurazioneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, unique = true, length = 255)
    private String nome;

    @Lob
    @Column(name = "valore")
    private String valore;
}
