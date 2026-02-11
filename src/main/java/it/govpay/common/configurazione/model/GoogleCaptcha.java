package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GoogleCaptcha {

    private String serverURL;
    private String siteKey;
    private String secretKey;
    private double soglia;
    private String responseParameter;
    private boolean denyOnFail;
    private int connectionTimeout;
    private int readTimeout;
}
