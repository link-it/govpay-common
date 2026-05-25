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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * DTO contenente le informazioni necessarie per inviare una email.
 * <p>
 * Copre tutte le funzionalita' della libreria GovWay openspcoop2-utils-mail:
 * body HTML/plain, encoding, header custom (User-Agent, Content-Language,
 * Message-ID) e allegati binari.
 */
@Data
@Builder
public class MailInfo {

    // ==================== Destinatari ====================

    /** Destinatari (obbligatorio) */
    private List<String> to;

    /** Destinatari in copia */
    private List<String> cc;

    /** Destinatari in copia nascosta */
    private List<String> bcc;

    /** Mittente override; se null usa il from configurato nel MailServer */
    private String from;

    // ==================== Contenuto ====================

    /** Oggetto del messaggio */
    private String oggetto;

    /** Corpo del messaggio */
    private String testo;

    /**
     * Indica se il corpo e' HTML ({@code true}) o plain text ({@code false}).
     * Default: {@code false} (plain text).
     */
    @Builder.Default
    private boolean html = false;

    /**
     * Charset per la codifica del messaggio.
     * Default: {@code UTF-8}.
     */
    @Builder.Default
    private String encoding = StandardCharsets.UTF_8.name();

    /** Allegati: filename → contenuto in byte */
    private Map<String, byte[]> allegati;

    // ==================== Header custom ====================

    /**
     * Valore dell'header {@code User-Agent}.
     * Corrisponde a {@code mail.setUserAgent()} in GovWay.
     */
    private String userAgent;

    /**
     * Valore dell'header {@code Content-Language} (es. {@code "it-IT"}).
     * Corrisponde a {@code mail.setContentLanguage()} in GovWay.
     */
    private String contentLanguage;

    /**
     * Dominio usato per generare il {@code Message-ID} univoco del messaggio
     * (es. {@code "link.it"} produce {@code <uuid@link.it>}).
     * Se null, il Message-ID non viene impostato esplicitamente.
     * Corrisponde a {@code mail.setMessageIdDomain()} in GovWay.
     */
    private String messageIdDomain;
}
