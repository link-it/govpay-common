package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AvvisaturaViaAppIo {

    private PromemoriaAvvisoBase promemoriaAvviso;
    private PromemoriaRicevutaBase promemoriaRicevuta;
    private PromemoriaScadenza promemoriaScadenza;
}
