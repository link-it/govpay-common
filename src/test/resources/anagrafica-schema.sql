-- Schema REALE (non generato da Hibernate) delle tabelle coinvolte dalle entita' di
-- anagrafica aggiunte da govpay-common#9 (https://github.com/link-it/govpay-common/issues/9),
-- copiato verbatim da govpay/src/main/resources/db/sql/postgresql/gov_pay.sql: usato con
-- spring.jpa.hibernate.ddl-auto=validate in AnagraficaEntityRepositoryTest, cosi' un
-- disallineamento fra entita' e schema reale (nome colonna, tipo, lunghezza) fa fallire il
-- bootstrap del contesto invece di passare inosservato come con create-drop.
--
-- Include anche "intermediari"/"stazioni" (dipendenza transitiva: DominioEntity.stazione
-- e' un @ManyToOne, Hibernate richiede che l'intera catena di relazioni stia nella stessa
-- persistence unit). Unica modifica rispetto all'originale: rimosso il vincolo FK di
-- "domini" verso "applicazioni" (unica tabella davvero non necessaria qui, non inclusa in
-- questo schema minimale) — Hibernate in modalita' validate non verifica i vincoli FK,
-- solo esistenza/tipo delle colonne mappate.

CREATE SEQUENCE seq_intermediari start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE intermediari
(
	cod_intermediario VARCHAR(35) NOT NULL,
	cod_connettore_pdd VARCHAR(35) NOT NULL,
	cod_connettore_recupero_rt VARCHAR(35),
	cod_connettore_aca VARCHAR(35),
	cod_connettore_gpd VARCHAR(35),
	cod_connettore_fr VARCHAR(35),
	cod_connettore_backoffice_ec VARCHAR(35),
	denominazione VARCHAR(255) NOT NULL,
	principal VARCHAR(4000) NOT NULL,
	principal_originale VARCHAR(4000) NOT NULL,
	abilitato BOOLEAN NOT NULL,
	id BIGINT DEFAULT nextval('seq_intermediari') NOT NULL,
	CONSTRAINT unique_intermediari_1 UNIQUE (cod_intermediario),
	CONSTRAINT pk_intermediari PRIMARY KEY (id)
);

CREATE SEQUENCE seq_stazioni start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE stazioni
(
	cod_stazione VARCHAR(35) NOT NULL,
	password VARCHAR(35) NOT NULL,
	abilitato BOOLEAN NOT NULL,
	application_code INT NOT NULL,
	versione VARCHAR(35) NOT NULL,
	id BIGINT DEFAULT nextval('seq_stazioni') NOT NULL,
	id_intermediario BIGINT NOT NULL,
	CONSTRAINT unique_stazioni_1 UNIQUE (cod_stazione),
	CONSTRAINT fk_stz_id_intermediario FOREIGN KEY (id_intermediario) REFERENCES intermediari(id),
	CONSTRAINT pk_stazioni PRIMARY KEY (id)
);

CREATE SEQUENCE seq_domini start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE domini
(
	cod_dominio VARCHAR(35) NOT NULL,
	gln VARCHAR(35),
	abilitato BOOLEAN NOT NULL,
	ragione_sociale VARCHAR(70) NOT NULL,
	aux_digit INT NOT NULL DEFAULT 0,
	iuv_prefix VARCHAR(255),
	segregation_code INT,
	logo BYTEA,
	cbill VARCHAR(255),
	aut_stampa_poste VARCHAR(255),
	cod_connettore_my_pivot VARCHAR(255),
	cod_connettore_secim VARCHAR(255),
	cod_connettore_gov_pay VARCHAR(255),
	cod_connettore_hyper_sic_apk VARCHAR(255),
	cod_connettore_send VARCHAR(255),
	intermediato BOOLEAN NOT NULL,
	tassonomia_pago_pa VARCHAR(35),
	scarica_fr BOOLEAN NOT NULL,
	id BIGINT DEFAULT nextval('seq_domini') NOT NULL,
	id_stazione BIGINT,
	id_applicazione_default BIGINT,
	CONSTRAINT unique_domini_1 UNIQUE (cod_dominio),
	CONSTRAINT fk_dom_id_stazione FOREIGN KEY (id_stazione) REFERENCES stazioni(id),
	CONSTRAINT pk_domini PRIMARY KEY (id)
);

CREATE SEQUENCE seq_uo start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE uo
(
	cod_uo VARCHAR(35) NOT NULL,
	abilitato BOOLEAN NOT NULL,
	uo_codice_identificativo VARCHAR(35),
	uo_denominazione VARCHAR(70),
	uo_indirizzo VARCHAR(70),
	uo_civico VARCHAR(16),
	uo_cap VARCHAR(16),
	uo_localita VARCHAR(35),
	uo_provincia VARCHAR(35),
	uo_nazione VARCHAR(2),
	uo_area VARCHAR(255),
	uo_url_sito_web VARCHAR(255),
	uo_email VARCHAR(255),
	uo_pec VARCHAR(255),
	uo_tel VARCHAR(255),
	uo_fax VARCHAR(255),
	id BIGINT DEFAULT nextval('seq_uo') NOT NULL,
	id_dominio BIGINT NOT NULL,
	CONSTRAINT unique_uo_1 UNIQUE (cod_uo,id_dominio),
	CONSTRAINT fk_uo_id_dominio FOREIGN KEY (id_dominio) REFERENCES domini(id),
	CONSTRAINT pk_uo PRIMARY KEY (id)
);

CREATE SEQUENCE seq_iban_accredito start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE iban_accredito
(
	cod_iban VARCHAR(255) NOT NULL,
	bic_accredito VARCHAR(255),
	postale BOOLEAN NOT NULL,
	abilitato BOOLEAN NOT NULL,
	descrizione VARCHAR(255),
	intestatario VARCHAR(255),
	aut_stampa_poste VARCHAR(255),
	id BIGINT DEFAULT nextval('seq_iban_accredito') NOT NULL,
	id_dominio BIGINT NOT NULL,
	CONSTRAINT unique_iban_accredito_1 UNIQUE (cod_iban,id_dominio),
	CONSTRAINT fk_iba_id_dominio FOREIGN KEY (id_dominio) REFERENCES domini(id),
	CONSTRAINT pk_iban_accredito PRIMARY KEY (id)
);

CREATE SEQUENCE seq_tipi_tributo start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE tipi_tributo
(
	cod_tributo VARCHAR(255) NOT NULL,
	descrizione VARCHAR(255),
	tipo_contabilita VARCHAR(1),
	cod_contabilita VARCHAR(255),
	id BIGINT DEFAULT nextval('seq_tipi_tributo') NOT NULL,
	CONSTRAINT unique_tipi_tributo_1 UNIQUE (cod_tributo),
	CONSTRAINT pk_tipi_tributo PRIMARY KEY (id)
);

CREATE SEQUENCE seq_tributi start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE tributi
(
	abilitato BOOLEAN NOT NULL,
	tipo_contabilita VARCHAR(1),
	codice_contabilita VARCHAR(255),
	id BIGINT DEFAULT nextval('seq_tributi') NOT NULL,
	id_dominio BIGINT NOT NULL,
	id_iban_accredito BIGINT,
	id_iban_appoggio BIGINT,
	id_tipo_tributo BIGINT NOT NULL,
	CONSTRAINT unique_tributi_1 UNIQUE (id_dominio,id_tipo_tributo),
	CONSTRAINT fk_trb_id_dominio FOREIGN KEY (id_dominio) REFERENCES domini(id),
	CONSTRAINT fk_trb_id_iban_accredito FOREIGN KEY (id_iban_accredito) REFERENCES iban_accredito(id),
	CONSTRAINT fk_trb_id_iban_appoggio FOREIGN KEY (id_iban_appoggio) REFERENCES iban_accredito(id),
	CONSTRAINT fk_trb_id_tipo_tributo FOREIGN KEY (id_tipo_tributo) REFERENCES tipi_tributo(id),
	CONSTRAINT pk_tributi PRIMARY KEY (id)
);

CREATE SEQUENCE seq_tipi_versamento start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE tipi_versamento
(
	cod_tipo_versamento VARCHAR(35) NOT NULL,
	descrizione VARCHAR(255) NOT NULL,
	codifica_iuv VARCHAR(4),
	paga_terzi BOOLEAN NOT NULL DEFAULT false,
	abilitato BOOLEAN NOT NULL,
	bo_form_tipo VARCHAR(35),
	bo_form_definizione TEXT,
	bo_validazione_def TEXT,
	bo_trasformazione_tipo VARCHAR(35),
	bo_trasformazione_def TEXT,
	bo_cod_applicazione VARCHAR(35),
	bo_abilitato BOOLEAN NOT NULL DEFAULT false,
	pag_form_tipo VARCHAR(35),
	pag_form_definizione TEXT,
	pag_form_impaginazione TEXT,
	pag_validazione_def TEXT,
	pag_trasformazione_tipo VARCHAR(35),
	pag_trasformazione_def TEXT,
	pag_cod_applicazione VARCHAR(35),
	pag_abilitato BOOLEAN NOT NULL DEFAULT false,
	avv_mail_prom_avv_abilitato BOOLEAN NOT NULL DEFAULT false,
	avv_mail_prom_avv_pdf BOOLEAN,
	avv_mail_prom_avv_tipo VARCHAR(35),
	avv_mail_prom_avv_oggetto TEXT,
	avv_mail_prom_avv_messaggio TEXT,
	avv_mail_prom_ric_abilitato BOOLEAN NOT NULL DEFAULT false,
	avv_mail_prom_ric_pdf BOOLEAN,
	avv_mail_prom_ric_tipo VARCHAR(35),
	avv_mail_prom_ric_oggetto TEXT,
	avv_mail_prom_ric_messaggio TEXT,
	avv_mail_prom_ric_eseguiti BOOLEAN,
	avv_mail_prom_scad_abilitato BOOLEAN NOT NULL DEFAULT false,
	avv_mail_prom_scad_preavviso INT,
	avv_mail_prom_scad_tipo VARCHAR(35),
	avv_mail_prom_scad_oggetto TEXT,
	avv_mail_prom_scad_messaggio TEXT,
	visualizzazione_definizione TEXT,
	trac_csv_tipo VARCHAR(35),
	trac_csv_header_risposta TEXT,
	trac_csv_template_richiesta TEXT,
	trac_csv_template_risposta TEXT,
	avv_app_io_prom_avv_abilitato BOOLEAN NOT NULL DEFAULT false,
	avv_app_io_prom_avv_tipo VARCHAR(35),
	avv_app_io_prom_avv_oggetto TEXT,
	avv_app_io_prom_avv_messaggio TEXT,
	avv_app_io_prom_ric_abilitato BOOLEAN NOT NULL DEFAULT false,
	avv_app_io_prom_ric_tipo VARCHAR(35),
	avv_app_io_prom_ric_oggetto TEXT,
	avv_app_io_prom_ric_messaggio TEXT,
	avv_app_io_prom_ric_eseguiti BOOLEAN,
	avv_app_io_prom_scad_abilitato BOOLEAN NOT NULL DEFAULT false,
	avv_app_io_prom_scad_preavviso INT,
	avv_app_io_prom_scad_tipo VARCHAR(35),
	avv_app_io_prom_scad_oggetto TEXT,
	avv_app_io_prom_scad_messaggio TEXT,
	id BIGINT DEFAULT nextval('seq_tipi_versamento') NOT NULL,
	CONSTRAINT unique_tipi_versamento_1 UNIQUE (cod_tipo_versamento),
	CONSTRAINT pk_tipi_versamento PRIMARY KEY (id)
);

CREATE SEQUENCE seq_tipi_vers_domini start 1 increment 1 maxvalue 9223372036854775807 minvalue 1 cache 1 NO CYCLE;

CREATE TABLE tipi_vers_domini
(
	codifica_iuv VARCHAR(4),
	paga_terzi BOOLEAN,
	abilitato BOOLEAN,
	bo_form_tipo VARCHAR(35),
	bo_form_definizione TEXT,
	bo_validazione_def TEXT,
	bo_trasformazione_tipo VARCHAR(35),
	bo_trasformazione_def TEXT,
	bo_cod_applicazione VARCHAR(35),
	bo_abilitato BOOLEAN,
	pag_form_tipo VARCHAR(35),
	pag_form_definizione TEXT,
	pag_form_impaginazione TEXT,
	pag_validazione_def TEXT,
	pag_trasformazione_tipo VARCHAR(35),
	pag_trasformazione_def TEXT,
	pag_cod_applicazione VARCHAR(35),
	pag_abilitato BOOLEAN,
	avv_mail_prom_avv_abilitato BOOLEAN,
	avv_mail_prom_avv_pdf BOOLEAN,
	avv_mail_prom_avv_tipo VARCHAR(35),
	avv_mail_prom_avv_oggetto TEXT,
	avv_mail_prom_avv_messaggio TEXT,
	avv_mail_prom_ric_abilitato BOOLEAN,
	avv_mail_prom_ric_pdf BOOLEAN,
	avv_mail_prom_ric_tipo VARCHAR(35),
	avv_mail_prom_ric_oggetto TEXT,
	avv_mail_prom_ric_messaggio TEXT,
	avv_mail_prom_ric_eseguiti BOOLEAN,
	avv_mail_prom_scad_abilitato BOOLEAN,
	avv_mail_prom_scad_preavviso INT,
	avv_mail_prom_scad_tipo VARCHAR(35),
	avv_mail_prom_scad_oggetto TEXT,
	avv_mail_prom_scad_messaggio TEXT,
	visualizzazione_definizione TEXT,
	trac_csv_tipo VARCHAR(35),
	trac_csv_header_risposta TEXT,
	trac_csv_template_richiesta TEXT,
	trac_csv_template_risposta TEXT,
	app_io_api_key VARCHAR(255),
	avv_app_io_prom_avv_abilitato BOOLEAN,
	avv_app_io_prom_avv_tipo VARCHAR(35),
	avv_app_io_prom_avv_oggetto TEXT,
	avv_app_io_prom_avv_messaggio TEXT,
	avv_app_io_prom_ric_abilitato BOOLEAN,
	avv_app_io_prom_ric_tipo VARCHAR(35),
	avv_app_io_prom_ric_oggetto TEXT,
	avv_app_io_prom_ric_messaggio TEXT,
	avv_app_io_prom_ric_eseguiti BOOLEAN,
	avv_app_io_prom_scad_abilitato BOOLEAN,
	avv_app_io_prom_scad_preavviso INT,
	avv_app_io_prom_scad_tipo VARCHAR(35),
	avv_app_io_prom_scad_oggetto TEXT,
	avv_app_io_prom_scad_messaggio TEXT,
	id BIGINT DEFAULT nextval('seq_tipi_vers_domini') NOT NULL,
	id_tipo_versamento BIGINT NOT NULL,
	id_dominio BIGINT NOT NULL,
	CONSTRAINT unique_tipi_vers_domini_1 UNIQUE (id_dominio,id_tipo_versamento),
	CONSTRAINT fk_tvd_id_tipo_versamento FOREIGN KEY (id_tipo_versamento) REFERENCES tipi_versamento(id),
	CONSTRAINT fk_tvd_id_dominio FOREIGN KEY (id_dominio) REFERENCES domini(id),
	CONSTRAINT pk_tipi_vers_domini PRIMARY KEY (id)
);
