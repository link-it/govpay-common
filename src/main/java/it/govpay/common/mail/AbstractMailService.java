/*
 * GovPay - Porta di Accesso al Nodo dei Pagamenti SPC
 * http://www.gov4j.it/govpay
 *
 * Copyright (c) 2014-2025 Link.it srl (http://www.link.it).
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

    private final ConfigurazioneService configurazioneService;

    protected AbstractMailService(ConfigurazioneService configurazioneService) {
        this.configurazioneService = configurazioneService;
    }

    /**
     * Verifica se il servizio di invio mail e' abilitato.
     *
     * @return true se la configurazione MailBatch esiste ed e' abilitata
     */
    public boolean isAbilitato() {
        return configurazioneService.getMailBatch()
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
        MailBatch mailBatch = configurazioneService.getMailBatch()
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
                for (Map.Entry<String, byte[]> entry : allegati.entrySet()) {
                    helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
                }
            }
        } catch (MessagingException e) {
            throw new MailSendException("Errore nella costruzione del messaggio email: " + e.getMessage(), e);
        }

        // Header custom impostati direttamente sul MimeMessage
        try {
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
        } catch (MessagingException e) {
            throw new MailSendException("Errore nell'impostazione degli header del messaggio: " + e.getMessage(), e);
        }

        mailSender.send(mimeMessage);
        log.info("Email inviata con successo a {}: oggetto={}", to, oggetto);
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
        mailSender.setPort(mailServer.getPort());

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

            KeyManager[] keyManagers = null;
            TrustManager[] trustManagers = null;

            it.govpay.common.configurazione.model.KeyStore ksConfig = sslConfig.getKeyStore();
            if (ksConfig != null && StringUtils.hasText(ksConfig.getLocation())) {
                String ksType = StringUtils.hasText(ksConfig.getType()) ? ksConfig.getType() : "PKCS12";
                java.security.KeyStore ks = java.security.KeyStore.getInstance(ksType);
                char[] ksPwd = ksConfig.getPassword() != null ? ksConfig.getPassword().toCharArray() : null;
                try (FileInputStream fis = new FileInputStream(ksConfig.getLocation())) {
                    ks.load(fis, ksPwd);
                }
                String algorithm = StringUtils.hasText(ksConfig.getManagementAlgorithm())
                        ? ksConfig.getManagementAlgorithm()
                        : KeyManagerFactory.getDefaultAlgorithm();
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
                kmf.init(ks, ksPwd);
                keyManagers = kmf.getKeyManagers();
                log.debug("SSL keystore caricato: {}", ksConfig.getLocation());
            }

            it.govpay.common.configurazione.model.KeyStore tsConfig = sslConfig.getTrustStore();
            if (tsConfig != null && StringUtils.hasText(tsConfig.getLocation())) {
                String tsType = StringUtils.hasText(tsConfig.getType()) ? tsConfig.getType() : "JKS";
                java.security.KeyStore ts = java.security.KeyStore.getInstance(tsType);
                char[] tsPwd = tsConfig.getPassword() != null ? tsConfig.getPassword().toCharArray() : null;
                try (FileInputStream fis = new FileInputStream(tsConfig.getLocation())) {
                    ts.load(fis, tsPwd);
                }
                String algorithm = StringUtils.hasText(tsConfig.getManagementAlgorithm())
                        ? tsConfig.getManagementAlgorithm()
                        : TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
                tmf.init(ts);
                trustManagers = tmf.getTrustManagers();
                log.debug("SSL truststore caricato: {}", tsConfig.getLocation());
            }

            sslContext.init(keyManagers, trustManagers, null);
            return sslContext;

        } catch (Exception e) {
            throw new MailSendException("Errore nella configurazione SSL: " + e.getMessage(), e);
        }
    }
}
