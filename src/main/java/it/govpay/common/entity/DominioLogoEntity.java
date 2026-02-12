package it.govpay.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "domini")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DominioLogoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_dominio")
    private String codDominio;

    @Column(name = "logo")
    private byte[] logo;
}
