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

-- Dati di test per configurazione

INSERT INTO configurazione (nome, valore) VALUES
('giornale_eventi', '{"apiEnte":{"letture":{"log":"SEMPRE","dump":"SEMPRE"},"scritture":{"log":"SEMPRE","dump":"SOLO_ERRORE"}},"apiPagamento":{"letture":{"log":"MAI","dump":"MAI"},"scritture":{"log":"SEMPRE","dump":"SEMPRE"}}}');

INSERT INTO configurazione (nome, valore) VALUES
('tracciato_csv', '{"tipo":"freemarker","intestazione":"idA2A|idPendenza|idDominio","richiesta":"${idA2A}|${idPendenza}|${idDominio}","risposta":"${esito}"}');

INSERT INTO configurazione (nome, valore) VALUES
('hardening', '{"abilitato":true,"googleCatpcha":{"serverURL":"https://www.google.com/recaptcha/api/siteverify","siteKey":"test-site-key","secretKey":"test-secret-key","soglia":0.7,"responseParameter":"g-recaptcha-response","denyOnFail":true,"connectionTimeout":5000,"readTimeout":5000}}');

INSERT INTO configurazione (nome, valore) VALUES
('mail_batch', '{"abilitato":true,"mailserver":{"host":"smtp.test.com","port":587,"username":"test@test.com","password":"secret","from":"noreply@test.com","readTimeout":10000,"connectionTimeout":5000,"startTls":true}}');

INSERT INTO configurazione (nome, valore) VALUES
('app_io_batch', '{"abilitato":false,"timeToLive":3600,"url":"https://api.io.italia.it"}');

INSERT INTO configurazione (nome, valore) VALUES
('avvisatura_mail', '{"promemoriaAvviso":{"tipo":"freemarker","oggetto":"Avviso di pagamento","messaggio":"Gentile utente...","allegaPdf":true},"promemoriaRicevuta":{"tipo":"freemarker","oggetto":"Ricevuta di pagamento","messaggio":"Pagamento effettuato...","soloEseguiti":true,"allegaPdf":false},"promemoriaScadenza":{"tipo":"freemarker","oggetto":"Scadenza pagamento","messaggio":"Il pagamento scade...","preavviso":7}}');

INSERT INTO configurazione (nome, valore) VALUES
('avvisatura_app_io', '{"promemoriaAvviso":{"tipo":"freemarker","oggetto":"Avviso IO","messaggio":"Messaggio IO avviso"},"promemoriaRicevuta":{"tipo":"freemarker","oggetto":"Ricevuta IO","messaggio":"Messaggio IO ricevuta","soloEseguiti":false},"promemoriaScadenza":{"tipo":"freemarker","oggetto":"Scadenza IO","messaggio":"Messaggio IO scadenza","preavviso":3}}');

-- Dati di test per intermediari

INSERT INTO intermediari (cod_intermediario, cod_connettore_pdd, cod_connettore_recupero_rt, cod_connettore_aca, cod_connettore_gpd, cod_connettore_fr, cod_connettore_backoffice_ec, cod_connettore_ftp, denominazione, principal, principal_originale, abilitato)
VALUES ('12345678901', 'TEST_BASIC', 'TEST_APIKEY', 'TEST_NONE', 'TEST_OAUTH2', 'TEST_AZURE', 'TEST_HTTP_HEADER', 'TEST_CUSTOM_HEADERS', 'Intermediario di Test', 'PRINCIPAL_TEST', 'PRINCIPAL_TEST_ORIG', true);

INSERT INTO intermediari (cod_intermediario, denominazione, abilitato)
VALUES ('99999999999', 'Intermediario Disabilitato', false);

-- Dati di test per stazioni

INSERT INTO stazioni (cod_stazione, password, abilitato, application_code, versione, id_intermediario)
VALUES ('12345678901_01', 'password01', true, 1, '2', (SELECT id FROM intermediari WHERE cod_intermediario = '12345678901'));

INSERT INTO stazioni (cod_stazione, password, abilitato, application_code, versione, id_intermediario)
VALUES ('12345678901_02', 'password02', true, 2, '1', (SELECT id FROM intermediari WHERE cod_intermediario = '12345678901'));

INSERT INTO stazioni (cod_stazione, password, abilitato, application_code, versione, id_intermediario)
VALUES ('99999999999_01', 'password99', false, 1, '2', (SELECT id FROM intermediari WHERE cod_intermediario = '99999999999'));

-- Dati di test per domini

INSERT INTO domini (cod_dominio, abilitato, ragione_sociale, aux_digit, iuv_prefix, segregation_code, cbill, intermediato, tassonomia_pago_pa, scarica_fr, id_stazione)
VALUES ('01234567890', true, 'Comune di Test', 3, 'TST', 01, 'ABCDE', true, 'PagoPa_01', true,
        (SELECT id FROM stazioni WHERE cod_stazione = '12345678901_01'));

INSERT INTO domini (cod_dominio, abilitato, ragione_sociale, aux_digit, intermediato, scarica_fr, id_stazione)
VALUES ('09876543210', true, 'Provincia di Test', 0, false, false,
        (SELECT id FROM stazioni WHERE cod_stazione = '12345678901_02'));

INSERT INTO domini (cod_dominio, abilitato, ragione_sociale, aux_digit, intermediato, scarica_fr)
VALUES ('00000000000', false, 'Ente Disabilitato', 0, false, false);

INSERT INTO domini (cod_dominio, abilitato, ragione_sociale, aux_digit, intermediato, scarica_fr, logo, id_stazione)
VALUES ('11111111111', true, 'Ente con Logo', 0, true, false, X'89504E47',
        (SELECT id FROM stazioni WHERE cod_stazione = '12345678901_01'));
