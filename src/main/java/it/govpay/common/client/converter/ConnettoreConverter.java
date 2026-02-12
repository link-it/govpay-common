package it.govpay.common.client.converter;

import it.govpay.common.entity.ConnettoreEntity;
import it.govpay.common.client.enums.TipoAutenticazione;
import it.govpay.common.client.model.Connettore;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConnettoreConverter {

    private ConnettoreConverter() {
    }

    public static Connettore toModel(List<ConnettoreEntity> connettoreEntities) {
        if (connettoreEntities == null || connettoreEntities.isEmpty()) {
            log.warn("Lista connettori vuota");
            return null;
        }

        Connettore connettore = new Connettore();
        connettore.setIdConnettore(connettoreEntities.get(0).getCodConnettore());

        // Mappe temporanee per gestire i custom headers
        Map<String, String> customHeaderNames = new HashMap<>();
        Map<String, String> customHeaderValues = new HashMap<>();

        for (ConnettoreEntity entity : connettoreEntities) {
            String proprieta = entity.getCodProprieta();
            String valore = entity.getValore();

            // Gestione custom headers con pattern X-CUSTOM-HEADER-NAME-N / X-CUSTOM-HEADER-VALUE-N
            if (proprieta.startsWith(Connettore.P_CUSTOM_HEADER_NAME_PREFIX)) {
                String index = proprieta.substring(Connettore.P_CUSTOM_HEADER_NAME_PREFIX.length());
                customHeaderNames.put(index, valore);
                continue;
            }
            if (proprieta.startsWith(Connettore.P_CUSTOM_HEADER_VALUE_PREFIX)) {
                String index = proprieta.substring(Connettore.P_CUSTOM_HEADER_VALUE_PREFIX.length());
                customHeaderValues.put(index, valore);
                continue;
            }

            switch (proprieta) {
                case Connettore.P_URL_NAME -> connettore.setUrl(valore);
                case Connettore.P_TIPOAUTENTICAZIONE_NAME ->
                        connettore.setTipoAutenticazione(TipoAutenticazione.fromGovPayAuthType(valore));
                case Connettore.P_HTTPUSER_NAME -> connettore.setHttpUser(valore);
                case Connettore.P_HTTPPASSW_NAME -> connettore.setHttpPassw(valore);
                case Connettore.P_TIPOSSL_NAME -> {
                    if (valore != null) {
                        connettore.setTipoSsl(Connettore.EnumSslType.valueOf(valore));
                    }
                }
                case Connettore.P_SSLKSLOCATION_NAME -> connettore.setSslKsLocation(valore);
                case Connettore.P_SSLKSPASS_WORD_NAME -> connettore.setSslKsPasswd(valore);
                case Connettore.P_SSLKSTYPE_NAME -> connettore.setSslKsType(valore);
                case Connettore.P_SSLPKEYPASS_WORD_NAME -> connettore.setSslPKeyPasswd(valore);
                case Connettore.P_SSLTSLOCATION_NAME -> connettore.setSslTsLocation(valore);
                case Connettore.P_SSLTSPASS_WORD_NAME -> connettore.setSslTsPasswd(valore);
                case Connettore.P_SSLTSTYPE_NAME -> connettore.setSslTsType(valore);
                case Connettore.P_SSLTYPE_NAME -> connettore.setSslType(valore);
                case Connettore.P_HTTP_HEADER_AUTH_HEADER_NAME_NAME -> connettore.setHttpHeaderName(valore);
                case Connettore.P_HTTP_HEADER_AUTH_HEADER_VALUE_NAME -> connettore.setHttpHeaderValue(valore);
                case Connettore.P_API_KEY_AUTH_API_ID_NAME -> connettore.setApiId(valore);
                case Connettore.P_API_KEY_AUTH_API_KEY_NAME -> connettore.setApiKey(valore);
                case Connettore.P_OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME ->
                        connettore.setOauth2ClientCredentialsClientId(valore);
                case Connettore.P_OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME ->
                        connettore.setOauth2ClientCredentialsClientSecret(valore);
                case Connettore.P_OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME ->
                        connettore.setOauth2ClientCredentialsUrlTokenEndpoint(valore);
                case Connettore.P_OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME ->
                        connettore.setOauth2ClientCredentialsScope(valore);
                case Connettore.P_SUBSCRIPTION_KEY_VALUE -> connettore.setSubscriptionKeyValue(valore);
                case Connettore.P_ABILITATO -> connettore.setAbilitato(Boolean.parseBoolean(valore));
                case Connettore.P_CONNECTION_TIMEOUT -> {
                    try {
                        connettore.setConnectionTimeout(Integer.parseInt(valore));
                    } catch (NumberFormatException e) {
                        log.warn("Valore non valido per CONNECTION_TIMEOUT: {}", valore);
                    }
                }
                case Connettore.P_READ_TIMEOUT -> {
                    try {
                        connettore.setReadTimeout(Integer.parseInt(valore));
                    } catch (NumberFormatException e) {
                        log.warn("Valore non valido per READ_TIMEOUT: {}", valore);
                    }
                }
                default -> log.debug("Proprietà non gestita: {}", proprieta);
            }
        }

        if (connettore.getTipoAutenticazione() == null) {
            connettore.setTipoAutenticazione(TipoAutenticazione.NONE);
        }

        // Popola customHeaders matchando gli indici
        if (!customHeaderNames.isEmpty() || !customHeaderValues.isEmpty()) {
            Map<String, String> customHeaders = new HashMap<>();
            for (String index : customHeaderNames.keySet()) {
                String headerName = customHeaderNames.get(index);
                String headerValue = customHeaderValues.get(index);
                if (headerName != null && headerValue != null) {
                    customHeaders.put(headerName, headerValue);
                    log.debug("Custom header aggiunto: {} = {}", headerName, headerValue);
                } else if (headerName != null) {
                    log.warn("Custom header '{}' (indice {}) manca del valore corrispondente", headerName, index);
                }
            }
            // Controlla anche header value senza name corrispondente
            for (String index : customHeaderValues.keySet()) {
                if (!customHeaderNames.containsKey(index)) {
                    log.warn("Custom header value per indice {} manca del nome corrispondente", index);
                }
            }
            if (!customHeaders.isEmpty()) {
                connettore.setCustomHeaders(customHeaders);
            }
        }

        log.debug("Connettore convertito: {} - Custom headers: {}",
                connettore.getIdConnettore(),
                connettore.getCustomHeaders() != null ? connettore.getCustomHeaders().size() : 0);
        return connettore;
    }
}
