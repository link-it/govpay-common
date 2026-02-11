package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TracciatoCsv {

    private String tipo;
    private String intestazione;
    private String richiesta;
    private String risposta;
}
