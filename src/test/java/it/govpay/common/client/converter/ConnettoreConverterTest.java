package it.govpay.common.client.converter;

import it.govpay.common.client.entity.ConnettoreEntity;
import it.govpay.common.client.enums.TipoAutenticazione;
import it.govpay.common.client.model.Connettore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConnettoreConverterTest {

    @Test
    void testToModel_EmptyList() {
        Connettore result = ConnettoreConverter.toModel(null);
        assertNull(result);

        result = ConnettoreConverter.toModel(new ArrayList<>());
        assertNull(result);
    }

    @Test
    void testToModel_BasicAuth() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_BASIC", "URL", "https://api.test.com"));
        entities.add(createEntity("TEST_BASIC", "TIPOAUTENTICAZIONE", "HTTPBasic"));
        entities.add(createEntity("TEST_BASIC", "HTTPUSER", "testuser"));
        entities.add(createEntity("TEST_BASIC", "HTTPPASSW", "testpass"));
        entities.add(createEntity("TEST_BASIC", "ABILITATO", "true"));
        entities.add(createEntity("TEST_BASIC", "CONNECTION_TIMEOUT", "5000"));
        entities.add(createEntity("TEST_BASIC", "READ_TIMEOUT", "30000"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals("TEST_BASIC", result.getIdConnettore());
        assertEquals("https://api.test.com", result.getUrl());
        assertEquals(TipoAutenticazione.HTTPBasic, result.getTipoAutenticazione());
        assertEquals("testuser", result.getHttpUser());
        assertEquals("testpass", result.getHttpPassw());
        assertTrue(result.isAbilitato());
        assertEquals(5000, result.getConnectionTimeout());
        assertEquals(30000, result.getReadTimeout());
    }

    @Test
    void testToModel_ApiKey() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_APIKEY", "URL", "https://api.key.com"));
        entities.add(createEntity("TEST_APIKEY", "TIPOAUTENTICAZIONE", "API_KEY"));
        entities.add(createEntity("TEST_APIKEY", "API_KEY_AUTH_API_KEY_NAME", "my-api-key"));
        entities.add(createEntity("TEST_APIKEY", "API_KEY_AUTH_API_ID_NAME", "X-API-Key"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals("TEST_APIKEY", result.getIdConnettore());
        assertEquals(TipoAutenticazione.API_KEY, result.getTipoAutenticazione());
        assertEquals("my-api-key", result.getApiKey());
        assertEquals("X-API-Key", result.getApiId());
    }

    @Test
    void testToModel_HttpHeader() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_HEADER", "URL", "https://api.header.com"));
        entities.add(createEntity("TEST_HEADER", "TIPOAUTENTICAZIONE", "HTTP_HEADER"));
        entities.add(createEntity("TEST_HEADER", "HTTP_HEADER_AUTH_HEADER_NAME", "X-Custom-Auth"));
        entities.add(createEntity("TEST_HEADER", "HTTP_HEADER_AUTH_HEADER_VALUE", "secret-value"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals(TipoAutenticazione.HTTP_HEADER, result.getTipoAutenticazione());
        assertEquals("X-Custom-Auth", result.getHttpHeaderName());
        assertEquals("secret-value", result.getHttpHeaderValue());
    }

    @Test
    void testToModel_OAuth2ClientCredentials() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_OAUTH2", "URL", "https://api.oauth.com"));
        entities.add(createEntity("TEST_OAUTH2", "TIPOAUTENTICAZIONE", "OAUTH2_CLIENT_CREDENTIALS"));
        entities.add(createEntity("TEST_OAUTH2", "OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME", "client-id"));
        entities.add(createEntity("TEST_OAUTH2", "OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME", "client-secret"));
        entities.add(createEntity("TEST_OAUTH2", "OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME", "https://auth.com/token"));
        entities.add(createEntity("TEST_OAUTH2", "OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME", "read write"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals(TipoAutenticazione.OAUTH2_CLIENT_CREDENTIALS, result.getTipoAutenticazione());
        assertEquals("client-id", result.getOauth2ClientCredentialsClientId());
        assertEquals("client-secret", result.getOauth2ClientCredentialsClientSecret());
        assertEquals("https://auth.com/token", result.getOauth2ClientCredentialsUrlTokenEndpoint());
        assertEquals("read write", result.getOauth2ClientCredentialsScope());
    }

    @Test
    void testToModel_SubscriptionKey() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_AZURE", "URL", "https://api.azure.com"));
        entities.add(createEntity("TEST_AZURE", "TIPOAUTENTICAZIONE", "NONE"));
        entities.add(createEntity("TEST_AZURE", "SUBSCRIPTION_KEY_VALUE", "azure-subscription-key"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals("azure-subscription-key", result.getSubscriptionKeyValue());
    }

    @Test
    void testToModel_CustomHeaders() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_CUSTOM", "URL", "https://api.custom.com"));
        entities.add(createEntity("TEST_CUSTOM", "TIPOAUTENTICAZIONE", "NONE"));
        entities.add(createEntity("TEST_CUSTOM", "X-CUSTOM-HEADER-NAME-1", "X-Api-Version"));
        entities.add(createEntity("TEST_CUSTOM", "X-CUSTOM-HEADER-VALUE-1", "2.0"));
        entities.add(createEntity("TEST_CUSTOM", "X-CUSTOM-HEADER-NAME-2", "X-Trace-Id"));
        entities.add(createEntity("TEST_CUSTOM", "X-CUSTOM-HEADER-VALUE-2", "trace-123"));
        entities.add(createEntity("TEST_CUSTOM", "X-CUSTOM-HEADER-NAME-3", "X-Client"));
        entities.add(createEntity("TEST_CUSTOM", "X-CUSTOM-HEADER-VALUE-3", "GovPay"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertNotNull(result.getCustomHeaders());
        assertEquals(3, result.getCustomHeaders().size());
        assertEquals("2.0", result.getCustomHeaders().get("X-Api-Version"));
        assertEquals("trace-123", result.getCustomHeaders().get("X-Trace-Id"));
        assertEquals("GovPay", result.getCustomHeaders().get("X-Client"));
    }

    @Test
    void testToModel_CustomHeaders_MismatchedPairs() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_MISMATCH", "URL", "https://api.test.com"));
        entities.add(createEntity("TEST_MISMATCH", "TIPOAUTENTICAZIONE", "NONE"));
        entities.add(createEntity("TEST_MISMATCH", "X-CUSTOM-HEADER-NAME-1", "X-Header-1"));
        entities.add(createEntity("TEST_MISMATCH", "X-CUSTOM-HEADER-VALUE-1", "value-1"));
        // Missing value for index 2
        entities.add(createEntity("TEST_MISMATCH", "X-CUSTOM-HEADER-NAME-2", "X-Header-2"));
        // Missing name for index 3
        entities.add(createEntity("TEST_MISMATCH", "X-CUSTOM-HEADER-VALUE-3", "value-3"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertNotNull(result.getCustomHeaders());
        // Solo la coppia 1 dovrebbe essere valida
        assertEquals(1, result.getCustomHeaders().size());
        assertEquals("value-1", result.getCustomHeaders().get("X-Header-1"));
    }

    @Test
    void testToModel_SslConfiguration() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_SSL", "URL", "https://api.ssl.com"));
        entities.add(createEntity("TEST_SSL", "TIPOAUTENTICAZIONE", "SSL"));
        entities.add(createEntity("TEST_SSL", "TIPOSSL", "CLIENT"));
        entities.add(createEntity("TEST_SSL", "SSLKSTYPE", "PKCS12"));
        entities.add(createEntity("TEST_SSL", "SSLKSLOCATION", "/path/to/keystore.p12"));
        entities.add(createEntity("TEST_SSL", "SSLKSPASSWD", "keystorepass"));
        entities.add(createEntity("TEST_SSL", "SSLPKEYPASSWD", "keypass"));
        entities.add(createEntity("TEST_SSL", "SSLTSTYPE", "JKS"));
        entities.add(createEntity("TEST_SSL", "SSLTSLOCATION", "/path/to/truststore.jks"));
        entities.add(createEntity("TEST_SSL", "SSLTSPASSWD", "truststorepass"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals(TipoAutenticazione.SSL, result.getTipoAutenticazione());
        assertEquals(Connettore.EnumSslType.CLIENT, result.getTipoSsl());
        assertEquals("PKCS12", result.getSslKsType());
        assertEquals("/path/to/keystore.p12", result.getSslKsLocation());
        assertEquals("keystorepass", result.getSslKsPasswd());
        assertEquals("keypass", result.getSslPKeyPasswd());
        assertEquals("JKS", result.getSslTsType());
        assertEquals("/path/to/truststore.jks", result.getSslTsLocation());
        assertEquals("truststorepass", result.getSslTsPasswd());
    }

    @Test
    void testToModel_DefaultValues() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_DEFAULTS", "URL", "https://api.defaults.com"));
        // No TIPOAUTENTICAZIONE specified

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals(TipoAutenticazione.NONE, result.getTipoAutenticazione());
        assertTrue(result.isAbilitato()); // default true
        assertEquals(5000, result.getConnectionTimeout()); // default
        assertEquals(30000, result.getReadTimeout()); // default
    }

    @Test
    void testToModel_InvalidTimeoutValues() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_INVALID", "URL", "https://api.test.com"));
        entities.add(createEntity("TEST_INVALID", "CONNECTION_TIMEOUT", "invalid"));
        entities.add(createEntity("TEST_INVALID", "READ_TIMEOUT", "not-a-number"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        // Should use default values when parsing fails
        assertEquals(5000, result.getConnectionTimeout());
        assertEquals(30000, result.getReadTimeout());
    }

    @Test
    void testToModel_Combined() {
        List<ConnettoreEntity> entities = new ArrayList<>();
        entities.add(createEntity("TEST_COMBINED", "URL", "https://api.combined.com"));
        entities.add(createEntity("TEST_COMBINED", "TIPOAUTENTICAZIONE", "API_KEY"));
        entities.add(createEntity("TEST_COMBINED", "API_KEY_AUTH_API_KEY_NAME", "combined-key"));
        entities.add(createEntity("TEST_COMBINED", "API_KEY_AUTH_API_ID_NAME", "X-API-Key"));
        entities.add(createEntity("TEST_COMBINED", "SUBSCRIPTION_KEY_VALUE", "azure-key"));
        entities.add(createEntity("TEST_COMBINED", "X-CUSTOM-HEADER-NAME-1", "X-Partner"));
        entities.add(createEntity("TEST_COMBINED", "X-CUSTOM-HEADER-VALUE-1", "PARTNER_001"));

        Connettore result = ConnettoreConverter.toModel(entities);

        assertNotNull(result);
        assertEquals(TipoAutenticazione.API_KEY, result.getTipoAutenticazione());
        assertEquals("combined-key", result.getApiKey());
        assertEquals("azure-key", result.getSubscriptionKeyValue());
        assertNotNull(result.getCustomHeaders());
        assertEquals(1, result.getCustomHeaders().size());
        assertEquals("PARTNER_001", result.getCustomHeaders().get("X-Partner"));
    }

    private ConnettoreEntity createEntity(String codConnettore, String codProprieta, String valore) {
        ConnettoreEntity entity = new ConnettoreEntity();
        entity.setCodConnettore(codConnettore);
        entity.setCodProprieta(codProprieta);
        entity.setValore(valore);
        return entity;
    }
}
