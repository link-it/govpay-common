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

/**
 * Esito del controllo che precede ogni tentativo di avvio di un job
 * (tick dello scheduler o richiesta manuale via REST).
 * <p>
 * I primi cinque valori corrispondono a quelli di
 * {@code JobExecutionHelper.PreExecutionCheckResult}; gli altri sono
 * prodotti solo dall'avvio manuale di {@code AbstractBatchController}.
 */
public enum EsitoTick {
    /** Nessuna esecuzione in corso: il job viene avviato */
    CAN_PROCEED,
    /** Esecuzione stale abbandonata: il job viene avviato */
    STALE_ABANDONED_CAN_PROCEED,
    /** Esecuzione stale che non e' stato possibile abbandonare: il job non parte */
    STALE_ABANDON_FAILED,
    /** Il job e' in esecuzione su un altro nodo: il job non parte */
    RUNNING_ON_OTHER_NODE,
    /** Il job e' in esecuzione su questo nodo: il job non parte */
    RUNNING_ON_THIS_NODE,
    /** Avvio manuale rifiutato perche' il job e' gia' in esecuzione (409) */
    RIFIUTATO_IN_ESECUZIONE,
    /** Avvio manuale con {@code force=true}: l'esecuzione corrente e' stata abbandonata */
    FORZATO,
    /** Avvio manuale con {@code force=true}: non e' stato possibile abbandonare l'esecuzione corrente */
    FORZATURA_FALLITA
}
