<p align="center">
<img src="https://www.link.it/wp-content/uploads/2025/01/logo-govpay.svg" alt="GovPay Logo" width="200"/>
</p>

# GovPay - Porta di accesso al sistema pagoPA - Common

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=link-it_govpay-common&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=link-it_govpay-common)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://raw.githubusercontent.com/link-it/govpay-common/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2Flink-it%2Fgovpay-bom%2Fmain%2Fpom.xml&query=%2F%2F*%5Blocal-name()%3D'spring-boot.version'%5D&label=Spring%20Boot&color=brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)

Libreria Spring Boot condivisa dai componenti GovPay: connettori HTTP configurati da database, invio di eventi al Giornale degli Eventi, componenti per i job Spring Batch, accesso alla configurazione, invio mail, tracciatura dei log e metriche.

## Sommario

La libreria non è un'applicazione: è un jar che i componenti GovPay includono come dipendenza Maven. Legge le anagrafiche e la configurazione dal database di GovPay e offre ai progetti che la usano servizi e classi base da estendere.

| Modulo | Package | Contenuto |
|--------|---------|-----------|
| [Connettori HTTP](#connettori-http) | `it.govpay.common.client` | `RestTemplate` sincroni e asincroni configurati dalla tabella `connettori` (struttura EAV), con autenticazione, timeout e cache |
| [GDE - Giornale degli Eventi](#gde---giornale-degli-eventi) | `it.govpay.common.gde` | Invio degli eventi al GDE con le policy di log e dump lette da database |
| [Batch](#batch) | `it.govpay.common.batch` | Componenti per job Spring Batch multi-nodo: concorrenza, runner, controller REST, persistenza dei metadati |
| [Configurazione](#configurazione-da-database) | `it.govpay.common.configurazione` | Accesso tipizzato alle configurazioni JSON della tabella `configurazione` |
| [Mail](#mail) | `it.govpay.common.mail` | Invio email con SSL/TLS, STARTTLS e allegati, con server SMTP letto da database |
| [Tracciatura e livelli di log](#tracciatura-e-livelli-di-log) | `it.govpay.common.logging` | Transaction id e correlation id nei log e negli header; livelli di log modificabili a runtime |
| [Metriche](#metriche) | `it.govpay.common.metrics` | Timer Micrometer per le chiamate a servizi esterni e per il tempo interno/esterno delle API |
| [Entità e repository](#entità-e-repository) | `it.govpay.common.entity`, `it.govpay.common.repository` | Entità JPA e repository delle anagrafiche di GovPay |
| [Date/Time e utilità](#datetime-e-utilità) | `it.govpay.common.utils` | Serializzatori Jackson per i formati data pagoPA, validazione IUV, utilità varie |

Le auto-configurazioni registrate in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` si attivano da sole: tracciatura dei log, livelli di log dinamici, metriche (solo se abilitate) e persistenza dei metadati Spring Batch. Gli altri moduli si attivano includendo i package della libreria nella scansione dei componenti dell'applicazione (vedi [Utilizzo](#utilizzo)).

## Integrazione con GovPay

| Componente | Quando serve | Obbligatorio | Ruolo |
|------------|--------------|--------------|-------|
| [govpay-bom](https://github.com/link-it/govpay-bom) | build | sì | Parent Maven: versioni delle dipendenze e dei plugin, compreso Spring Boot. Il profilo `dist` dei batch estrae da questa libreria gli script `sql/cleanup/**` (proprietà `govpay-common.sql.version`) |
| Database di GovPay | runtime | sì | Tabelle lette dalla libreria: `connettori`, `configurazione`, `domini`, `intermediari`, `stazioni`, `applicazioni`; entità JPA anche per `iban_accredito`, `tributi`, `tipi_tributo`, `tipi_versamento`, `tipi_vers_domini`, `uo` (repository non registrati di default) e per le tabelle Spring Batch `batch_job_instance`, `batch_job_execution`, `batch_job_execution_params`. Lo schema è creato dagli script di installazione di GovPay: la libreria non lo crea |
| [govpay-gde-api](https://github.com/link-it/govpay-gde-api) | runtime | no | Riceve gli eventi inviati dal modulo GDE con `POST /eventi` (client generato da [src/main/resources/gde/openapi.yaml](src/main/resources/gde/openapi.yaml)), all'indirizzo del connettore `govpay_gde_api`. Se il connettore manca o è disabilitato gli eventi non vengono inviati |
| Progetti che usano la libreria | build | - | Dipendenza diretta nel `pom.xml`: [govpay-aca-batch](https://github.com/link-it/govpay-aca-batch), [govpay-common-pendenze](https://github.com/link-it/govpay-common-pendenze), [govpay-console-api](https://github.com/link-it/govpay-console-api), [govpay-fdr-batch](https://github.com/link-it/govpay-fdr-batch), [govpay-gde-api](https://github.com/link-it/govpay-gde-api), [govpay-iban-batch](https://github.com/link-it/govpay-iban-batch), [govpay-maggioli-jppa](https://github.com/link-it/govpay-maggioli-jppa), [govpay-mail-batch](https://github.com/link-it/govpay-mail-batch), [govpay-notify-batch](https://github.com/link-it/govpay-notify-batch), [govpay-pafornode-api](https://github.com/link-it/govpay-pafornode-api), [govpay-portal-api](https://github.com/link-it/govpay-portal-api), [govpay-riconciliazioni-api](https://github.com/link-it/govpay-riconciliazioni-api), [govpay-rt-batch](https://github.com/link-it/govpay-rt-batch), [govpay-stampe-api](https://github.com/link-it/govpay-stampe-api), [govpay-tracciati-batch](https://github.com/link-it/govpay-tracciati-batch); [govpay-pendenze-api](https://github.com/link-it/govpay-pendenze-api) la usa tramite `govpay-common-pendenze` |

## Compilazione

Requisiti: JDK 21 e Maven 3.8.1 o successivo.

```bash
mvn clean install
```

Il risultato è `target/govpay-common-<versione>.jar`, installato nel repository Maven locale. La build genera il client del GDE, esegue i test con il report di copertura JaCoCo (`target/site/jacoco/`) e, nella fase `verify`, l'analisi OWASP Dependency-Check delle dipendenze. Per escludere l'analisi OWASP, per esempio in sviluppo:

```bash
mvn clean install -Dowasp.phase=none
```

Altri comandi utili:

```bash
# Solo i test
mvn clean test

# Test singolo
mvn test -Dtest=ConnettoreServiceWithCacheTest
mvn test -Dtest=ConnettoreConverterTest#testToModelWithBasicAuth
```

### Test

- Oltre 700 test automatici
- Unit test con Mockito
- Integration test con database H2 in-memory
- Test GDE con verifica delle policy di log e dump da database

### SBOM (CycloneDX)

La pipeline CI/CD genera automaticamente un SBOM in formato CycloneDX (JSON + XML, schema 1.6) tramite `cyclonedx-maven-plugin` su push di `main` e su tag. Il report viene pubblicato come artefatto `sbom-report` e incluso nello ZIP dei report della GitHub Release. L'esecuzione è controllabile con le variabili `DISABLE_SBOM_JOB` e `FORCE_SBOM_JOB`.

Generazione locale:

```bash
mvn org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom \
  -DoutputDirectory=sbom/cyclonedx \
  -DoutputName=bom.cdx \
  -DoutputFormat=all \
  -DschemaVersion=1.6
```

## Utilizzo

Coordinate Maven:

```xml
<dependency>
    <groupId>org.gov4j.govpay</groupId>
    <artifactId>govpay-common</artifactId>
    <version>${govpay-common.version}</version>
</dependency>
```

La versione corrente di sviluppo è `2.0.4-SNAPSHOT` (pom su `main`); i progetti GovPay la impostano con la proprietà `govpay-common.version`. Le versioni SNAPSHOT sono pubblicate su `https://central.sonatype.com/repository/maven-snapshots`.

I moduli Spring Batch e Mail dipendono da `spring-boot-starter-batch` e `spring-boot-starter-mail`, dichiarati come opzionali: il progetto che li usa deve aggiungerli alle proprie dipendenze. I driver JDBC sono in scope `provided`.

I componenti non auto-configurati si attivano aggiungendo i package della libreria alla scansione, per esempio:

```java
@SpringBootApplication(scanBasePackages = {"it.govpay.mioprogetto", "it.govpay.common.client"})
@EntityScan(basePackages = {"it.govpay.mioprogetto", "it.govpay.common.client", "it.govpay.common.entity"})
public class MiaApplicazione { }
```

`it.govpay.common.client` contiene `GovPayClientAutoConfiguration`, che a sua volta scansiona `it.govpay.common.client` e `it.govpay.common.configurazione` e registra i repository di `it.govpay.common.repository`. `ConfigurazioneAutoConfiguration` registra il solo modulo di configurazione.

### Connettori HTTP

Configurazione dinamica di `RestTemplate` dalla tabella `connettori` di GovPay (struttura EAV).

#### Struttura della tabella

```sql
CREATE TABLE connettori (
    id BIGINT PRIMARY KEY,
    cod_connettore VARCHAR(255) NOT NULL,
    cod_proprieta VARCHAR(255) NOT NULL,
    valore VARCHAR(255) NOT NULL,
    UNIQUE (cod_connettore, cod_proprieta)
);
```

#### Tipi di autenticazione

| Tipo | Descrizione |
|------|-------------|
| `NONE` | Nessuna autenticazione |
| `HTTPBasic` | Basic auth con username/password |
| `SSL` | Certificato client (keystore/truststore) |
| `API_KEY` | API key in header configurabile |
| `HTTP_HEADER` | Header di autenticazione personalizzato |
| `OAUTH2_CLIENT_CREDENTIALS` | OAuth2 client credentials flow |

#### Proprietà supportate

| Proprietà | Descrizione |
|-----------|-------------|
| `URL` | URL base del servizio |
| `TIPOAUTENTICAZIONE` | Tipo di autenticazione |
| `HTTPUSER` / `HTTPPASSW` | Credenziali HTTP Basic |
| `SSLKSLOCATION` / `SSLKSPASSWD` / `SSLKSTYPE` / `SSLPKEYPASSWD` | Configurazione keystore SSL |
| `SSLTSLOCATION` / `SSLTSPASSWD` / `SSLTSTYPE` | Configurazione truststore SSL |
| `HTTP_HEADER_AUTH_HEADER_NAME` / `VALUE` | Header autenticazione personalizzato |
| `API_KEY_AUTH_API_KEY_NAME` / `API_ID_NAME` | Configurazione API Key |
| `OAUTH2_CLIENT_CREDENTIALS_*` | Client ID, Secret, Token URL, Scope per OAuth2 |
| `SUBSCRIPTION_KEY_VALUE` | Subscription Key per Azure APIM (Ocp-Apim-Subscription-Key) |
| `X-CUSTOM-HEADER-NAME-N` / `VALUE-N` | Custom header generici (N = indice numerico) |
| `ABILITATO` | Flag di abilitazione (true/false) |
| `CONNECTION_TIMEOUT` / `READ_TIMEOUT` | Timeout in millisecondi |

`ConnettoreMapUtils` legge in modo tipizzato i valori della mappa restituita da `ConnettoreService.getConnettoreAsMap()`.

#### Esempio

```java
@Service
public class MioServizio {

    @Autowired
    private ConnettoreService connettoreService;

    public void chiamataSincrona() {
        RestTemplate restTemplate = connettoreService.getRestTemplate("COD_CONNETTORE");
        ResponseEntity<String> response = restTemplate.getForEntity("/api/endpoint", String.class);
    }

    public void chiamataAsincrona() {
        AsyncRestTemplateWrapper asyncClient =
            connettoreService.getAsyncRestTemplate("COD_CONNETTORE");

        CompletableFuture<ResponseEntity<DataDTO>> future =
            asyncClient.getForEntityAsync("/api/data", DataDTO.class);

        future.thenAccept(response -> processData(response.getBody()));
    }
}
```

Cache dei connettori e thread pool delle chiamate asincrone si configurano con le proprietà `govpay.client.*` (vedi [Configurazione](#connettori-http-1)).

#### Operazioni sulla cache

```java
connettoreService.invalidateCache("COD_CONNETTORE");
connettoreService.reloadConnettore("COD_CONNETTORE");
connettoreService.refreshCache();
```

### GDE - Giornale degli Eventi

Componenti per l'invio di eventi al Giornale degli Eventi di GovPay con policy di log/dump configurabili da database. Gli eventi vengono inviati con il `RestTemplate` del connettore `govpay_gde_api` (costante `AbstractGdeService.COD_CONNETTORE_GDE`): se il connettore non esiste o non è abilitato, l'invio viene saltato.

#### Architettura

```
RestTemplate con GdeCapturingInterceptor
    |
HttpDataHolder (cattura request/response)
    |
GdeEventInfo (DTO con dati evento)
    |
AbstractGdeService.inviaEvento()
    |--- isAbilitato() --> connettore govpay_gde_api
    |--- resolveGdeEventoPolicy() --> ConfigurazioneService.getGiornale() --> DB
    |--- isRequestLettura/Scrittura() --> GdeUtils (classifica operazione)
    |--- logEvento/dumpEvento policy check
    |--- convertToGdeEvent()
    |
NuovoEvento --> POST /eventi --> GDE Backend
```

#### Componenti

- **AbstractGdeService**: classe base astratta per servizi GDE con:
  - Invio sincrono e asincrono di eventi
  - Valutazione automatica policy log/dump dal Giornale in database
  - Classificazione lettura/scrittura con logica specifica per API_PAGOPA e tracciati
  - Metodi di utilità (determineEsito, extractStatusCode, buildUrl, ecc.)
- **GdeEventInfo**: DTO builder per costruire eventi GDE
- **GdeUtils**: metodi statici per:
  - Serializzazione payload in Base64
  - Costruzione URL con path/query parameters
  - Gestione headers HTTP
  - Valutazione policy log/dump (`logEvento`, `dumpEvento`)
  - Classificazione operazioni (`isRequestLettura`, `isRequestScrittura`, `isOperazioneScrittura`)
  - Mapping componente/configurazione (`getConfigurazioneComponente`)
- **GdeCostanti**: costanti per nomi eventi (API pagoPA, GPD, MyPivot, Secim, GovPay, HyperSIC APKappa)
- **GdeCapturingInterceptor**: interceptor RestTemplate che cattura request/response in HttpDataHolder

Il client del GDE (`it.govpay.gde.client.*`) è generato in fase di build da [src/main/resources/gde/openapi.yaml](src/main/resources/gde/openapi.yaml).

#### Implementazione

```java
@Service
public class MyGdeService extends AbstractGdeService {

    public MyGdeService(ObjectMapper objectMapper, Executor asyncExecutor,
                        ConfigurazioneService configurazioneService) {
        super(objectMapper, asyncExecutor, configurazioneService);
    }

    @Override
    protected NuovoEvento convertToGdeEvent(GdeEventInfo eventInfo) {
        NuovoEvento evento = new NuovoEvento();
        evento.setComponente(eventInfo.getComponente());
        evento.setEsito(eventInfo.getEsito());
        // ... mapping specifico del progetto
        return evento;
    }

    @Override
    protected String getGdeEndpoint() {
        return "https://gde.govpay.it/eventi";
    }

    @Override
    protected GdeInterfaccia getConfigurazioneComponente(ComponenteEvento c, Giornale g) {
        // Per i casi comuni, delega a GdeUtils
        return GdeUtils.getConfigurazioneComponente(c, g);
    }
}
```

#### Invio eventi

```java
GdeEventInfo eventInfo = GdeEventInfo.builder()
    .componente(ComponenteEvento.API_BACKOFFICE)
    .categoriaEvento(CategoriaEvento.INTERFACCIA)
    .ruolo(RuoloEvento.CLIENT)
    .tipoEvento("invioNotifica")
    .metodoHttp("POST")
    .esito(EsitoEvento.OK)
    .urlRichiesta("https://api.example.com/notifiche")
    .build();

// Asincrono (non bloccante)
gdeService.inviaEventoAsync(eventInfo);
gdeService.inviaEventoOk(eventInfo);   // imposta esito=OK
gdeService.inviaEventoKo(eventInfo);   // imposta esito=KO

// Sincrono (bloccante)
gdeService.inviaEvento(eventInfo);
```

#### Policy log/dump

La configurazione è letta dalla tabella `configurazione` (chiave `giornale_eventi`):

```json
{
  "apiEnte": {
    "letture": { "log": "SEMPRE", "dump": "SEMPRE" },
    "scritture": { "log": "SEMPRE", "dump": "SOLO_ERRORE" }
  },
  "apiPagamento": {
    "letture": { "log": "MAI", "dump": "MAI" },
    "scritture": { "log": "SEMPRE", "dump": "SEMPRE" }
  }
}
```

Valori possibili per `log` e `dump`: `SEMPRE`, `MAI`, `SOLO_ERRORE`. Se la policy dice di non registrare l'evento, l'invio viene saltato; se dice di non fare il dump, i payload di richiesta e risposta vengono rimossi prima dell'invio.

### Batch

Componenti riutilizzabili per job Spring Batch con coordinamento multi-nodo. Richiedono `spring-boot-starter-batch` nel progetto.

#### Componenti

- **JobConcurrencyService**: previene esecuzioni concorrenti su più nodi interrogando il `JobRepository`. Rileva job stale/bloccati.
- **JobExecutionHelper**: helper per eseguire job con parametri standard (JobID, When, ClusterID).
- **AbstractCronJobRunner**: classe base per esecuzione CLI (profilo `cron`). Esegue una volta ed esce.
- **AbstractScheduledJobRunner**: classe base per scheduling interno (profilo `default`) con `@Scheduled`.
- **AbstractBatchController**: controller REST con endpoint per esecuzione manuale, stato, ultima/prossima esecuzione, storico delle esecuzioni.
- **AbstractBatchExecutionListener**: logging strutturato per esecuzioni batch con statistiche aggregate per step partizionati.
- **TransactionContextJobListener**: apre il contesto di tracciatura dei log per l'esecuzione del job.
- **BatchJobRepositoryAutoConfiguration**: auto-configurazione che rende persistenti su database i metadati Spring Batch (tabelle `BATCH_*`). Senza di essa Spring Boot 4 usa un `ResourcelessJobRepository` e restano senza effetto la guardia anti-concorrenza, la soglia dei job stale, gli endpoint del controller e lo svecchiamento. Usa un `JdbcTransactionManager` dedicato sul `DataSource`: i metadati sono committati in una transazione separata da quella dei dati applicativi degli step. Si disattiva con `govpay.batch.job-repository.jdbc.enabled=false`.
- **DTOs**: `BatchInfo`, `BatchStatusInfo`, `LastExecutionInfo`, `NextExecutionInfo`, `ExecutionSummaryInfo`, `ExecutionsPage`, `Problem` (RFC 7807)

#### Configurazione dei bean

```java
@Configuration
@ConfigurationProperties(prefix = "govpay.batch")
public class MyBatchProperties extends BatchJobProperties {}

@Bean
public JobConcurrencyService jobConcurrencyService(JobRepository r, MyBatchProperties p) {
    return BatchCommonAutoConfiguration.createJobConcurrencyService(r, p);
}

@Bean
public JobExecutionHelper jobExecutionHelper(JobOperator o, JobConcurrencyService s, MyBatchProperties p) {
    return BatchCommonAutoConfiguration.createJobExecutionHelper(o, s, p);
}
```

`BatchCommonAutoConfiguration` non è una configurazione Spring registrata automaticamente: offre metodi factory statici, anche con parametri espliciti (soglia, cluster id, `ZoneId`, `Clock`). Le proprietà di `BatchJobProperties` sono elencate in [Configurazione](#batch-1).

#### Rilevamento job stale

I job sono considerati stale se in stato UNKNOWN/ABANDONED o senza aggiornamenti per oltre `stale-threshold-minutes` (default: 120).

#### API REST batch

La sottoclasse di `AbstractBatchController` sceglie il path base e la protezione; `run`, `status`, `lastExecution` e `nextExecution` li espone la sottoclasse con le proprie mappature, gli altri sono già mappati nella classe base. Specifica dei primi cinque endpoint in [src/main/resources/batch/openapi.yaml](src/main/resources/batch/openapi.yaml), descrizione delle operazioni in [src/main/resources/batch/batch-api-doc.txt](src/main/resources/batch/batch-api-doc.txt).

| Metodo e path | Descrizione |
|---------------|-------------|
| `GET /run?force=false` | Esecuzione manuale del job |
| `GET /status` | Stato corrente del job |
| `GET /lastExecution` | Ultima esecuzione completata |
| `GET /nextExecution` | Prossima esecuzione pianificata |
| `GET /info` | Informazioni sul batch |
| `GET /executions` | Elenco paginato delle esecuzioni |
| `GET /executions/{executionId}` | Dettaglio di un'esecuzione |
| `DELETE /executions/{executionId}` | Arresto di un'esecuzione |
| `GET /cache/clear` | Svuota la cache dell'applicazione |

**`GET /run?force=false`** - Esecuzione manuale del job

```
HTTP 202 Accepted
```
```json
// HTTP 409 - Job gia' in esecuzione
{ "type": "https://www.rfc-editor.org/rfc/rfc9110.html#name-409-conflict",
  "title": "Conflict", "status": 409, "detail": "Job already running." }
```

**`GET /status`** - Stato corrente del job

```json
{
  "running": true,
  "executionId": 42,
  "clusterId": "node-1",
  "startTime": "2026-04-07T10:30:00.000+02:00",
  "runningSeconds": 120,
  "status": "STARTED",
  "currentStep": "invioNotificheStep"
}
```

**`GET /lastExecution`** - Ultima esecuzione completata

```json
{
  "executionId": 41,
  "clusterId": "node-1",
  "startTime": "2026-04-07T08:00:00.000+02:00",
  "endTime": "2026-04-07T08:05:30.000+02:00",
  "durationSeconds": 330,
  "status": "COMPLETED",
  "exitCode": "COMPLETED",
  "exitDescription": ""
}
```

**`GET /nextExecution`** - Prossima esecuzione pianificata

```json
{
  "schedulingMode": "scheduler",
  "nextExecutionTime": "2026-04-07T12:00:00.000+02:00",
  "intervalMillis": 600000,
  "intervalFormatted": "10 minutes",
  "lastCompletedTime": "2026-04-07T11:50:00.000+02:00",
  "message": null
}
```

**`GET /cache/clear`** - Svuota cache applicazione

```json
"Cache svuotata con successo"
```

#### Svecchiamento dei metadati Spring Batch

In [src/main/resources/sql/cleanup](src/main/resources/sql/cleanup) ci sono gli script che cancellano le esecuzioni anteriori a una data di taglio: `spring-batch-cleanup.sh` sceglie il file `spring-batch-cleanup-<database>.sql` (PostgreSQL, MySQL/MariaDB, Oracle, SQL Server, HSQLDB) e lo esegue con SqlTool, usando le variabili di connessione `GOVPAY_DB_*` dell'init dei container. Nelle immagini Docker dei batch GovPay si trovano in `/opt/sql/cleanup/`. Lo script non filtra per stato: un job in corso iniziato prima della data di taglio viene cancellato. Le opzioni sono descritte con `spring-batch-cleanup.sh -h`.

### Configurazione da database

Accesso tipizzato alle configurazioni JSON memorizzate nella tabella `configurazione`.

#### Struttura della tabella

```sql
CREATE TABLE configurazione (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    valore CLOB
);
```

#### Configurazioni disponibili

| Chiave | Tipo | Descrizione |
|--------|------|-------------|
| `giornale_eventi` | `Giornale` | Policy log/dump per ogni interfaccia API |
| `tracciato_csv` | `TracciatoCsv` | Template per tracciati CSV |
| `hardening` | `Hardening` | Configurazione Google reCAPTCHA |
| `mail_batch` | `MailBatch` | Configurazione server SMTP |
| `app_io_batch` | `AppIOBatch` | Configurazione API App IO |
| `avvisatura_mail` | `AvvisaturaViaMail` | Template promemoria email |
| `avvisatura_app_io` | `AvvisaturaViaAppIo` | Template promemoria App IO |
| `log_level` | `LogLevelConfig` | Livelli di log impostati a runtime (vedi [Tracciatura e livelli di log](#tracciatura-e-livelli-di-log)) |

Le chiavi sono le costanti di `ConfigurazioneKeys`.

#### Esempio

```java
@Autowired
private ConfigurazioneService configurazioneService;

Optional<Giornale> giornale = configurazioneService.getGiornale();
Optional<MailBatch> mailBatch = configurazioneService.getMailBatch();
Optional<Hardening> hardening = configurazioneService.getHardening();

// Accesso generico
Optional<String> raw = configurazioneService.getValore("chiave");
Optional<MyType> typed = configurazioneService.getAsObject("chiave", MyType.class);
```

### Mail

Invio email con supporto SSL/TLS, STARTTLS, allegati e configurazione da database. Richiede `spring-boot-starter-mail` nel progetto.

- **AbstractMailService**: classe base (Template Method) per l'invio sincrono (`inviaEmail`) e asincrono (`inviaEmailAsync`). Il server SMTP è letto dalla configurazione `mail_batch` e tenuto in cache per 60 secondi (TTL modificabile dal costruttore, `resetCache()` la invalida); `isAbilitato()` indica se la configurazione esiste ed è abilitata.
- **MailInfo**: DTO con to, cc, bcc, from, oggetto, testo, html, encoding, allegati, userAgent, contentLanguage, messageIdDomain.

```java
@Service
public class MyMailService extends AbstractMailService {
    public MyMailService(ConfigurazioneService configurazioneService) {
        super(configurazioneService);
    }
}
```

### Tracciatura e livelli di log

**Tracciatura (`LoggingAutoConfiguration`)**, attiva di default:

- `TransactionIdFilter`: nelle applicazioni web servlet popola l'MDC con `transactionId` e `correlationId`, riusa il correlation id ricevuto negli header (se valido) e li restituisce negli header di risposta.
- `CorrelationIdClientInterceptor`: propaga il correlation id sulle chiamate downstream dei `RestTemplate` creati dalla libreria.
- `MdcTaskDecorator`: mantiene il contesto sui thread degli executor.
- `TransactionContext`: nelle elaborazioni non HTTP (job batch, task schedulati) il contesto si apre esplicitamente con `try (TransactionContext.Scope scope = TransactionContext.apri()) { ... }`.

Perché gli identificativi compaiano nei log va aggiunto il pattern nel progetto:

```properties
logging.pattern.level=%5p [%X{transactionId:-},%X{correlationId:-}]
```

**Livelli di log dinamici (`DynamicLogLevelAutoConfiguration`)**: si attiva quando è presente un `ConfigurazioneService`. I livelli impostati a runtime sono salvati nella riga `log_level` della tabella `configurazione` e riletti periodicamente da ogni nodo; sono modificabili solo i logger sotto i prefissi di `logger-gestiti`.

```json
{
  "loggers": {
    "it.govpay.common.client": "DEBUG",
    "it.govpay.common.gde": "TRACE"
  }
}
```

Livelli ammessi: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`, `OFF`. `AbstractLogLevelController` offre gli endpoint di gestione (elenco dei logger, impostazione, rimozione, refresh): la sottoclasse ne sceglie path e protezione, e vanno esposti solo come endpoint di amministrazione.

### Metriche

`GovpayMetricsAutoConfiguration`, disattiva di default (`govpay.metrics.enabled=true` per attivarla), si attiva solo nelle applicazioni web servlet con un `MeterRegistry` (tipicamente via actuator):

- `ExternalCallMetricsRecorder`: timer `govpay.external.service.duration` per le chiamate a servizi esterni, con tag `client`, `operation` e `outcome` (esito classificato da `ExternalCallOutcomeRegistry`);
- `ApiTimingMetricsFilter`: per ogni richiesta, tempo speso nelle chiamate esterne (`govpay.api.external.duration`) e tempo interno (`govpay.api.internal.duration`), con tag allineati a `http.server.requests`;
- `ExternalCallMetricsContext`: accumulatore per richiesta che collega i due.

### Entità e repository

Entità JPA delle anagrafiche di GovPay (`it.govpay.common.entity`): connettori, configurazione, domini (anche il solo logo), intermediari, stazioni, applicazioni, IBAN di accredito, tributi, tipi tributo, tipi versamento, tipi versamento per dominio, unità operative, e le tabelle dei metadati Spring Batch (`it.govpay.common.entity.batch`).

`GovPayClientAutoConfiguration` registra i repository di `it.govpay.common.repository` tranne `IbanAccreditoRepository`, `TipoTributoRepository`, `TipoVersamentoDominioRepository`, `TipoVersamentoRepository`, `TributoRepository` e `UnitaOperativaRepository`: chi li usa li dichiara nel proprio `@EnableJpaRepositories`.

### Date/Time e utilità

Serializzatori/deserializzatori Jackson per formati data pagoPA e utilità (`it.govpay.common.utils`).

- **OffsetDateTimeSerializer**: serializza con pattern configurabile (default: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`)
- **OffsetDateTimeDeserializer**: deserializza con fallback multipli per formati pagoPA variabili (millisecondi 1-9 cifre, timezone opzionale)
- **LocalDateFlexibleDeserializer**: gestisce sia `yyyy-MM-dd` che datetime completi
- **IuvUtils**: validazione IUV pagoPA basata su AuxDigit e codice segregazione
- **DurationUtils**: durate fra `LocalDateTime` ancorate a una `ZoneId` esplicita, corrette ai cambi di ora legale (es. tempi dei metadati Spring Batch)
- **RisolutoreSegnaposto**: sostituisce i segnaposto `%(chiave)` di un testo con i valori di una mappa

`TimezoneConfig` (`it.govpay.common.config`) imposta il fuso orario di default della JVM da `spring.jackson.time-zone` ed espone un bean `ZoneId`.

### Compatibilità

- Java 21+
- Spring Boot 4.x (versione definita dal parent [govpay-bom](https://github.com/link-it/govpay-bom))
- Database supportati: PostgreSQL, MySQL, Oracle, SQL Server, H2 (driver in scope `provided`, da fornire nel progetto che usa la libreria)

## Configurazione

La libreria non ha un proprio `application.properties`: le proprietà si impostano nell'applicazione che la usa (`application.properties`, `-D<proprietà>=<valore>` o variabili d'ambiente equivalenti di Spring Boot). Le configurazioni lette da database sono descritte in [Configurazione da database](#configurazione-da-database) e, per i connettori, in [Connettori HTTP](#connettori-http).

### Connettori HTTP

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `govpay.client.cache.enabled` | `false` | Cache in memoria dei connettori. Disabilitata: ogni `getRestTemplate()` legge il connettore dal database |
| `govpay.client.async.core-pool-size` | `10` | Thread minimi del pool delle chiamate asincrone |
| `govpay.client.async.max-pool-size` | `50` | Thread massimi del pool delle chiamate asincrone |
| `govpay.client.async.queue-capacity` | `100` | Capacità della coda del pool |
| `govpay.client.async.thread-name-prefix` | `async-http-` | Prefisso del nome dei thread del pool |

### Batch

Proprietà di `BatchJobProperties`, lette con il prefisso scelto dal progetto (nei batch GovPay `govpay.batch`):

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `govpay.batch.cluster-id` | `GovPay-Batch` | Identificativo del nodo che esegue il batch, usato per coordinare più nodi |
| `govpay.batch.stale-threshold-minutes` | `120` | Minuti senza aggiornamenti oltre i quali un job in esecuzione è considerato bloccato |
| `govpay.batch.time-zone` | `Europe/Rome` | Fuso orario per i calcoli temporali |
| `govpay.batch.scheduler-interval-millis` | `600000` | Intervallo di esecuzione con lo scheduler interno, in ms |
| `govpay.batch.initial-delay-millis` | `1` | Ritardo della prima esecuzione schedulata, in ms |
| `govpay.batch.job-repository.jdbc.enabled` | `true` | Metadati Spring Batch persistenti su database (`false`: comportamento non persistente di Spring Boot) |

### Tracciatura dei log

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `govpay.logging.tracciatura.enabled` | `true` | Attiva filtro, interceptor e task decorator della tracciatura |
| `govpay.logging.tracciatura.ordine` | `-2147483548` | Ordine del filtro nella catena servlet (`Integer.MIN_VALUE + 100`) |
| `govpay.logging.tracciatura.lunghezza-massima-id` | `128` | Lunghezza massima di un identificativo ricevuto dall'esterno |
| `govpay.logging.tracciatura.pattern-id-valido` | `[A-Za-z0-9._:\-]+` | Espressione regolare che un identificativo ricevuto deve rispettare; se non conforme ne viene generato uno nuovo |
| `govpay.logging.tracciatura.correlation-inbound-headers` | `X-Correlation-ID,X-Request-ID` | Header letti in ingresso per il correlation id, in ordine di precedenza |
| `govpay.logging.tracciatura.correlation-response-headers` | `X-Correlation-ID,X-Request-ID` | Header di risposta con il correlation id |
| `govpay.logging.tracciatura.correlation-downstream-headers` | `X-Correlation-ID,X-Request-ID` | Header con il correlation id sulle chiamate downstream |
| `govpay.logging.tracciatura.transaction-response-headers` | `X-Transaction-ID,X-Govpay-IdTransazione` | Header di risposta con il transaction id |

### Livelli di log dinamici

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `govpay.logging.livelli-dinamici.enabled` | `true` | Gestione dei livelli di log da database (attiva solo se è presente `ConfigurazioneService`) |
| `govpay.logging.livelli-dinamici.intervallo-refresh` | `60s` | Intervallo di rilettura della configurazione da database; `0` disattiva il polling |
| `govpay.logging.livelli-dinamici.ritardo-iniziale` | `60s` | Ritardo della prima rilettura dopo quella di avvio |
| `govpay.logging.livelli-dinamici.logger-gestiti` | `it.govpay` | Prefissi dei logger modificabili a runtime |

### Metriche e fuso orario

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `govpay.metrics.enabled` | `false` | Attiva le metriche custom GovPay |
| `spring.jackson.time-zone` | `Europe/Rome` | Letta da `TimezoneConfig`: fuso orario di default della JVM e bean `ZoneId` |

## Licenza

GovPay Common è distribuito con licenza [GNU General Public License v3.0](LICENSE).

Copyright (c) 2014-2026 Link.it srl.
