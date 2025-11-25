-- Dati di test per connettori

-- Connettore 1: HTTP Basic Auth
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_BASIC', 'URL', 'https://api.test-basic.com'),
('TEST_BASIC', 'TIPOAUTENTICAZIONE', 'HTTPBasic'),
('TEST_BASIC', 'HTTPUSER', 'testuser'),
('TEST_BASIC', 'HTTPPASSW', 'testpass'),
('TEST_BASIC', 'ABILITATO', 'true'),
('TEST_BASIC', 'CONNECTION_TIMEOUT', '5000'),
('TEST_BASIC', 'READ_TIMEOUT', '30000');

-- Connettore 2: API Key
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_APIKEY', 'URL', 'https://api.test-apikey.com'),
('TEST_APIKEY', 'TIPOAUTENTICAZIONE', 'API_KEY'),
('TEST_APIKEY', 'API_KEY_AUTH_API_KEY_NAME', 'test-api-key-123'),
('TEST_APIKEY', 'API_KEY_AUTH_API_ID_NAME', 'X-API-Key'),
('TEST_APIKEY', 'ABILITATO', 'true');

-- Connettore 3: Custom Headers
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_CUSTOM_HEADERS', 'URL', 'https://api.test-headers.com'),
('TEST_CUSTOM_HEADERS', 'TIPOAUTENTICAZIONE', 'NONE'),
('TEST_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-NAME-1', 'X-Api-Version'),
('TEST_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-VALUE-1', '2.0'),
('TEST_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-NAME-2', 'X-Trace-Id'),
('TEST_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-VALUE-2', 'test-trace'),
('TEST_CUSTOM_HEADERS', 'ABILITATO', 'true');

-- Connettore 4: Azure APIM Subscription Key
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_AZURE', 'URL', 'https://myapi.azure-api.net'),
('TEST_AZURE', 'TIPOAUTENTICAZIONE', 'NONE'),
('TEST_AZURE', 'SUBSCRIPTION_KEY_VALUE', 'test-subscription-key'),
('TEST_AZURE', 'ABILITATO', 'true');

-- Connettore 5: HTTP Header Auth
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_HTTP_HEADER', 'URL', 'https://api.test-header.com'),
('TEST_HTTP_HEADER', 'TIPOAUTENTICAZIONE', 'HTTP_HEADER'),
('TEST_HTTP_HEADER', 'HTTP_HEADER_AUTH_HEADER_NAME', 'X-Auth-Token'),
('TEST_HTTP_HEADER', 'HTTP_HEADER_AUTH_HEADER_VALUE', 'secret-token-123'),
('TEST_HTTP_HEADER', 'ABILITATO', 'true');

-- Connettore 6: OAuth2 Client Credentials
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_OAUTH2', 'URL', 'https://api.test-oauth.com'),
('TEST_OAUTH2', 'TIPOAUTENTICAZIONE', 'OAUTH2_CLIENT_CREDENTIALS'),
('TEST_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME', 'client-id-123'),
('TEST_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME', 'client-secret-456'),
('TEST_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME', 'https://auth.test.com/token'),
('TEST_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME', 'read write'),
('TEST_OAUTH2', 'ABILITATO', 'true');

-- Connettore 7: Combinato (API Key + Subscription Key + Custom Headers)
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_COMBINED', 'URL', 'https://api.test-combined.com'),
('TEST_COMBINED', 'TIPOAUTENTICAZIONE', 'API_KEY'),
('TEST_COMBINED', 'API_KEY_AUTH_API_KEY_NAME', 'combined-api-key'),
('TEST_COMBINED', 'API_KEY_AUTH_API_ID_NAME', 'X-API-Key'),
('TEST_COMBINED', 'SUBSCRIPTION_KEY_VALUE', 'combined-subscription'),
('TEST_COMBINED', 'X-CUSTOM-HEADER-NAME-1', 'X-Partner-Id'),
('TEST_COMBINED', 'X-CUSTOM-HEADER-VALUE-1', 'PARTNER_001'),
('TEST_COMBINED', 'ABILITATO', 'true');

-- Connettore 8: Disabilitato
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_DISABLED', 'URL', 'https://api.test-disabled.com'),
('TEST_DISABLED', 'TIPOAUTENTICAZIONE', 'NONE'),
('TEST_DISABLED', 'ABILITATO', 'false');

-- Connettore 9: NONE auth
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('TEST_NONE', 'URL', 'https://api.test-none.com'),
('TEST_NONE', 'TIPOAUTENTICAZIONE', 'NONE'),
('TEST_NONE', 'ABILITATO', 'true');
