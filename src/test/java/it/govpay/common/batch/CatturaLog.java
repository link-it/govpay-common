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
package it.govpay.common.batch;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Cattura nei test i messaggi di log di una classe, indipendentemente da dove
 * logback scrive la console: si aggancia direttamente al logger.
 * <p>
 * Per la durata della cattura il logger e' portato a {@code TRACE}: i test che
 * avviano un contesto Spring applicano {@code logging.level.it.govpay=OFF} da
 * {@code application.properties}, e il livello resta impostato per tutta la JVM.
 *
 * <pre>
 * try (CatturaLog log = CatturaLog.di(MiaClasse.class)) {
 *     ...
 *     assertTrue(log.contiene("messaggio atteso"));
 * }
 * </pre>
 */
public final class CatturaLog implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final Level livelloPrecedente;

    private CatturaLog(Class<?> classe) {
        this.logger = (Logger) LoggerFactory.getLogger(classe);
        this.livelloPrecedente = logger.getLevel();
        logger.setLevel(Level.TRACE);
        appender.start();
        logger.addAppender(appender);
    }

    public static CatturaLog di(Class<?> classe) {
        return new CatturaLog(classe);
    }

    /** @return true se un messaggio registrato contiene il testo */
    public boolean contiene(String testo) {
        return appender.list.stream().anyMatch(evento -> evento.getFormattedMessage().contains(testo));
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(livelloPrecedente);
    }
}
