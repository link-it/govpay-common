package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SslConfig {

    private boolean abilitato;
    private String type;
    private boolean hostnameVerifier;
    private KeyStore trustStore;
    private KeyStore keyStore;
}
