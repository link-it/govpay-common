# GovPay Client Commons

Libreria Spring Boot per la gestione dinamica di RestTemplate configurabili a partire dalla tabella `connettori` di GovPay.

## Caratteristiche

- **Configurazione da Database**: Carica le configurazioni dei connettori dalla tabella `connettori` di GovPay (struttura EAV)
- **Cache Intelligente**: Sistema di caching per ottimizzare le performance
- **Supporto Multi-Database**: PostgreSQL, MySQL, Oracle, SQL Server, HSQLDB
- **Autenticazione Multipla**: Supporto per vari tipi di autenticazione:
  - HTTP Basic Authentication
  - SSL/TLS con certificati client
  - OAuth2 Client Credentials
  - API Key
  - Custom HTTP Headers
  - Nessuna autenticazione

## Struttura della Tabella Connettori

La tabella `connettori` utilizza un approccio EAV (Entity-Attribute-Value):

```sql
CREATE TABLE connettori (
    id BIGINT PRIMARY KEY,
    cod_connettore VARCHAR(255) NOT NULL,
    cod_proprieta VARCHAR(255) NOT NULL,
    valore VARCHAR(255) NOT NULL,
    UNIQUE (cod_connettore, cod_proprieta)
);
```

## Proprietà Supportate

| Proprietà | Descrizione |
|-----------|-------------|
| `URL` | URL base del servizio |
| `TIPOAUTENTICAZIONE` | Tipo di autenticazione (NONE, HTTPBasic, SSL, API_KEY, HTTP_HEADER, OAUTH2_CLIENT_CREDENTIALS) |
| `HTTPUSER` | Username per HTTP Basic Auth |
| `HTTPPASSW` | Password per HTTP Basic Auth |
| `SSLKSLOCATION` | Path del keystore |
| `SSLKSPASSWD` | Password del keystore |
| `SSLKSTYPE` | Tipo di keystore (JKS, PKCS12) |
| `SSLPKEYPASSWD` | Password della chiave privata |
| `SSLTSLOCATION` | Path del truststore |
| `SSLTSPASSWD` | Password del truststore |
| `SSLTSTYPE` | Tipo di truststore |
| `HTTP_HEADER_AUTH_HEADER_NAME` | Nome dell'header personalizzato |
| `HTTP_HEADER_AUTH_HEADER_VALUE` | Valore dell'header personalizzato |
| `API_KEY_AUTH_API_KEY_NAME` | Nome dell'API Key |
| `API_KEY_AUTH_API_ID_NAME` | ID dell'API |
| `OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME` | Client ID OAuth2 |
| `OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME` | Client Secret OAuth2 |
| `OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME` | URL token endpoint OAuth2 |
| `OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME` | Scope OAuth2 |
| `SUBSCRIPTION_KEY_VALUE` | Subscription Key per Azure APIM (header: Ocp-Apim-Subscription-Key) |
| `X-CUSTOM-HEADER-NAME-N` | Nome di un custom header generico (N = indice numerico) |
| `X-CUSTOM-HEADER-VALUE-N` | Valore del custom header corrispondente (stesso indice N) |
| `ABILITATO` | Flag di abilitazione (true/false) |
| `CONNECTION_TIMEOUT` | Timeout di connessione in millisecondi |
| `READ_TIMEOUT` | Timeout di lettura in millisecondi |

## Installazione

Aggiungi la dipendenza al tuo `pom.xml`:

```xml
<dependency>
    <groupId>it.govpay</groupId>
    <artifactId>govpay-client-commons</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configurazione

### application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/govpay
    username: govpay
    password: govpay
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

govpay:
  client:
    cache:
      # Abilita/disabilita il caching delle configurazioni
      # Default: false (disabilitato)
      enabled: false
```

### Abilitazione Auto-Configuration

La libreria si auto-configura automaticamente. Assicurati che lo scan delle componenti includa il package:

```java
@SpringBootApplication
@ComponentScan(basePackages = {"it.govpay.client.commons", "com.tuoprogetto"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## Utilizzo

### Chiamate Sincrone con RestTemplate

```java
@Service
public class MioServizio {

    @Autowired
    private ConnettoreService connettoreService;

    public void chiamataEsempio() {
        // Ottieni il RestTemplate configurato per il connettore
        RestTemplate restTemplate = connettoreService.getRestTemplate("COD_CONNETTORE");

        // Usa il RestTemplate per le chiamate REST
        ResponseEntity<String> response = restTemplate.getForEntity("/api/endpoint", String.class);
        System.out.println(response.getBody());
    }
}
```

### Chiamate Asincrone con CompletableFuture

Per batch ad alto volume o chiamate parallele, usa `AsyncRestTemplateWrapper`:

```java
@Service
public class MyPivotBatchService {

    @Autowired
    private ConnettoreService connettoreService;

    @Scheduled(cron = "0 0 2 * * *")
    public void batchMyPivot() {
        // Ottieni il wrapper asincrono
        AsyncRestTemplateWrapper asyncClient =
            connettoreService.getAsyncRestTemplate("MYPIVOT_CLIENT");

        // GET asincrono
        CompletableFuture<ResponseEntity<FlussiDTO>> future =
            asyncClient.getForEntityAsync("/api/flussi", FlussiDTO.class);

        future.thenAccept(response -> {
            log.info("Ricevuti {} flussi", response.getBody().size());
            processaFlussi(response.getBody());
        });

        // POST asincrono
        CompletableFuture<ResponseEntity<RispostaDTO>> postFuture =
            asyncClient.postForEntityAsync("/api/posizioni", request, RispostaDTO.class);

        postFuture.thenAccept(response ->
            log.info("Posizione creata: {}", response.getBody())
        );
    }
}
```

### Chiamate Parallele Multiple

```java
@Service
public class RendicontazioneBatchService {

    @Autowired
    private ConnettoreService connettoreService;

    @Scheduled(cron = "0 0 3 * * *")
    public void elaboraRendicontazioni() {
        AsyncRestTemplateWrapper asyncClient =
            connettoreService.getAsyncRestTemplate("ENTE_CREDITORE");

        List<String> codiciEnte = Arrays.asList("E001", "E002", "E003", "E004", "E005");

        // Crea una lista di future per chiamate parallele
        List<CompletableFuture<ResponseEntity<RendicontazioneDTO>>> futures =
            codiciEnte.stream()
                .map(codice -> asyncClient.getForEntityAsync(
                    "/api/rendicontazioni/" + codice,
                    RendicontazioneDTO.class
                ))
                .toList();

        // Attendi il completamento di tutte le chiamate
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                List<RendicontazioneDTO> results = futures.stream()
                    .map(CompletableFuture::join)
                    .map(ResponseEntity::getBody)
                    .toList();

                log.info("Elaborate {} rendicontazioni", results.size());
                saveToDatabase(results);
            });
    }
}
```

### Gestione Errori Asincrona

```java
asyncClient.getForEntityAsync("/api/data", DataDTO.class)
    .exceptionally(ex -> {
        log.error("Errore nella chiamata: {}", ex.getMessage());
        return ResponseEntity.status(500).body(new DataDTO()); // Fallback
    })
    .thenAccept(response -> {
        if (response.getStatusCode().is2xxSuccessful()) {
            processData(response.getBody());
        }
    });
```

### Configurazione Thread Pool Asincrono

Il thread pool per le operazioni asincrone è configurabile tramite properties:

```yaml
govpay:
  client:
    async:
      core-pool-size: 10       # Thread sempre attivi (default: 10)
      max-pool-size: 50        # Thread massimi (default: 50)
      queue-capacity: 100      # Task in coda (default: 100)
      thread-name-prefix: async-http-  # Prefisso nome thread
```

**Dimensionamento suggerito:**
- Batch low-volume (< 50 chiamate): core=5, max=20
- Batch medium-volume (50-200 chiamate): core=10, max=50 (default)
- Batch high-volume (> 200 chiamate): core=20, max=100

### Gestione della Cache

La cache è **disabilitata di default** per garantire che le modifiche alla configurazione nel database siano immediatamente effettive.

#### Abilitazione Cache

Per abilitare la cache e migliorare le performance:

```yaml
govpay:
  client:
    cache:
      enabled: true
```

#### Operazioni sulla Cache

```java
// Verifica se la cache è abilitata
boolean enabled = connettoreService.isCacheEnabled();

// Invalida la cache per un connettore specifico
connettoreService.invalidateCache("COD_CONNETTORE");

// Ricarica un connettore specifico
connettoreService.reloadConnettore("COD_CONNETTORE");

// Refresh completo della cache
connettoreService.refreshCache();

// Verifica dimensione cache
int size = connettoreService.getCacheSize();

// Verifica se un connettore è in cache
boolean inCache = connettoreService.isInCache("COD_CONNETTORE");
```

**Note:**
- Se la cache è disabilitata, tutte le operazioni di gestione cache vengono ignorate
- Con cache disabilitata, ogni chiamata a `getRestTemplate()` carica la configurazione dal database
- Con cache abilitata, le configurazioni vengono precaricate all'avvio e mantenute in memoria
- Ricorda di invalidare la cache dopo aver modificato le configurazioni nel database

## Esempio di Configurazione nel Database

### Connettore con HTTP Basic Auth

```sql
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('SERVIZIO_BASIC', 'URL', 'https://api.example.com'),
('SERVIZIO_BASIC', 'TIPOAUTENTICAZIONE', 'HTTPBasic'),
('SERVIZIO_BASIC', 'HTTPUSER', 'username'),
('SERVIZIO_BASIC', 'HTTPPASSW', 'password'),
('SERVIZIO_BASIC', 'ABILITATO', 'true'),
('SERVIZIO_BASIC', 'CONNECTION_TIMEOUT', '5000'),
('SERVIZIO_BASIC', 'READ_TIMEOUT', '30000');
```

### Connettore con API Key

```sql
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('SERVIZIO_APIKEY', 'URL', 'https://api.withkey.com'),
('SERVIZIO_APIKEY', 'TIPOAUTENTICAZIONE', 'API_KEY'),
('SERVIZIO_APIKEY', 'API_KEY_AUTH_API_KEY_NAME', 'your-api-key'),
('SERVIZIO_APIKEY', 'API_KEY_AUTH_API_ID_NAME', 'X-API-Key'),
('SERVIZIO_APIKEY', 'ABILITATO', 'true');
```

### Connettore con OAuth2

```sql
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('SERVIZIO_OAUTH2', 'URL', 'https://api.oauth.com'),
('SERVIZIO_OAUTH2', 'TIPOAUTENTICAZIONE', 'OAUTH2_CLIENT_CREDENTIALS'),
('SERVIZIO_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_CLIENT_ID_NAME', 'client-id'),
('SERVIZIO_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_CLIENT_SECRET_NAME', 'client-secret'),
('SERVIZIO_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_URL_TOKEN_ENDPOINT_NAME', 'https://auth.oauth.com/token'),
('SERVIZIO_OAUTH2', 'OAUTH2_CLIENT_CREDENTIALS_SCOPE_NAME', 'read write'),
('SERVIZIO_OAUTH2', 'ABILITATO', 'true');
```

### Connettore con Azure APIM Subscription Key

```sql
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('SERVIZIO_AZURE', 'URL', 'https://myapi.azure-api.net'),
('SERVIZIO_AZURE', 'TIPOAUTENTICAZIONE', 'NONE'),
('SERVIZIO_AZURE', 'SUBSCRIPTION_KEY_VALUE', 'your-subscription-key-here'),
('SERVIZIO_AZURE', 'ABILITATO', 'true');
```

Questo configurerà automaticamente l'header `Ocp-Apim-Subscription-Key` per le chiamate verso Azure API Management.

### Connettore con Custom Headers Generici

Il sistema supporta l'aggiunta di header HTTP personalizzati utilizzando coppie di proprietà con pattern indicizzato:

```sql
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('SERVIZIO_CUSTOM_HEADERS', 'URL', 'https://api.example.com'),
('SERVIZIO_CUSTOM_HEADERS', 'TIPOAUTENTICAZIONE', 'NONE'),
-- Custom header 1: API versioning
('SERVIZIO_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-NAME-1', 'X-Api-Version'),
('SERVIZIO_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-VALUE-1', '2.0'),
-- Custom header 2: Tracing
('SERVIZIO_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-NAME-2', 'X-Trace-Id'),
('SERVIZIO_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-VALUE-2', 'govpay-trace'),
-- Custom header 3: Client identification
('SERVIZIO_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-NAME-3', 'X-Client-Name'),
('SERVIZIO_CUSTOM_HEADERS', 'X-CUSTOM-HEADER-VALUE-3', 'GovPay'),
('SERVIZIO_CUSTOM_HEADERS', 'ABILITATO', 'true');
```

**Caratteristiche:**
- Gli indici (N) devono essere uguali per nome e valore dello stesso header
- Non c'è limite al numero di custom headers (usa indici sequenziali: 1, 2, 3, ...)
- Gli header personalizzati sono applicati a tutte le chiamate HTTP del connettore
- I custom headers sono compatibili con tutti i tipi di autenticazione
- Possono essere combinati con Subscription Key di Azure APIM

**Esempio combinato (API Key + Custom Headers):**
```sql
INSERT INTO connettori (cod_connettore, cod_proprieta, valore) VALUES
('SERVIZIO_COMBINATO', 'URL', 'https://api.partner.com'),
('SERVIZIO_COMBINATO', 'TIPOAUTENTICAZIONE', 'API_KEY'),
('SERVIZIO_COMBINATO', 'API_KEY_AUTH_API_KEY_NAME', 'secret-api-key-123'),
('SERVIZIO_COMBINATO', 'API_KEY_AUTH_API_ID_NAME', 'X-API-Key'),
-- Custom headers aggiuntivi
('SERVIZIO_COMBINATO', 'X-CUSTOM-HEADER-NAME-1', 'X-Partner-Id'),
('SERVIZIO_COMBINATO', 'X-CUSTOM-HEADER-VALUE-1', 'GOVPAY_001'),
('SERVIZIO_COMBINATO', 'X-CUSTOM-HEADER-NAME-2', 'X-Request-Source'),
('SERVIZIO_COMBINATO', 'X-CUSTOM-HEADER-VALUE-2', 'GovPay-Backend'),
('SERVIZIO_COMBINATO', 'ABILITATO', 'true');
```

Questo configurerà:
- Header `X-API-Key: secret-api-key-123` (autenticazione)
- Header `X-Partner-Id: GOVPAY_001` (custom)
- Header `X-Request-Source: GovPay-Backend` (custom)

## Logging

Configura il livello di log in `application.yml`:

```yaml
logging:
  level:
    it.govpay.client.commons: DEBUG
```

## Architettura

### Componenti Principali

- **ConnettoreEntity**: Entity JPA che mappa la tabella `connettori` (struttura EAV)
- **Connettore**: Model di dominio che rappresenta un connettore con tutte le sue proprietà
- **ConnettoreConverter**: Converte da EAV (lista di entity) a model di dominio
- **ConnettoreEntityRepository**: Repository per accedere alla tabella
- **RestTemplateFactory**: Factory per creare RestTemplate configurati
- **ConnettoreService**: Service principale con gestione cache e lifecycle

### Test Suite

- **68 test automatici** con copertura ~85%
- Unit test: ConnettoreConverter, RestTemplateFactory
- Integration test: ConnettoreService (cache on/off)
- End-to-end test con H2 in-memory database
- 9 connettori di test pre-configurati

## Quality Assurance

La libreria include strumenti di Quality Assurance integrati per garantire sicurezza e qualità del codice.

### Code Coverage con Jacoco

Jacoco è configurato per generare report di code coverage durante l'esecuzione dei test.

```bash
# Esegui test con coverage
mvn clean test

# Report disponibile in: target/site/jacoco/index.html
```

**Report generati:**
- `target/site/jacoco/index.html` - Report HTML navigabile
- `target/site/jacoco/jacoco.xml` - Report XML per SonarQube

### OWASP Dependency Check

Plugin per rilevare vulnerabilità note (CVE) nelle dipendenze del progetto.

```bash
# Esegui analisi vulnerabilità durante verify
mvn clean verify

# Esegui solo dependency check
mvn dependency-check:aggregate

# Report disponibile in: target/dependency-check-report.html
```

**Configurazione:**
- `failBuildOnCVSS: 0` - Build fallisce per qualsiasi vulnerabilità rilevata
- `autoUpdate: true` - Aggiorna automaticamente il database CVE
- `nvdApiDelay: 120000` - Delay 2 minuti tra richieste NVD API

**Per disabilitare il check durante sviluppo:**
```bash
mvn clean verify -Dowasp=none
```

**Per modificare la soglia di fallimento:**
```xml
<!-- pom.xml -->
<owasp.plugin.failBuildOnCVSS>7</owasp.plugin.failBuildOnCVSS>
```
- `0` = Fallisce per qualsiasi vulnerabilità
- `7` = Fallisce solo per vulnerabilità HIGH/CRITICAL (CVSS ≥ 7.0)
- `11` = Non fallisce mai (max CVSS è 10.0)

## Compatibilità

- Java 21+
- Spring Boot 3.5.6+
- Apache Tomcat 10.1.48+ (CVE-2025-61795 fixed)
- Database supportati: PostgreSQL, MySQL, Oracle, SQL Server, HSQLDB

## Licenza

Copyright (c) 2025 - Licenza GNU GPL v3
