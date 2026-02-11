package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PromemoriaAvvisoBase {

    private String tipo;
    private String oggetto;
    private String messaggio;
}
