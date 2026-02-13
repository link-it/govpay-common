package it.govpay.common.entity;

public enum TipoAutenticazione {
    NONE("Nessuna autenticazione"),
    HTTPBasic("HTTP Basic Authentication"),
    SSL("SSL/TLS con certificati client"),
    OAUTH2_CLIENT_CREDENTIALS("OAuth2 Client Credentials"),
    API_KEY("API Key Authentication"),
    HTTP_HEADER("Custom HTTP Header Authentication");

    private final String descrizione;

    TipoAutenticazione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public static TipoAutenticazione fromGovPayAuthType(String authType) {
        if (authType == null) {
            return NONE;
        }
        return switch (authType) {
            case "NONE" -> NONE;
            case "HTTPBasic" -> HTTPBasic;
            case "SSL" -> SSL;
            case "OAUTH2_CLIENT_CREDENTIALS" -> OAUTH2_CLIENT_CREDENTIALS;
            case "API_KEY" -> API_KEY;
            case "HTTP_HEADER" -> HTTP_HEADER;
            default -> NONE;
        };
    }
}
