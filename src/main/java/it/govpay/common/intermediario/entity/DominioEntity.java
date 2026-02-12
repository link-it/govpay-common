package it.govpay.common.intermediario.entity;

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
public class DominioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cod_dominio", nullable = false, unique = true, length = 35)
    private String codDominio;

    @Column(name = "gln", length = 35)
    private String gln;

    @Column(name = "abilitato", nullable = false)
    private Boolean abilitato;

    @Column(name = "ragione_sociale", nullable = false, length = 70)
    private String ragioneSociale;

    @Column(name = "aux_digit", nullable = false)
    private Integer auxDigit;

    @Column(name = "iuv_prefix", length = 255)
    private String iuvPrefix;

    @Column(name = "segregation_code")
    private Integer segregationCode;

    @Column(name = "cbill", length = 255)
    private String cbill;

    @Column(name = "aut_stampa_poste", length = 255)
    private String autStampaPoste;

    @Column(name = "cod_connettore_my_pivot", length = 255)
    private String codConnettoreMyPivot;

    @Column(name = "cod_connettore_secim", length = 255)
    private String codConnettoreSecim;

    @Column(name = "cod_connettore_gov_pay", length = 255)
    private String codConnettoreGovPay;

    @Column(name = "cod_connettore_hyper_sic_apk", length = 255)
    private String codConnettoreHyperSicApk;

    @Column(name = "intermediato", nullable = false)
    private Boolean intermediato;

    @Column(name = "tassonomia_pago_pa", length = 35)
    private String tassonomiaPagoPa;

    @Column(name = "scarica_fr", nullable = false)
    private Boolean scaricaFr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_stazione")
    private StazioneEntity stazione;

    @Column(name = "id_applicazione_default")
    private Long idApplicazioneDefault;
}
