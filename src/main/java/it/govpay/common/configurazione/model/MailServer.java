package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MailServer {

    private String host;
    private int port;
    private String username;
    private String password;
    private String from;
    private Integer readTimeout;
    private Integer connectionTimeout;
    private SslConfig sslConfig;
    private boolean startTls;
}
