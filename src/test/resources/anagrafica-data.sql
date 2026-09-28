-- Dati di seed per AnagraficaEntityRepositoryTest, sullo schema reale in anagrafica-schema.sql.

INSERT INTO domini (id, cod_dominio, abilitato, ragione_sociale, aux_digit, intermediato, scarica_fr)
VALUES (1, '01234567890', true, 'Comune di Test', 3, false, false);

INSERT INTO uo (id, cod_uo, abilitato, uo_denominazione, id_dominio)
VALUES (1, 'UO-TEST', true, 'Ufficio Tributi', 1);

INSERT INTO iban_accredito (id, cod_iban, postale, abilitato, descrizione, id_dominio)
VALUES (1, 'IT60X0542811101000000123456', false, true, 'Conto principale', 1);

INSERT INTO tipi_tributo (id, cod_tributo, descrizione, tipo_contabilita, cod_contabilita)
VALUES (1, 'TARI', 'Tassa rifiuti', '2', '1234');

INSERT INTO tributi (id, abilitato, tipo_contabilita, codice_contabilita, id_dominio, id_iban_accredito, id_tipo_tributo)
VALUES (1, true, '2', '1234', 1, 1, 1);

INSERT INTO tipi_versamento (id, cod_tipo_versamento, descrizione, abilitato, paga_terzi)
VALUES (1, 'IMU', 'Imposta Municipale Unica', true, false);

INSERT INTO tipi_vers_domini (id, id_tipo_versamento, id_dominio)
VALUES (1, 1, 1);
