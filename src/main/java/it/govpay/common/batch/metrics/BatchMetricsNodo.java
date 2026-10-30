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
package it.govpay.common.batch.metrics;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import it.govpay.common.batch.config.BatchJobProperties;
import it.govpay.common.batch.config.ClusterIdPredefinito;

/**
 * Valore del tag comune {@value BatchMetricsAutoConfiguration#TAG_NODO} e
 * sorgente da cui e' stato ricavato.
 *
 * @param valore Identificativo del nodo
 * @param fonte Sorgente del valore, per i messaggi di log
 */
public record BatchMetricsNodo(String valore, String fonte) {

    /** Property con cui i progetti batch configurano il cluster-id */
    public static final String PROPERTY_CLUSTER_ID = "govpay.batch.cluster-id";

    /**
     * Ricava l'identificativo del nodo dalla prima sorgente valorizzata:
     * <ol>
     *   <li>{@code govpay.metrics.batch.nodo}, per i progetti che configurano il
     *       cluster-id con un'altra property;</li>
     *   <li>{@value #PROPERTY_CLUSTER_ID}, la property dei progetti batch;</li>
     *   <li>{@link BatchJobProperties#getClusterId()}, se c'e' un unico bean;</li>
     *   <li>il cluster-id predefinito del nodo ({@link ClusterIdPredefinito}).</li>
     * </ol>
     * Le prime due sono lette dall'{@link Environment} perche' i progetti
     * passano il cluster-id all'helper con {@code @Value}, senza un bean
     * {@link BatchJobProperties}.
     *
     * @param environment Ambiente Spring
     * @param properties Proprieta' delle metriche batch
     * @param batchJobProperties Proprieta' del batch, se c'e' un unico bean; puo' essere null
     * @return identificativo del nodo e sua sorgente
     */
    public static BatchMetricsNodo risolvi(Environment environment, BatchMetricsProperties properties,
            BatchJobProperties batchJobProperties) {
        if (StringUtils.hasText(properties.getNodo())) {
            return new BatchMetricsNodo(properties.getNodo().trim(), "govpay.metrics.batch.nodo");
        }
        String daProperty = environment.getProperty(PROPERTY_CLUSTER_ID);
        if (StringUtils.hasText(daProperty)) {
            return new BatchMetricsNodo(daProperty.trim(), PROPERTY_CLUSTER_ID);
        }
        if (batchJobProperties != null && StringUtils.hasText(batchJobProperties.getClusterId())) {
            return new BatchMetricsNodo(batchJobProperties.getClusterId().trim(), "BatchJobProperties");
        }
        return new BatchMetricsNodo(ClusterIdPredefinito.valore(), "identificativo dell'host");
    }
}
