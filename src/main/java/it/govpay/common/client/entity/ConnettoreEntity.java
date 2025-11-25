package it.govpay.common.client.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "connettori",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cod_connettore", "cod_proprieta"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnettoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_connettore", nullable = false, length = 255)
    private String codConnettore;

    @Column(name = "cod_proprieta", nullable = false, length = 255)
    private String codProprieta;

    @Column(name = "valore", nullable = false, length = 255)
    private String valore;
}
