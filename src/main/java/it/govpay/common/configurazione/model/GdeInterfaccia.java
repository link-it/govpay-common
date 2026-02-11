package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GdeInterfaccia {

    private GdeEvento letture;
    private GdeEvento scritture;
}
