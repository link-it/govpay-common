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
package it.govpay.common.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sostituisce i segnaposto {@code %(chiave)} di un testo con i valori forniti dal chiamante.
 * Utility generica, senza alcuna conoscenza del significato dei segnaposto o del testo che li
 * contiene: la mappa dei valori e il testo sono entrambi parametri, non e' legata a IUV, avvisi
 * o ad alcun altro dominio applicativo specifico.
 */
public final class RisolutoreSegnaposto {

    private static final Pattern PLACEHOLDER = Pattern.compile("%\\(([a-zA-Z])\\)");

    private RisolutoreSegnaposto() {
        // Utility class - prevent instantiation
    }

    /**
     * @param testo  testo eventualmente con segnaposto {@code %(chiave)}; {@code null} trattato come vuoto
     * @param valori valori noti per le chiavi dei segnaposto presenti nel testo
     * @return il testo con ogni {@code %(chiave)} sostituito dal valore corrispondente
     * @throws IllegalArgumentException se il testo contiene un segnaposto per cui {@code valori} non ha un valore
     */
    public static String risolvi(String testo, Map<String, String> valori) {
        if (testo == null || testo.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(testo);
        StringBuilder risultato = new StringBuilder();
        while (matcher.find()) {
            String chiave = matcher.group(1);
            String valore = valori.get(chiave);
            if (valore == null) {
                throw new IllegalArgumentException(
                        "il testo [" + testo + "] contiene il segnaposto %(" + chiave
                                + ") per cui non e' stato fornito alcun valore");
            }
            matcher.appendReplacement(risultato, Matcher.quoteReplacement(valore));
        }
        matcher.appendTail(risultato);
        return risultato.toString();
    }
}
