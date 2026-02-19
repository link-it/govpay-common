package it.govpay.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stazioni")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StazioneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_stazione", nullable = false, unique = true, length = 255)
    private String codStazione;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;

    @Column(name = "application_code")
    private Integer applicationCode;

    @Column(name = "versione", length = 255)
    private String versione;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_intermediario", nullable = false)
    private IntermediarioEntity intermediario;
}
