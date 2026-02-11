package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Giornale {

    private GdeInterfaccia apiEnte;
    private GdeInterfaccia apiPagamento;
    private GdeInterfaccia apiRagioneria;
    private GdeInterfaccia apiBackoffice;
    private GdeInterfaccia apiPagoPA;
    private GdeInterfaccia apiPendenze;
    private GdeInterfaccia apiBackendIO;
    private GdeInterfaccia apiMaggioliJPPA;
}
