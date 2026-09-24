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
package it.govpay.common.logging;

import org.springframework.core.task.TaskDecorator;

/**
 * {@link TaskDecorator} che propaga l'MDC del thread chiamante al thread del pool.
 *
 * <p>Senza questa decorazione un task sottomesso a un {@code ThreadPoolTaskExecutor}
 * perde transaction id e correlation id: i log del task risulterebbero scollegati
 * dalla richiesta che lo ha originato e il correlation id non verrebbe propagato
 * alle chiamate HTTP eseguite dal task.
 *
 * <p>Il contesto del thread esecutore viene ripristinato al termine del task, cosi'
 * che il thread riusato dal pool non trascini identificativi di un'esecuzione
 * precedente.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        return TransactionContext.wrap(runnable);
    }
}
