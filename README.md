<p align="center">
<img src="https://www.link.it/wp-content/uploads/2025/01/logo-govpay.svg" alt="GovPay Logo" width="200"/>
</p>

# GovPay Common

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://raw.githubusercontent.com/link-it/govpay-common/main/LICENSE)

Libreria Spring Boot condivisa tra tutti i moduli GovPay. Fornisce:

- **Connettori HTTP**: RestTemplate configurabili da database (tabella `connettori`, struttura EAV)
- **GDE (Giornale degli Eventi)**: Invio e gestione eventi con policy log/dump configurabili
- **Batch**: Componenti riutilizzabili per job Spring Batch multi-nodo, con metriche di esecuzione su Micrometer/Prometheus
- **Configurazione**: Accesso tipizzato alle configurazioni da database (tabella `configurazione`)
- **Mail**: Invio email con supporto SSL/TLS
- **Date/Time**: Serializzatori Jackson per formati pagoPA

## Installazione

```xml
<dependency>
    <groupId>org.gov4j.govpay</groupId>
    <artifactId>govpay-common</artifactId>
    <version>2.0.4-SNAPSHOT</version>
</dependency>
```

## Moduli

### Connettori HTTP (`it.govpay.common.client`)

Configurazione dinamica di RestTemplate dalla tabella `connettori` di GovPay (struttura EAV).

#### Struttura della Tabella

```sql
CREATE TABLE connettori (
    id BIGINT PRIMARY KEY,
    cod_connettore VARCHAR(255) NOT NULL,
    cod_proprieta VARCHAR(255) NOT NULL,
    valore VARCHAR(255) NOT NULL,
    UNIQUE (cod_connettore, cod_proprieta)
);
```

#### Tipi di Autenticazione

| Tipo | Descrizione |
|------|-------------|
| `NONE` | Nessuna autenticazione |
| `HTTPBasic` | Basic auth con username/password |
| `SSL` | Certificato client (keystore/truststore) |
| `API_KEY` | API key in header configurabile |
| `HTTP_HEADER` | Header di autenticazione personalizzato |
| `OAUTH2_CLIENT_CREDENTIALS` | OAuth2 client credentials flow |

#### Proprieta' Supportate

| Proprieta' | Descrizione |
|------------|-------------|
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

#### Utilizzo

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

#### Configurazione Cache e Thread Pool

```yaml
govpay:
  client:
    cache:
      enabled: false  # Default: disabilitata. Ogni getRestTemplate() carica dal DB.
    async:
      core-pool-size: 10
      max-pool-size: 50
      queue-capacity: 100
      thread-name-prefix: async-http-
```

#### Operazioni sulla Cache

```java
connettoreService.invalidateCache("COD_CONNETTORE");
connettoreService.reloadConnettore("COD_CONNETTORE");
connettoreService.refreshCache();
```

---

### GDE - Giornale degli Eventi (`it.govpay.common.gde`)

Componenti per l'invio di eventi al Giornale degli Eventi di GovPay con policy di log/dump configurabili da database.

#### Architettura

```
RestTemplate con GdeCapturingInterceptor
    |
HttpDataHolder (cattura request/response)
    |
GdeEventInfo (DTO con dati evento)
    |
AbstractGdeService.inviaEvento()
    |--- resolveGdeEventoPolicy() --> ConfigurazioneService.getGiornale() --> DB
    |--- isRequestLettura/Scrittura() --> GdeUtils (classifica operazione)
    |--- logEvento/dumpEvento policy check
    |--- convertToGdeEvent()
    |
NuovoEvento --> EventiApi.addEvento() --> GDE Backend
```

#### Componenti

- **AbstractGdeService**: Classe base astratta per servizi GDE con:
  - Invio sincrono e asincrono di eventi
  - Valutazione automatica policy log/dump dal Giornale in database
  - Classificazione lettura/scrittura con logica specifica per API_PAGOPA e tracciati
  - Metodi di utilita' (determineEsito, extractStatusCode, buildUrl, ecc.)

- **GdeEventInfo**: DTO builder per costruire eventi GDE

- **GdeUtils**: Metodi statici per:
  - Serializzazione payload in Base64
  - Costruzione URL con path/query parameters
  - Gestione headers HTTP
  - Valutazione policy log/dump (`logEvento`, `dumpEvento`)
  - Classificazione operazioni (`isRequestLettura`, `isRequestScrittura`, `isOperazioneScrittura`)
  - Mapping componente/configurazione (`getConfigurazioneComponente`)

- **GdeCostanti**: Costanti per nomi eventi (API pagoPA, GPD, MyPivot, Secim, GovPay, HyperSIC APKappa)

- **GdeCapturingInterceptor**: Interceptor RestTemplate che cattura request/response in HttpDataHolder

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

#### Policy Log/Dump

La configurazione e' letta dalla tabella `configurazione` (chiave `giornale_eventi`):

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

Valori possibili per `log` e `dump`: `SEMPRE`, `MAI`, `SOLO_ERRORE`.

---

### Batch (`it.govpay.common.batch`)

Componenti riutilizzabili per job Spring Batch con coordinamento multi-nodo.

#### Componenti

- **JobConcurrencyService**: Previene esecuzioni concorrenti su piu' nodi tramite JobRepository. Rileva job stale/bloccati.
- **JobExecutionHelper**: Helper per eseguire job con parametri standard (JobID, When, ClusterID). Tiene traccia delle esecuzioni avviate dal proprio processo.
- **AbstractCronJobRunner**: Classe base per esecuzione CLI (profilo `cron`). Esegue una volta ed esce.
- **AbstractScheduledJobRunner**: Classe base per scheduling interno (profilo `default`) con `@Scheduled`.
- **AbstractBatchController**: Controller REST con endpoint per esecuzione manuale, stato, ultima/prossima esecuzione.
- **AbstractBatchExecutionListener**: Logging strutturato per esecuzioni batch con statistiche aggregate per step partizionati.
- **DTOs**: `BatchStatusInfo`, `LastExecutionInfo`, `NextExecutionInfo`, `Problem` (RFC 7807)
- **BatchMetricsAutoConfiguration**: Metriche di esecuzione dei job, attive di default (vedi "Metriche di esecuzione").

#### Configurazione

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

In alternativa, senza una classe di properties, il cluster-id si passa con
`createJobExecutionHelper(jobOperator, service, clusterId, zoneId)`, letto da
`govpay.batch.cluster-id` (vedi sotto).

#### Cluster-id: identificativo del nodo

Il `cluster-id` identifica in modo univoco il **nodo** che esegue il batch. Nelle installazioni
multinodo deve essere diverso su ogni nodo: con lo stesso valore, un job in esecuzione su un
altro nodo viene classificato `RUNNING_ON_THIS_NODE` e log, endpoint e metriche non indicano
il nodo reale (issue #36). La mutua esclusione non ne risente.

Configurazione consigliata nei progetti, valida anche in container senza impostare nulla:

```properties
govpay.batch.cluster-id=${GOVPAY_BATCH_CLUSTER_ID:${HOSTNAME:GovPay-Xxx-Batch}}
```

Il default di `BatchJobProperties` segue la stessa logica (`ClusterIdPredefinito`): la variabile
`HOSTNAME` (in Kubernetes e' il nome del pod), poi il nome dell'host, e solo in ultima istanza
`GovPay-Batch`. Sulle VM `HOSTNAME` di solito non e' esportata alla JVM: va impostata
`GOVPAY_BATCH_CLUSTER_ID`.

Con lo scheduler interno, se il job risulta in esecuzione con il cluster-id del nodo ma non e'
stato avviato da questo processo, un warning segnala un probabile cluster-id duplicato (oppure
un'esecuzione rimasta attiva dopo un riavvio, che diventera' stale).

#### Rilevamento Job Stale

I job sono considerati stale se in stato UNKNOWN/ABANDONED o senza aggiornamenti per oltre `stale-threshold-minutes` (default: 120). La soglia e' esposta anche come metrica `govpay.batch.stale.threshold`.

#### API REST Batch

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

#### Metriche di esecuzione

`BatchMetricsAutoConfiguration` pubblica le metriche dei job su Micrometer, quindi su
`/actuator/prometheus`. Si attiva da sola quando l'applicazione ha un `MeterRegistry`
(actuator) e Spring Batch, anche senza web: i progetti non devono configurare nulla.
Il listener viene agganciato a ogni job e l'osservatore dei tentativi di avvio a ogni
`JobExecutionHelper`.

| Serie Prometheus | Tipo | Tag | Significato |
|---|---|---|---|
| `govpay_batch_job_active_seconds_{count,sum,max}` | long task timer | `job_name`, `trigger` | esecuzioni in corso (`_count`) e da quanto (`_max`) |
| `govpay_batch_job_seconds_{bucket,count,sum,max}` | timer | `job_name`, `trigger`, `esito` | durata ed esito delle esecuzioni concluse |
| `govpay_batch_job_last_success_timestamp_seconds` | gauge | `job_name` | istante dell'ultimo successo, riletto dal DB all'avvio |
| `govpay_batch_job_last_failure_timestamp_seconds` | gauge | `job_name` | istante dell'ultimo fallimento, riletto dal DB all'avvio |
| `govpay_batch_schedule_ticks_total` | counter | `job_name`, `trigger`, `esito_tick` | tentativi di avvio, per esito del controllo |
| `govpay_batch_schedule_interval_seconds` | gauge | `job_name`, `tipo` | intervallo previsto, letto da `@Scheduled` |
| `govpay_batch_stale_threshold_seconds` | gauge | | soglia di stale |

Valori dei tag:

- `trigger`: `SCHEDULED`, `MANUAL`; `UNKNOWN` se il job non e' avviato da `JobExecutionHelper`.
- `esito`: il `BatchStatus` (`COMPLETED`, `FAILED`, `STOPPED`, `UNKNOWN`), non l'exit code.
- `esito_tick`: `CAN_PROCEED`, `STALE_ABANDONED_CAN_PROCEED`, `STALE_ABANDON_FAILED`,
  `RUNNING_ON_OTHER_NODE`, `RUNNING_ON_THIS_NODE`, e per l'avvio manuale
  `RIFIUTATO_IN_ESECUZIONE`, `FORZATO`, `FORZATURA_FALLITA`.
- `tipo`: `fixedDelay`, `fixedRate`, `cron` (per `cron`, distanza tra le due prossime esecuzioni).
- `nodo`: tag comune con il cluster-id del nodo, su **tutte** le metriche dell'applicazione,
  comprese quelle HTTP. Il valore viene da `govpay.metrics.batch.nodo`, altrimenti da
  `govpay.batch.cluster-id`, altrimenti da `BatchJobProperties`, altrimenti dal cluster-id
  predefinito del nodo. Se non coincide con il cluster-id del `JobExecutionHelper` un warning
  lo segnala all'avvio.

Il nome del job e' nel tag `job_name` e non `job`: Prometheus aggiunge a ogni target
l'etichetta `job` (nome dello scrape job) e con `honor_labels: false` rinominerebbe la
nostra in `exported_job`.

```yaml
govpay:
  metrics:
    batch:
      enabled: true          # attive di default
      tag-nodo: true         # false per non aggiungere il tag nodo
      nodo:                  # solo se il cluster-id non e' in govpay.batch.cluster-id
      duration-buckets: [1s, 5s, 30s, 1m, 5m, 15m, 30m, 1h, 2h, 4h]
      istanze-ultimi-esiti: 20
```

Perche' il tag `nodo` distingua i nodi, il cluster-id va impostato diverso su ogni nodo
(vedi "Cluster-id: identificativo del nodo"): con `GovPay-Batch` un warning lo segnala all'avvio.

Nel profilo `cron` il processo termina a fine job, prima di uno scrape: per quei batch
le metriche Prometheus non sono affidabili e fanno fede le tabelle Spring Batch.

Ricette PromQL (analisi completa nella issue #31):

```promql
# Batch fermo: nessun successo da piu' di 3 intervalli
(time() - max by (job_name) (govpay_batch_job_last_success_timestamp_seconds))
  > 3 * max by (job_name) (govpay_batch_schedule_interval_seconds)

# Ultimo esito KO
max by (job_name) (govpay_batch_job_last_failure_timestamp_seconds)
  > max by (job_name) (govpay_batch_job_last_success_timestamp_seconds)

# Tasso di errore nelle ultime 24 h
sum by (job_name) (increase(govpay_batch_job_seconds_count{esito="FAILED"}[24h]))
  / sum by (job_name) (increase(govpay_batch_job_seconds_count[24h]))

# p95 della durata rispetto all'intervallo
histogram_quantile(0.95,
  sum by (job_name, le) (rate(govpay_batch_job_seconds_bucket{esito="COMPLETED"}[24h])))
  / max by (job_name) (govpay_batch_schedule_interval_seconds)

# Quota di tick saltati sullo stesso nodo
sum by (job_name, nodo) (increase(govpay_batch_schedule_ticks_total{trigger="SCHEDULED", esito_tick="RUNNING_ON_THIS_NODE"}[24h]))
  / sum by (job_name, nodo) (increase(govpay_batch_schedule_ticks_total{trigger="SCHEDULED"}[24h]))

# Job stale che non e' stato possibile abbandonare
sum by (job_name) (increase(govpay_batch_schedule_ticks_total{esito_tick="STALE_ABANDON_FAILED"}[1h])) > 0

# Esecuzione in corso oltre l'80% della soglia di stale
max by (job_name, nodo) (govpay_batch_job_active_seconds_max)
  > on (nodo) group_left 0.8 * max by (nodo) (govpay_batch_stale_threshold_seconds)
```

---

### Configurazione (`it.govpay.common.configurazione`)

Accesso tipizzato alle configurazioni JSON memorizzate nella tabella `configurazione`.

#### Struttura della Tabella

```sql
CREATE TABLE configurazione (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL UNIQUE,
    valore CLOB
);
```

#### Configurazioni Disponibili

| Chiave | Tipo | Descrizione |
|--------|------|-------------|
| `giornale_eventi` | `Giornale` | Policy log/dump per ogni interfaccia API |
| `tracciato_csv` | `TracciatoCsv` | Template per tracciati CSV |
| `hardening` | `Hardening` | Configurazione Google reCAPTCHA |
| `mail_batch` | `MailBatch` | Configurazione server SMTP |
| `app_io_batch` | `AppIOBatch` | Configurazione API App IO |
| `avvisatura_mail` | `AvvisaturaViaMail` | Template promemoria email |
| `avvisatura_app_io` | `AvvisaturaViaAppIo` | Template promemoria App IO |

#### Utilizzo

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

---

### Mail (`it.govpay.common.mail`)

Invio email con supporto SSL/TLS, allegati e configurazione da database.

- **AbstractMailService**: Template Method per invio email. Le sottoclassi implementano `getMailServer()`.
- **MailInfo**: DTO con to, cc, bcc, from, oggetto, testo, html, encoding, allegati, userAgent, contentLanguage, messageIdDomain.

---

### Date/Time (`it.govpay.common.utils`)

Serializzatori/deserializzatori Jackson per formati data pagoPA.

- **OffsetDateTimeSerializer**: Serializza con pattern configurabile (default: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX`)
- **OffsetDateTimeDeserializer**: Deserializza con fallback multipli per formati pagoPA variabili (millisecondi 1-9 cifre, timezone opzionale)
- **LocalDateFlexibleDeserializer**: Gestisce sia `yyyy-MM-dd` che datetime completi
- **IuvUtils**: Validazione IUV pagoPA basata su AuxDigit e codice segregazione

---

## Build

Richiede Maven 3.8.1 o successivo (controllato dall'enforcer del BOM `govpay-bom`).

```bash
# Build completa
mvn clean install

# Test
mvn clean test

# Test singolo
mvn test -Dtest=ConnettoreServiceWithCacheTest
mvn test -Dtest=ConnettoreConverterTest#testToModelWithBasicAuth

# OWASP dependency check
mvn clean verify

# Disabilitare OWASP durante sviluppo
mvn clean verify -Dowasp.phase=none
```

### SBOM (CycloneDX)

La pipeline CI/CD genera automaticamente un SBOM in formato CycloneDX (JSON + XML, schema 1.6) tramite `cyclonedx-maven-plugin` su push di `main` e su tag. Il report viene pubblicato come artefatto `sbom-report` e incluso nello ZIP dei report della GitHub Release. L'esecuzione e' controllabile con le variabili `DISABLE_SBOM_JOB` e `FORCE_SBOM_JOB`.

Generazione locale:

```bash
mvn org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom \
  -DoutputDirectory=sbom/cyclonedx \
  -DoutputName=bom.cdx \
  -DoutputFormat=all \
  -DschemaVersion=1.6
```

## Test

- **821 test automatici**
- Unit test con Mockito
- Integration test con database H2 in-memory
- Test GDE con verifica policy log/dump da database
- Test delle metriche dei batch con un job reale su H2 e verifica dei nomi delle serie sullo scrape Prometheus

## Compatibilita'

- Java 21+
- Spring Boot 4.1 (Spring Framework 7, Spring Batch 6, Micrometer 1.17)
- Database supportati: PostgreSQL, MySQL, Oracle, SQL Server, H2

## Licenza

Copyright (c) 2014-2026 Link.it srl - Licenza GNU GPL v3
