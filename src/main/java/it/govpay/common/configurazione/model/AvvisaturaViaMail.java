package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AvvisaturaViaMail {

    private PromemoriaAvviso promemoriaAvviso;
    private PromemoriaRicevuta promemoriaRicevuta;
    private PromemoriaScadenza promemoriaScadenza;
}
