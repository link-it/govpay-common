package it.govpay.common.mail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import it.govpay.common.configurazione.model.MailBatch;
import it.govpay.common.configurazione.model.MailServer;
import it.govpay.common.configurazione.model.SslConfig;
import it.govpay.common.configurazione.service.ConfigurazioneService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

@ExtendWith(MockitoExtension.class)
class AbstractMailServiceTest {

    @Mock
    private ConfigurazioneService configurazioneService;

    @Mock
    private JavaMailSender mockMailSender;

    private TestMailService mailService;

    /** Concrete subclass that overrides buildMailSender to avoid real SMTP connections */
    static class TestMailService extends AbstractMailService {
        private final JavaMailSender overrideMailSender;

        TestMailService(ConfigurazioneService configurazioneService, JavaMailSender overrideMailSender) {
            super(configurazioneService);
            this.overrideMailSender = overrideMailSender;
        }

        @Override
        protected JavaMailSender buildMailSender(MailServer mailServer) {
            return overrideMailSender;
        }
    }

    private static MimeMessage newMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static MailBatch abilitato(MailServer mailServer) {
        MailBatch mb = new MailBatch();
        mb.setAbilitato(true);
        mb.setMailserver(mailServer);
        return mb;
    }

    private static MailServer defaultMailServer() {
        MailServer ms = new MailServer();
        ms.setHost("smtp.test.local");
        ms.setPort(25);
        ms.setFrom("noreply@test.local");
        return ms;
    }

    @BeforeEach
    void setUp() {
        mailService = new TestMailService(configurazioneService, mockMailSender);
    }

    // ==================== isAbilitato ====================

    @Nested
    @DisplayName("isAbilitato")
    class IsAbilitato {

        @Test
        @DisplayName("true quando MailBatch presente e abilitato")
        void trueQuandoAbilitato() {
            MailBatch mb = new MailBatch();
            mb.setAbilitato(true);
            when(configurazioneService.getMailBatch()).thenReturn(Optional.of(mb));

            assertTrue(mailService.isAbilitato());
        }

        @Test
        @DisplayName("false quando MailBatch presente ma non abilitato")
        void falseQuandoNonAbilitato() {
            MailBatch mb = new MailBatch();
            mb.setAbilitato(false);
            when(configurazioneService.getMailBatch()).thenReturn(Optional.of(mb));

            assertFalse(mailService.isAbilitato());
        }

        @Test
        @DisplayName("false quando MailBatch assente")
        void falseQuandoAssente() {
            when(configurazioneService.getMailBatch()).thenReturn(Optional.empty());

            assertFalse(mailService.isAbilitato());
        }
    }

    // ==================== inviaEmail - eccezioni ====================

    @Nested
    @DisplayName("inviaEmail - configurazione mancante o non abilitata")
    class InviaEmailConfigurazioneKo {

        @Test
        @DisplayName("lancia IllegalStateException se MailBatch assente")
        void eccezioneSeMailBatchAssente() {
            when(configurazioneService.getMailBatch()).thenReturn(Optional.empty());

            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();

            assertThrows(IllegalStateException.class, () -> mailService.inviaEmail(mailInfo));
        }

        @Test
        @DisplayName("lancia IllegalStateException se non abilitato")
        void eccezioneSeNonAbilitato() {
            MailBatch mb = new MailBatch();
            mb.setAbilitato(false);
            mb.setMailserver(defaultMailServer());
            when(configurazioneService.getMailBatch()).thenReturn(Optional.of(mb));

            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> mailService.inviaEmail(mailInfo));
            assertTrue(ex.getMessage().contains("abilitato"));
        }
    }

    // ==================== inviaEmail - invio semplice ====================

    @Nested
    @DisplayName("inviaEmail - invio semplice")
    class InviaEmailSemplice {

        @BeforeEach
        void configuraMock() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());
        }

        @Test
        @DisplayName("invia messaggio plain text senza errori")
        void invioSenzaErrori() {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Test oggetto")
                    .testo("Corpo del messaggio")
                    .build();

            assertDoesNotThrow(() -> mailService.inviaEmail(mailInfo));
            verify(mockMailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("usa from dal MailServer se non specificato in MailInfo")
        void fromDaMailServer() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Soggetto")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            assertEquals("noreply@test.local", captor.getValue().getFrom()[0].toString());
        }

        @Test
        @DisplayName("usa from override da MailInfo se specificato")
        void fromOverrideDaMailInfo() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .from("custom@test.local")
                    .oggetto("Soggetto")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            assertEquals("custom@test.local", captor.getValue().getFrom()[0].toString());
        }
    }

    // ==================== inviaEmail - HTML ====================

    @Nested
    @DisplayName("inviaEmail - HTML body")
    class InviaEmailHtml {

        @BeforeEach
        void configuraMock() {
            // lenient: alcuni test non chiamano inviaEmail e non usano questi stub
            lenient().when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            lenient().when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());
        }

        @Test
        @DisplayName("body HTML imposta content-type text/html")
        void htmlContentType() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("HTML email")
                    .testo("<h1>Ciao</h1><p>Messaggio HTML</p>")
                    .html(true)
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            // saveChanges() risolve i Content-Type header dal DataHandler
            MimeMessage msg = captor.getValue();
            msg.saveChanges();
            assertTrue(msg.getContentType().toLowerCase().contains("text/html"),
                    "Content-Type atteso text/html, ottenuto: " + msg.getContentType());
        }

        @Test
        @DisplayName("body plain text (default) imposta content-type text/plain")
        void plainTextContentType() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Plain email")
                    .testo("Testo semplice")
                    .build();  // html=false di default

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            MimeMessage msg = captor.getValue();
            msg.saveChanges();
            assertTrue(msg.getContentType().toLowerCase().contains("text/plain"),
                    "Content-Type atteso text/plain, ottenuto: " + msg.getContentType());
        }

        @Test
        @DisplayName("html=false e' il default del builder")
        void htmlDefaultFalse() {
            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();
            assertFalse(mailInfo.isHtml());
        }
    }

    // ==================== inviaEmail - encoding ====================

    @Nested
    @DisplayName("inviaEmail - encoding")
    class InviaEmailEncoding {

        @BeforeEach
        void configuraMock() {
            // lenient: alcuni test non chiamano inviaEmail e non usano questi stub
            lenient().when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            lenient().when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());
        }

        @Test
        @DisplayName("encoding default e' UTF-8")
        void encodingDefaultUtf8() {
            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();
            assertEquals(StandardCharsets.UTF_8.name(), mailInfo.getEncoding());
        }

        @Test
        @DisplayName("encoding ISO-8859-1 viene usato nel messaggio")
        void encodingIso() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Test encoding")
                    .testo("Testo in ISO")
                    .encoding("ISO-8859-1")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            MimeMessage msg = captor.getValue();
            msg.saveChanges();
            assertTrue(msg.getContentType().toLowerCase().contains("iso-8859-1"),
                    "Content-Type atteso con charset ISO-8859-1, ottenuto: " + msg.getContentType());
        }
    }

    // ==================== inviaEmail - header custom ====================

    @Nested
    @DisplayName("inviaEmail - header custom")
    class InviaEmailHeaderCustom {

        @BeforeEach
        void configuraMock() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());
        }

        @Test
        @DisplayName("imposta User-Agent se specificato")
        void userAgent() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .userAgent("GovPay/1.1.0")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            String[] headers = captor.getValue().getHeader("User-Agent");
            assertNotNull(headers);
            assertEquals("GovPay/1.1.0", headers[0]);
        }

        @Test
        @DisplayName("imposta Content-Language se specificato")
        void contentLanguage() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .contentLanguage("it-IT")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            String[] headers = captor.getValue().getHeader("Content-Language");
            assertNotNull(headers);
            assertEquals("it-IT", headers[0]);
        }

        @Test
        @DisplayName("imposta Message-ID con dominio se specificato")
        void messageIdDomain() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .messageIdDomain("link.it")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            String[] headers = captor.getValue().getHeader("Message-ID");
            assertNotNull(headers);
            assertTrue(headers[0].endsWith("@link.it>"),
                    "Message-ID atteso con @link.it, ottenuto: " + headers[0]);
            assertTrue(headers[0].startsWith("<"),
                    "Message-ID deve iniziare con <, ottenuto: " + headers[0]);
        }

        @Test
        @DisplayName("non imposta header se non specificati")
        void nessunHeaderSeNonSpecificato() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Nessun header")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            assertNull(captor.getValue().getHeader("User-Agent"));
            assertNull(captor.getValue().getHeader("Content-Language"));
        }
    }

    // ==================== inviaEmail - CC e BCC ====================

    @Nested
    @DisplayName("inviaEmail - CC e BCC")
    class InviaEmailCcBcc {

        @BeforeEach
        void configuraMock() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());
        }

        @Test
        @DisplayName("imposta CC correttamente")
        void impostaCc() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .cc(List.of("cc1@test.local", "cc2@test.local"))
                    .oggetto("Con CC")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            assertNotNull(captor.getValue().getRecipients(jakarta.mail.Message.RecipientType.CC));
            assertEquals(2, captor.getValue().getRecipients(jakarta.mail.Message.RecipientType.CC).length);
        }

        @Test
        @DisplayName("imposta BCC correttamente")
        void impostaBcc() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .bcc(List.of("bcc@test.local"))
                    .oggetto("Con BCC")
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            assertNotNull(captor.getValue().getRecipients(jakarta.mail.Message.RecipientType.BCC));
            assertEquals(1, captor.getValue().getRecipients(jakarta.mail.Message.RecipientType.BCC).length);
        }
    }

    // ==================== inviaEmail - allegati ====================

    @Nested
    @DisplayName("inviaEmail - con allegati")
    class InviaEmailConAllegati {

        @BeforeEach
        void configuraMock() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());
        }

        @Test
        @DisplayName("invia con allegato binario senza errori")
        void invioConAllegato() {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Con allegato")
                    .testo("Vedi allegato")
                    .allegati(Map.of("documento.pdf", "contenuto pdf".getBytes()))
                    .build();

            assertDoesNotThrow(() -> mailService.inviaEmail(mailInfo));
            verify(mockMailSender).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("messaggio con allegati e' multipart")
        void messaggioMultipart() throws Exception {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Multipart")
                    .testo("Corpo")
                    .allegati(Map.of("file.txt", "contenuto".getBytes()))
                    .build();

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
            mailService.inviaEmail(mailInfo);
            verify(mockMailSender).send(captor.capture());

            assertInstanceOf(MimeMultipart.class, captor.getValue().getContent());
        }

        @Test
        @DisplayName("invia con piu' allegati")
        void invioConPiuAllegati() {
            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Multi allegati")
                    .allegati(Map.of(
                            "file1.txt", "contenuto 1".getBytes(),
                            "file2.txt", "contenuto 2".getBytes()))
                    .build();

            assertDoesNotThrow(() -> mailService.inviaEmail(mailInfo));
            verify(mockMailSender).send(any(MimeMessage.class));
        }
    }

    // ==================== inviaEmailAsync ====================

    @Nested
    @DisplayName("inviaEmailAsync")
    class InviaEmailAsync {

        private final Executor syncExecutor = Runnable::run;

        @Test
        @DisplayName("ritorna CompletableFuture completato con successo")
        void completatoConSuccesso() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());

            MailInfo mailInfo = MailInfo.builder()
                    .to(List.of("dest@test.local"))
                    .oggetto("Async test")
                    .build();

            CompletableFuture<Void> future = mailService.inviaEmailAsync(mailInfo, syncExecutor);

            assertNotNull(future);
            assertTrue(future.isDone());
            assertFalse(future.isCompletedExceptionally());
        }

        @Test
        @DisplayName("errore non propagato al chiamante in modalita' asincrona")
        void erroreNonPropagato() {
            when(configurazioneService.getMailBatch()).thenReturn(Optional.empty());

            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();

            CompletableFuture<Void> future = mailService.inviaEmailAsync(mailInfo, syncExecutor);

            assertNotNull(future);
            assertTrue(future.isDone());
            assertFalse(future.isCompletedExceptionally());
        }
    }

    // ==================== Cache MailBatch ====================

    @Nested
    @DisplayName("Cache MailBatch")
    class CacheMailBatch {

        @Test
        @DisplayName("seconda inviaEmail entro TTL non rilegge la configurazione dal DB")
        void cacheHitNonInterrogaDb() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            lenient().when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());

            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();
            mailService.inviaEmail(mailInfo);
            mailService.inviaEmail(mailInfo);

            verify(configurazioneService, times(1)).getMailBatch();
        }

        @Test
        @DisplayName("resetCache forza la rilettura dal DB alla chiamata successiva")
        void resetCacheForceDbRead() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            lenient().when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());

            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();
            mailService.inviaEmail(mailInfo);
            mailService.resetCache();
            mailService.inviaEmail(mailInfo);

            verify(configurazioneService, times(2)).getMailBatch();
        }

        @Test
        @DisplayName("cache scaduta forza la rilettura dal DB")
        void cacheScadutaForceDbRead() {
            long[] fakeTime = {0L};
            TestMailService service = new TestMailService(configurazioneService, mockMailSender);
            service.clock = () -> fakeTime[0];

            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));
            lenient().when(mockMailSender.createMimeMessage()).thenReturn(newMimeMessage());

            MailInfo mailInfo = MailInfo.builder().to(List.of("dest@test.local")).build();
            service.inviaEmail(mailInfo);                                    // cache at t=0
            fakeTime[0] = AbstractMailService.DEFAULT_CACHE_TTL_MS + 1;     // advance past TTL
            service.inviaEmail(mailInfo);                                    // cache expired

            verify(configurazioneService, times(2)).getMailBatch();
        }

        @Test
        @DisplayName("isAbilitato usa la cache")
        void isAbilitatoUsaCache() {
            when(configurazioneService.getMailBatch())
                    .thenReturn(Optional.of(abilitato(defaultMailServer())));

            mailService.isAbilitato();
            mailService.isAbilitato();

            verify(configurazioneService, times(1)).getMailBatch();
        }

        @Test
        @DisplayName("DEFAULT_CACHE_TTL_MS e' 60 secondi")
        void defaultTtl() {
            assertEquals(60_000L, AbstractMailService.DEFAULT_CACHE_TTL_MS);
        }
    }

    // ==================== buildMailSender ====================

    @Nested
    @DisplayName("buildMailSender")
    class BuildMailSender {

        private AbstractMailService realService;

        @BeforeEach
        void setUp() {
            realService = new AbstractMailService(configurazioneService) {};
        }

        @Test
        @DisplayName("configura host e porta")
        void configuraHostEPorta() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(587);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertEquals("smtp.example.com", impl.getHost());
            assertEquals(587, impl.getPort());
        }

        @Test
        @DisplayName("configura username e password")
        void configuraCredenziali() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(25);
            ms.setUsername("user@example.com");
            ms.setPassword("secret");

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertEquals("user@example.com", impl.getUsername());
            assertEquals("secret", impl.getPassword());
        }

        @Test
        @DisplayName("abilita STARTTLS se configurato")
        void abilitaStartTls() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(587);
            ms.setStartTls(true);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertEquals("true", impl.getJavaMailProperties().getProperty("mail.smtp.starttls.enable"));
        }

        @Test
        @DisplayName("abilita SSL e imposta checkserveridentity=true se hostnameVerifier=true")
        void sslConHostnameVerifier() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(465);
            SslConfig ssl = new SslConfig();
            ssl.setAbilitato(true);
            ssl.setHostnameVerifier(true);
            ms.setSslConfig(ssl);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertEquals("true", impl.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
            assertEquals("true", impl.getJavaMailProperties().getProperty("mail.smtp.ssl.checkserveridentity"));
        }

        @Test
        @DisplayName("abilita SSL e imposta checkserveridentity=false se hostnameVerifier=false")
        void sslSenzaHostnameVerifier() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(465);
            SslConfig ssl = new SslConfig();
            ssl.setAbilitato(true);
            ssl.setHostnameVerifier(false);
            ms.setSslConfig(ssl);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertEquals("true", impl.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
            assertEquals("false", impl.getJavaMailProperties().getProperty("mail.smtp.ssl.checkserveridentity"));
        }

        @Test
        @DisplayName("SSL con solo abilitato=true non imposta socketFactory")
        void sslSenzaKeystoreNonImpostaSocketFactory() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(465);
            SslConfig ssl = new SslConfig();
            ssl.setAbilitato(true);
            ms.setSslConfig(ssl);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertNull(impl.getJavaMailProperties().get("mail.smtp.ssl.socketFactory"));
        }

        @Test
        @DisplayName("SSL con truststore imposta socketFactory")
        void sslConTruststoreImpostaSocketFactory(@TempDir File tempDir) throws Exception {
            // Crea un truststore JKS minimale
            KeyStore ts = KeyStore.getInstance("JKS");
            ts.load(null, null);
            File tsFile = new File(tempDir, "truststore.jks");
            try (FileOutputStream fos = new FileOutputStream(tsFile)) {
                ts.store(fos, "changeit".toCharArray());
            }

            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(465);
            SslConfig ssl = new SslConfig();
            ssl.setAbilitato(true);
            it.govpay.common.configurazione.model.KeyStore tsConfig =
                    new it.govpay.common.configurazione.model.KeyStore();
            tsConfig.setLocation(tsFile.getAbsolutePath());
            tsConfig.setPassword("changeit");
            tsConfig.setType("JKS");
            ssl.setTrustStore(tsConfig);
            ms.setSslConfig(ssl);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertNotNull(impl.getJavaMailProperties().get("mail.smtp.ssl.socketFactory"),
                    "SSLSocketFactory deve essere impostata quando truststore e' configurato");
        }

        @Test
        @DisplayName("SSL con keystore PKCS12 imposta socketFactory")
        void sslConKeystoreImpostaSocketFactory(@TempDir File tempDir) throws Exception {
            // Crea un keystore PKCS12 minimale
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            File ksFile = new File(tempDir, "keystore.p12");
            try (FileOutputStream fos = new FileOutputStream(ksFile)) {
                ks.store(fos, "changeit".toCharArray());
            }

            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(465);
            SslConfig ssl = new SslConfig();
            ssl.setAbilitato(true);
            it.govpay.common.configurazione.model.KeyStore ksConfig =
                    new it.govpay.common.configurazione.model.KeyStore();
            ksConfig.setLocation(ksFile.getAbsolutePath());
            ksConfig.setPassword("changeit");
            ksConfig.setType("PKCS12");
            ssl.setKeyStore(ksConfig);
            ms.setSslConfig(ssl);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertNotNull(impl.getJavaMailProperties().get("mail.smtp.ssl.socketFactory"),
                    "SSLSocketFactory deve essere impostata quando keystore e' configurato");
        }

        @Test
        @DisplayName("configura timeout se presenti")
        void configuraTimeout() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(25);
            ms.setReadTimeout(5000);
            ms.setConnectionTimeout(3000);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertEquals("5000", impl.getJavaMailProperties().getProperty("mail.smtp.timeout"));
            assertEquals("3000", impl.getJavaMailProperties().getProperty("mail.smtp.connectiontimeout"));
        }

        @Test
        @DisplayName("non configura SSL se SslConfig disabilitato")
        void nonConfiguraSslSeDisabilitato() {
            MailServer ms = new MailServer();
            ms.setHost("smtp.example.com");
            ms.setPort(25);
            SslConfig ssl = new SslConfig();
            ssl.setAbilitato(false);
            ms.setSslConfig(ssl);

            JavaMailSenderImpl impl = (JavaMailSenderImpl) realService.buildMailSender(ms);

            assertNull(impl.getJavaMailProperties().getProperty("mail.smtp.ssl.enable"));
        }
    }
}
