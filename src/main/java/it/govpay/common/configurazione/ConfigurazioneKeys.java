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
package it.govpay.common.configurazione;

public final class ConfigurazioneKeys {

    private ConfigurazioneKeys() {}

    public static final String KEY_GIORNALE_EVENTI = "giornale_eventi";
    public static final String KEY_TRACCIATO_CSV = "tracciato_csv";
    public static final String KEY_HARDENING = "hardening";
    public static final String KEY_MAIL_BATCH = "mail_batch";
    public static final String KEY_APP_IO_BATCH = "app_io_batch";
    public static final String KEY_AVVISATURA_MAIL = "avvisatura_mail";
    public static final String KEY_AVVISATURA_APP_IO = "avvisatura_app_io";

    public static final String COD_CONNETTORE_GDE = "govpay_gde_api";
}
