package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KeyStore {

    private String type;
    private String location;
    private String password;
    private String managementAlgorithm;
}
