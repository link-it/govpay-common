/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2026 Link.it srl (http://www.link.it).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3, as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.govpay.common.mail;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;

import it.govpay.common.configurazione.model.MailBatch;
import it.govpay.common.configurazione.model.MailServer;
import it.govpay.common.configurazione.model.SslConfig;
import it.govpay.common.configurazione.service.ConfigurazioneService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Classe base astratta per l'invio di email tramite la configurazione MailBatch.
 * <p>
 * Segue il pattern Template Method analogo ad {@code AbstractGdeService} e copre
 * tutte le funzionalita' della libreria GovWay {@code openspcoop2-utils-mail}:
 * <ul>
 *   <li>Body HTML o plain text con encoding configurabile</li>
 *   <li>Allegati binari (byte[])</li>
 *   <li>SSL/TLS con keystore e truststore personalizzati</li>
 *   <li>STARTTLS</li>
 *   <li>Header custom: User-Agent, Content-Language, Message-ID</li>
 *   <li>Invio sincrono e asincrono</li>
 * </ul>
 *
 * <p>La configurazione {@link MailBatch} viene cachata in memoria per evitare
 * query ripetute al database. Il TTL di default e' {@value #DEFAULT_CACHE_TTL_MS} ms
 * (60 secondi) e puo' essere personalizzato tramite il costruttore.
 * Il metodo {@link #resetCache()} invalida la cache immediatamente.
 *
 * <p>Esempio d'uso:
 * <pre>{@code
 * @Service
 * public class MyMailService extends AbstractMailService {
 *     public MyMailService(ConfigurazioneService configurazioneService) {
 *         super(configurazioneService);
 *     }
 * }
 * }</pre>
 */
@Slf4j
public abstract class AbstractMailService {

    /** TTL di default della cache di configurazione: 60 secondi. */
    public static final long DEFAULT_CACHE_TTL_MS = 60_000L;

    private final ConfigurazioneService configurazioneService;
    private final long cacheTtlMs;

    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    private record CacheEntry(MailBatch value, long timestamp) {}

    /** Orologio usato per il TTL della cache; sostituibile nei test per evitare Thread.sleep. */
    LongSupplier clock = System::currentTimeMillis;

    protected AbstractMailService(ConfigurazioneService configurazioneService) {
        this(configurazioneService, DEFAULT_CACHE_TTL_MS);
    }

    protected AbstractMailService(ConfigurazioneService configurazioneService, long cacheTtlMs) {
        this.configurazioneService = configurazioneService;
        this.cacheTtlMs = cacheTtlMs;
    }

    /**
     * Restituisce la configurazione {@link MailBatch} dalla cache, ricaricandola
     * dal database solo se assente o scaduta.
     * <p>
     * In caso di accessi concorrenti a cache scaduta, piu' thread potrebbero
     * eseguire la query contemporaneamente: e' accettabile perche' il risultato
     * e' idempotente e il costo e' trascurabile.
     *
     * @return configurazione MailBatch, o {@link java.util.Optional#empty()} se assente
     */
    private java.util.Optional<MailBatch> getMailBatchCached() {
        long now = clock.getAsLong();
        CacheEntry entry = cache.get();
        if (entry == null || (now - entry.timestamp()) > cacheTtlMs) {
            MailBatch fresh = configurazioneService.getMailBatch().orElse(null);
            cache.set(new CacheEntry(fresh, now));
            log.debug("Configurazione MailBatch caricata dal database");
            return java.util.Optional.ofNullable(fresh);
        }
        return java.util.Optional.ofNullable(entry.value());
    }

    /**
     * Invalida la cache della configurazione {@link MailBatch}.
     * <p>
     * La chiamata successiva a {@link #isAbilitato()} o {@link #inviaEmail(MailInfo)}
     * rileggera' la configurazione dal database.
     */
    public void resetCache() {
        cache.set(null);
        log.debug("Cache MailBatch invalidata");
    }

    /**
     * Verifica se il servizio di invio mail e' abilitato.
     *
     * @return true se la configurazione MailBatch esiste ed e' abilitata
     */
    public boolean isAbilitato() {
        return getMailBatchCached()
                .map(MailBatch::isAbilitato)
                .orElse(false);
    }

    /**
     * Invia una email in modo sincrono.
     *
     * @param mailInfo informazioni del messaggio da inviare
     * @throws MailException         se l'invio fallisce
     * @throws IllegalStateException se il servizio non e' abilitato o non configurato
     */
    public void inviaEmail(MailInfo mailInfo) throws MailException {
        MailBatch mailBatch = getMailBatchCached()
                .orElseThrow(() -> new IllegalStateException("Configurazione MailBatch non trovata"));

        if (!mailBatch.isAbilitato()) {
            throw new IllegalStateException("Il servizio di invio mail non e' abilitato");
        }

        MailServer mailServer = mailBatch.getMailserver();
        JavaMailSender mailSender = buildMailSender(mailServer);

        List<String> to = mailInfo.getTo();
        String oggetto = mailInfo.getOggetto();
        log.debug("Invio email a {}: oggetto={}", to, oggetto);

        Map<String, byte[]> allegati = mailInfo.getAllegati();
        boolean multipart = allegati != null && !allegati.isEmpty();
        String encoding = mailInfo.getEncoding();

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, multipart, encoding);

            String from = mailInfo.getFrom() != null ? mailInfo.getFrom() : mailServer.getFrom();
            if (from != null) {
                helper.setFrom(from);
            }

            if (to != null && !to.isEmpty()) {
                helper.setTo(to.toArray(new String[0]));
            }

            List<String> cc = mailInfo.getCc();
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }

            List<String> bcc = mailInfo.getBcc();
            if (bcc != null && !bcc.isEmpty()) {
                helper.setBcc(bcc.toArray(new String[0]));
            }

            if (oggetto != null) {
                helper.setSubject(oggetto);
            }

            if (mailInfo.getTesto() != null) {
                helper.setText(mailInfo.getTesto(), mailInfo.isHtml());
            }

            if (multipart) {
                addAttachments(helper, allegati);
            }

            applyCustomHeaders(mimeMessage, mailInfo);
        } catch (MessagingException e) {
            throw new MailSendException("Errore nella costruzione del messaggio email: " + e.getMessage(), e);
        }

        mailSender.send(mimeMessage);
        log.info("Email inviata con successo a {}: oggetto={}", to, oggetto);
    }

    private void addAttachments(MimeMessageHelper helper, Map<String, byte[]> allegati)
            throws MessagingException {
        for (Map.Entry<String, byte[]> entry : allegati.entrySet()) {
            helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
        }
    }

    private void applyCustomHeaders(MimeMessage mimeMessage, MailInfo mailInfo)
            throws MessagingException {
        if (StringUtils.hasText(mailInfo.getUserAgent())) {
            mimeMessage.setHeader("User-Agent", mailInfo.getUserAgent());
        }
        if (StringUtils.hasText(mailInfo.getContentLanguage())) {
            mimeMessage.setHeader("Content-Language", mailInfo.getContentLanguage());
        }
        if (StringUtils.hasText(mailInfo.getMessageIdDomain())) {
            mimeMessage.setHeader("Message-ID",
                    "<" + UUID.randomUUID() + "@" + mailInfo.getMessageIdDomain() + ">");
        }
    }

    /**
     * Invia una email in modo asincrono (non bloccante).
     * <p>
     * Gli errori vengono loggati ma non propagati al chiamante.
     *
     * @param mailInfo informazioni del messaggio da inviare
     * @param executor Executor per l'esecuzione asincrona
     * @return {@code CompletableFuture<Void>} che completa al termine dell'invio
     */
    public CompletableFuture<Void> inviaEmailAsync(MailInfo mailInfo, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                inviaEmail(mailInfo);
            } catch (Exception e) {
                log.error("Errore invio email asincrono a {}: {}", mailInfo.getTo(), e.getMessage(), e);
            }
        }, executor);
    }

    /**
     * Crea e configura un {@link JavaMailSender} in base alle impostazioni SMTP.
     * <p>
     * Configura:
     * <ul>
     *   <li>Host, porta, credenziali</li>
     *   <li>STARTTLS ({@code mail.smtp.starttls.enable})</li>
     *   <li>SSL/TLS con eventuale keystore e truststore personalizzati</li>
     *   <li>Timeout di connessione e lettura</li>
     * </ul>
     * Il metodo e' {@code protected} per consentire l'override nei test.
     *
     * @param mailServer configurazione del server SMTP
     * @return JavaMailSender configurato
     * @throws MailException se la configurazione SSL non e' valida
     */
    protected JavaMailSender buildMailSender(MailServer mailServer) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailServer.getHost());
        mailSender.setPort(mailServer.getPort() != null ? mailServer.getPort() : JavaMailSenderImpl.DEFAULT_PORT);

        if (mailServer.getUsername() != null) {
            mailSender.setUsername(mailServer.getUsername());
        }
        if (mailServer.getPassword() != null) {
            mailSender.setPassword(mailServer.getPassword());
        }

        Properties props = mailSender.getJavaMailProperties();

        if (mailServer.isStartTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        }

        SslConfig sslConfig = mailServer.getSslConfig();
        if (sslConfig != null && sslConfig.isAbilitato()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.checkserveridentity", String.valueOf(sslConfig.isHostnameVerifier()));

            boolean hasKeyStore = sslConfig.getKeyStore() != null
                    && StringUtils.hasText(sslConfig.getKeyStore().getLocation());
            boolean hasTrustStore = sslConfig.getTrustStore() != null
                    && StringUtils.hasText(sslConfig.getTrustStore().getLocation());

            if (hasKeyStore || hasTrustStore) {
                SSLContext sslContext = buildSslContext(sslConfig);
                props.put("mail.smtp.ssl.socketFactory", sslContext.getSocketFactory());
            }
        }

        if (mailServer.getReadTimeout() != null) {
            props.put("mail.smtp.timeout", String.valueOf(mailServer.getReadTimeout()));
        }

        if (mailServer.getConnectionTimeout() != null) {
            props.put("mail.smtp.connectiontimeout", String.valueOf(mailServer.getConnectionTimeout()));
        }

        return mailSender;
    }

    /**
     * Costruisce un {@link SSLContext} a partire dalla configurazione {@link SslConfig}.
     * <p>
     * Carica keystore e/o truststore se configurati, altrimenti usa i default JVM.
     *
     * @param sslConfig configurazione SSL
     * @return SSLContext configurato
     * @throws MailSendException se il caricamento del keystore/truststore fallisce
     */
    private SSLContext buildSslContext(SslConfig sslConfig) {
        try {
            String protocol = StringUtils.hasText(sslConfig.getType()) ? sslConfig.getType() : "TLS";
            SSLContext sslContext = SSLContext.getInstance(protocol);
            KeyManager[] loadKeyManagers = sslConfig.getKeyStore() != null ? loadKeyManagers(sslConfig.getKeyStore()) : null;
            TrustManager[] loadTrustManagers = sslConfig.getTrustStore() != null ? loadTrustManagers(sslConfig.getTrustStore()) : null;
            
            sslContext.init(loadKeyManagers, loadTrustManagers, null);
            return sslContext;
        } catch (Exception e) {
            throw new MailSendException("Errore nella configurazione SSL: " + e.getMessage(), e);
        }
    }

    private KeyManager[] loadKeyManagers(it.govpay.common.configurazione.model.KeyStore ksConfig)
            throws Exception {
        String type = StringUtils.hasText(ksConfig.getType()) ? ksConfig.getType() : "PKCS12";
        char[] pwd = ksConfig.getPassword() != null ? ksConfig.getPassword().toCharArray() : null;
        java.security.KeyStore ks = java.security.KeyStore.getInstance(type);
        try (FileInputStream fis = new FileInputStream(ksConfig.getLocation())) {
            ks.load(fis, pwd);
        }
        String algorithm = StringUtils.hasText(ksConfig.getManagementAlgorithm())
                ? ksConfig.getManagementAlgorithm()
                : KeyManagerFactory.getDefaultAlgorithm();
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, pwd);
        log.debug("SSL keystore caricato: {}", ksConfig.getLocation());
        return kmf.getKeyManagers();
    }

    private TrustManager[] loadTrustManagers(it.govpay.common.configurazione.model.KeyStore tsConfig)
            throws Exception {
        String type = StringUtils.hasText(tsConfig.getType()) ? tsConfig.getType() : "JKS";
        char[] pwd = tsConfig.getPassword() != null ? tsConfig.getPassword().toCharArray() : null;
        java.security.KeyStore ts = java.security.KeyStore.getInstance(type);
        try (FileInputStream fis = new FileInputStream(tsConfig.getLocation())) {
            ts.load(fis, pwd);
        }
        String algorithm = StringUtils.hasText(tsConfig.getManagementAlgorithm())
                ? tsConfig.getManagementAlgorithm()
                : TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        tmf.init(ts);
        log.debug("SSL truststore caricato: {}", tsConfig.getLocation());
        return tmf.getTrustManagers();
    }
}
