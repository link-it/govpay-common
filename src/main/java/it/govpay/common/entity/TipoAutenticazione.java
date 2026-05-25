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
package it.govpay.common.entity;

public enum TipoAutenticazione {
    NONE("Nessuna autenticazione"),
    HTTP_BASIC("HTTP Basic Authentication"),
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
            case "HTTPBasic" -> HTTP_BASIC;
            case "SSL" -> SSL;
            case "OAUTH2_CLIENT_CREDENTIALS" -> OAUTH2_CLIENT_CREDENTIALS;
            case "API_KEY" -> API_KEY;
            case "HTTP_HEADER" -> HTTP_HEADER;
            default -> NONE;
        };
    }
    
    @Override
    public String toString() {
		return switch (this) {
		case NONE -> "NONE";
		case HTTP_BASIC -> "HTTPBasic";
		case SSL -> "SSL";
		case OAUTH2_CLIENT_CREDENTIALS -> "OAUTH2_CLIENT_CREDENTIALS";
		case API_KEY -> "API_KEY";
		case HTTP_HEADER -> "HTTP_HEADER";
		};
    }
}
