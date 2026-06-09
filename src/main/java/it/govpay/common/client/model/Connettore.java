/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2026 Link.it srl (http://www.link.it).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.govpay.common.client.model;

import it.govpay.common.entity.TipoAutenticazione;
import it.govpay.common.entity.VersioneApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Connettore {

    public static final String P_URL_NAME = "URL";
    public static final String P_TIPOAUTENTICAZIONE_NAME = "TIPOAUTENTICAZIONE";
    public static final String P_HTTPUSER_NAME = "HTTPUSER";
    public static final String P_HTTPPASSW_NAME = "HTTPPASSW";
    public static final String P_TIPOSSL_NAME = "TIPOSSL";
    public static final String P_SSLKSTYPE_NAME = "SSLKSTYPE";
    public static final String P_SSLKSLOCATION_NAME = "SSLKSLOCATION";
    public static final String P_SSLKSPASS_WORD_NAME = "SSLKSPASSWD";
    public static final String P_SSLPKEYPASS_WORD_NAME = "SSLPKEYPASSWD";
    public static final String P_SSLTSTYPE_NAME = "SSLTSTYPE";
    public static final String P_SSLTSLOCATION_NAME = "SSLTSLOCATION";
    public static final String P_SSLTSPASS_WORD_NAME = "SSLTSPASSWD";
    public static final String P_SSLTYPE_NAME = "SSLTYPE";
    public static final String P_HTTP_HEADER_AUTH_HEADER_NAME_NAME = "HTTP_HEADER_AUTH_HEADER_NAME";
    public static final String P_HTTP_HEADER_AUTH_HEADER_VALUE_NAME = "HTTP_HEADER_AUTH_HEADER_VALUE";
    public static final String P_API_KEY_AUTH_API_KEY_NAME = "API_KEY_AUTH_API_KEY_NAME";
    public static final String P_API_KEY_AUTH_API_ID_NAME = "API_KEY_AUTH_API_ID_NAME";
    public static final String P_OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME = "OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME";
    public static final String P_OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME = "OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME";
    public static final String P_OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME = "OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME";
    public static final String P_OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME = "OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME";
    public static final String P_SUBSCRIPTION_KEY_VALUE = "SUBSCRIPTION_KEY_VALUE";
    public static final String P_ABILITATO = "ABILITATO";
    public static final String P_CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
    public static final String P_READ_TIMEOUT = "READ_TIMEOUT";
    public static final String P_VERSIONE = "VERSIONE";

    // Custom Headers - pattern: X-CUSTOM-HEADER-NAME-N / X-CUSTOM-HEADER-VALUE-N
    public static final String P_CUSTOM_HEADER_NAME_PREFIX = "X-CUSTOM-HEADER-NAME-";
    public static final String P_CUSTOM_HEADER_VALUE_PREFIX = "X-CUSTOM-HEADER-VALUE-";

    public enum EnumSslType {
        CLIENT, SERVER
    }

    private String idConnettore;
    private String url;
    private TipoAutenticazione tipoAutenticazione;
    private VersioneApi versione;

    private String httpUser;
    private String httpPassw;

    private EnumSslType tipoSsl;
    private String sslKsType;
    private String sslKsLocation;
    private String sslKsPasswd;
    private String sslPKeyPasswd;
    private String sslTsType;
    private String sslTsLocation;
    private String sslTsPasswd;
    private String sslType;

    private String httpHeaderName;
    private String httpHeaderValue;

    private String apiKey;
    private String apiId;

    private String oauth2ClientCredentialsClientId;
    private String oauth2ClientCredentialsClientSecret;
    private String oauth2ClientCredentialsUrlTokenEndpoint;
    private String oauth2ClientCredentialsScope;

    private String subscriptionKeyValue;

    private Map<String, String> customHeaders;

    @Builder.Default
    private boolean abilitato = true;
    
    @Builder.Default
    private Integer connectionTimeout = 5000;
    
    @Builder.Default
    private Integer readTimeout = 30000;

    public Connettore(Connettore src) {
        this.idConnettore = src.idConnettore;
        this.url = src.url;
        this.tipoAutenticazione = src.tipoAutenticazione;
        this.versione = src.versione;
        this.httpUser = src.httpUser;
        this.httpPassw = src.httpPassw;
        this.tipoSsl = src.tipoSsl;
        this.sslKsType = src.sslKsType;
        this.sslKsLocation = src.sslKsLocation;
        this.sslKsPasswd = src.sslKsPasswd;
        this.sslPKeyPasswd = src.sslPKeyPasswd;
        this.sslTsType = src.sslTsType;
        this.sslTsLocation = src.sslTsLocation;
        this.sslTsPasswd = src.sslTsPasswd;
        this.sslType = src.sslType;
        this.httpHeaderName = src.httpHeaderName;
        this.httpHeaderValue = src.httpHeaderValue;
        this.apiKey = src.apiKey;
        this.apiId = src.apiId;
        this.oauth2ClientCredentialsClientId = src.oauth2ClientCredentialsClientId;
        this.oauth2ClientCredentialsClientSecret = src.oauth2ClientCredentialsClientSecret;
        this.oauth2ClientCredentialsUrlTokenEndpoint = src.oauth2ClientCredentialsUrlTokenEndpoint;
        this.oauth2ClientCredentialsScope = src.oauth2ClientCredentialsScope;
        this.subscriptionKeyValue = src.subscriptionKeyValue;
        this.customHeaders = src.customHeaders != null ? new HashMap<>(src.customHeaders) : null;
        this.abilitato = src.abilitato;
        this.connectionTimeout = src.connectionTimeout;
        this.readTimeout = src.readTimeout;
    }
}
