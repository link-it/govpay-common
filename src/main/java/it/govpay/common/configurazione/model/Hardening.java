package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Hardening {

    private boolean abilitato;
    private GoogleCaptcha googleCatpcha;
}
