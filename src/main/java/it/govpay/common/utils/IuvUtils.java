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

import java.math.BigInteger;

import it.govpay.common.entity.DominioEntity;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class per la validazione degli IUV (Identificativo Univoco Versamento).
 * <p>
 * Fornisce metodi per determinare se uno IUV e' stato generato internamente
 * da GovPay in base alle regole pagoPA e alla configurazione del dominio.
 * <p>
 * Le regole dipendono dall'AuxDigit configurato per il dominio:
 * <ul>
 *   <li>AuxDigit 0: EC monointermediato, IUV numerico di 15 cifre per pagamenti tipo 3</li>
 *   <li>AuxDigit 1: EC monointermediato, IUV numerico di 17 cifre per pagamenti tipo 3</li>
 *   <li>AuxDigit 3: EC plurintermediato, IUV con codice segregazione</li>
 * </ul>
 */
@Slf4j
public final class IuvUtils {

    private IuvUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Verifica se uno IUV e' stato generato internamente da GovPay.
     * <p>
     * La verifica si basa sul formato dello IUV e sulla configurazione
     * del dominio (AuxDigit e codice segregazione).
     *
     * @param codDominio       codice fiscale del dominio/EC
     * @param auxDigit         AuxDigit configurato (0, 1, o 3)
     * @param segregationCode  codice segregazione (solo per AuxDigit 3)
     * @param iuv              lo IUV da verificare
     * @return true se lo IUV e' interno, false altrimenti
     */
    public static boolean isIuvInterno(String codDominio, int auxDigit, Integer segregationCode, String iuv) {
        if (iuv == null || iuv.isEmpty()) {
            return false;
        }

        boolean isNumerico = isNumeric(iuv);

        log.debug("Dominio:{}, AuxDigit:{}, Codice segregazione:{}", codDominio, auxDigit, segregationCode);
        log.debug("IUV:{}, lunghezza:{} di tipo numerico: {}", iuv, iuv.length(), (isNumerico ? "SI" : "NO"));

        // AuxDigit 0: Ente monointermediato.
        // Per i pagamenti di tipo 1 e 2, se non ho trovato il pagamento e sono arrivato qui, posso assumere che non e' interno.
        // Per i pagamenti di tipo 3, e' mio se e' di 15 cifre.
        // Quindi controllo solo se e' numerico e di 15 cifre.
        if (auxDigit == 0 && isNumerico && iuv.length() == 15) {
            log.debug("AuxDigit 0 -> EC Monointermediato, iuv numerico di lunghezza 15: e' interno.");
            return true;
        }

        // AuxDigit 1: Ente monointermediato.
        // Per i pagamenti di tipo 1 e 2, se non ho trovato il pagamento e sono arrivato qui, posso assumere che non e' interno.
        // Per i pagamenti di tipo 3, e' mio se e' di 17 cifre.
        // Quindi controllo solo se e' numerico e di 17 cifre.
        if (auxDigit == 1 && isNumerico && iuv.length() == 17) {
            log.debug("AuxDigit 1 -> EC Monointermediato, iuv numerico di lunghezza 17: e' interno.");
            return true;
        }

        if (auxDigit == 3 && segregationCode != null) {
            // AuxDigit 3: Ente plurintermediato.
            //
            // Gli IUV generati da GovPay sono nelle forme:
            // RF <check digit (2n)><codice segregazione (2n)><codice alfanumerico (max 19)>
            // <codice segregazione (2n)><IUV base (max 13n)><IUV check digit (2n)>

            String segregationCodeStr = String.format("%02d", segregationCode);

            // Pagamenti tipo 1 e 2 operati da GovPay
            if (iuv.startsWith("RF") && iuv.length() >= 6 && iuv.substring(4, 6).equals(segregationCodeStr)) {
                log.debug("AuxDigit 3 -> EC Plurintermediato, iuv non numerico contenente il codice di segregazione: e' interno.");
                return true;
            }

            // Pagamenti tipo 3
            if (isNumerico && iuv.length() == 17 && iuv.startsWith(segregationCodeStr)) {
                log.debug("AuxDigit 3 -> EC Plurintermediato, iuv numerico di lunghezza 17, inizia con il codice di segregazione: e' interno.");
                return true;
            }
        }

        log.debug("IUV {} non interno.", iuv);
        return false;
    }

    /**
     * Verifica se uno IUV e' stato generato internamente da GovPay.
     * Versione che accetta un oggetto DominioInfo per i dati del dominio.
     *
     * @param dominio informazioni sul dominio (null se non censito)
     * @param iuv         lo IUV da verificare
     * @return true se lo IUV e' interno, false altrimenti
     */
    public static boolean isIuvInterno(DominioEntity dominio, String iuv) {
        if (dominio == null) {
            log.debug("Dominio non censito, IUV:{} non interno", iuv);
            return false;
        }

        return isIuvInterno(
                dominio.getCodDominio(),
                dominio.getAuxDigit(),
                dominio.getSegregationCode(),
                iuv);
    }

    /**
     * Verifica se una stringa e' composta solo da caratteri numerici.
     *
     * @param value la stringa da verificare
     * @return true se la stringa e' numerica, false altrimenti
     */
    public static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        try {
            new BigInteger(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
