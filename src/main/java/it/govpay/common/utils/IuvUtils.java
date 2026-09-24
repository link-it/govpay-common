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

    /**
     * Coppia IUV/numero avviso (NAV) prodotta da {@link #genera}, o ricavata da
     * {@link #convertiDaNumeroAvviso}.
     *
     * @param iuv          Identificativo Univoco di Versamento
     * @param numeroAvviso NAV: identificativo dell'avviso di pagamento pagoPA
     */
    public record IdentificativiPagamento(String iuv, String numeroAvviso) {
    }

    /**
     * Genera un nuovo IUV e il relativo numero avviso (NAV) secondo il formato pagoPA
     * (aux digit 0/1/2/3, check digit mod-93), a partire da un progressivo già allocato dal
     * chiamante. Nessuno stato: la stessa combinazione di argomenti produce sempre lo stesso
     * risultato — l'unicità del progressivo è responsabilità di chi lo alloca.
     *
     * @param auxDigit        AuxDigit configurato sul dominio (0, 1, 2 o 3)
     * @param iuvPrefix       prefisso IUV del dominio, già risolto (nessun placeholder), o
     *                        {@code null}/vuoto se assente
     * @param segregationCode codice di segregazione del dominio, richiesto solo per AuxDigit 3
     * @param applicationCode application code della stazione, richiesto solo per AuxDigit 0
     * @param progressivo     valore progressivo da incorporare nell'IUV
     * @return la coppia IUV/numero avviso
     * @throws IllegalStateException se manca un dato di configurazione richiesto dall'AuxDigit
     *                                (application code per 0, codice di segregazione per 3), o
     *                                se il progressivo eccede le cifre disponibili
     */
    public static IdentificativiPagamento genera(int auxDigit, String iuvPrefix, Integer segregationCode,
            Integer applicationCode, long progressivo) {
        String prefix = iuvPrefix != null ? iuvPrefix : "";
        String iuv;
        String numeroAvviso;

        switch (auxDigit) {
            case 0 -> {
                if (applicationCode == null) {
                    throw new IllegalStateException(
                            "AuxDigit 0 richiede l'application code della stazione del dominio, assente");
                }
                String reference = costruisciReference(prefix, progressivo, 13);
                String check = checkDigit93(reference, auxDigit, applicationCode);
                iuv = reference + check;
                numeroAvviso = "0" + String.format("%02d", applicationCode) + iuv;
            }
            case 1, 2 -> {
                String reference = costruisciReference(prefix, progressivo, 15);
                String check = checkDigit93(reference, auxDigit);
                iuv = reference + check;
                numeroAvviso = auxDigit + iuv;
            }
            case 3 -> {
                if (segregationCode == null) {
                    throw new IllegalStateException(
                            "AuxDigit 3 richiede il codice di segregazione del dominio, assente");
                }
                String reference = costruisciReference(prefix, progressivo, 13);
                String check = checkDigit93(reference, auxDigit, segregationCode);
                iuv = String.format("%02d", segregationCode) + reference + check;
                numeroAvviso = "3" + iuv;
            }
            default -> throw new IllegalStateException("AuxDigit [" + auxDigit + "] non supportato per la generazione");
        }

        return new IdentificativiPagamento(iuv, numeroAvviso);
    }

    /**
     * Ricava e valida l'IUV da un numero avviso fornito dal chiamante: nessun progressivo
     * consumato, e' una decodifica di formato, non una generazione.
     *
     * @param numeroAvviso    numero avviso a 18 cifre fornito dal chiamante
     * @param auxDigit        AuxDigit configurato sul dominio
     * @param segregationCode codice di segregazione del dominio (per AuxDigit 3)
     * @param applicationCode application code della stazione (per AuxDigit 0)
     * @return l'IUV incorporato nel numero avviso
     * @throws IllegalArgumentException se il formato non e' un numero avviso valido, se
     *                                   l'AuxDigit non corrisponde alla configurazione del
     *                                   dominio, o se il check digit non torna
     */
    public static String convertiDaNumeroAvviso(String numeroAvviso, int auxDigit, Integer segregationCode,
            Integer applicationCode) {
        validaFormatoNumeroAvviso(numeroAvviso);

        int auxDigitEffettivo = Character.getNumericValue(numeroAvviso.charAt(0));
        if (auxDigitEffettivo != auxDigit) {
            throw new IllegalArgumentException(
                    "numeroAvviso [" + numeroAvviso + "] ha AuxDigit [" + auxDigitEffettivo
                            + "] ma il dominio e' configurato per AuxDigit [" + auxDigit + "]");
        }

        return switch (auxDigitEffettivo) {
            case 0 -> convertiAuxDigit0(numeroAvviso, applicationCode);
            case 1, 2 -> convertiAuxDigit1o2(numeroAvviso, auxDigitEffettivo);
            case 3 -> convertiAuxDigit3(numeroAvviso, segregationCode);
            default -> throw new IllegalArgumentException(
                    "numeroAvviso [" + numeroAvviso + "] ha AuxDigit [" + auxDigitEffettivo + "] non supportato");
        };
    }

    private static void validaFormatoNumeroAvviso(String numeroAvviso) {
        if (numeroAvviso == null || numeroAvviso.length() != 18 || !isNumeric(numeroAvviso)) {
            throw new IllegalArgumentException(
                    "numeroAvviso [" + numeroAvviso + "] non e' un identificativo pagoPA valido (18 cifre numeriche)");
        }
    }

    private static String convertiAuxDigit0(String numeroAvviso, Integer applicationCode) {
        if (applicationCode == null) {
            throw new IllegalStateException(
                    "AuxDigit 0 richiede l'application code della stazione del dominio, assente");
        }
        String applicationCodeAtteso = String.format("%02d", applicationCode);
        String applicationCodeNumeroAvviso = numeroAvviso.substring(1, 3);
        if (!applicationCodeAtteso.equals(applicationCodeNumeroAvviso)) {
            throw new IllegalArgumentException(
                    "numeroAvviso [" + numeroAvviso + "] ha application code [" + applicationCodeNumeroAvviso
                            + "] ma il dominio e' configurato per application code [" + applicationCodeAtteso + "]");
        }
        String iuv = numeroAvviso.substring(3);
        String reference = iuv.substring(0, iuv.length() - 2);
        verificaCheckDigit(numeroAvviso, iuv, reference, checkDigit93(reference, 0, applicationCode));
        return iuv;
    }

    private static String convertiAuxDigit1o2(String numeroAvviso, int auxDigit) {
        String iuv = numeroAvviso.substring(1);
        String reference = iuv.substring(0, iuv.length() - 2);
        verificaCheckDigit(numeroAvviso, iuv, reference, checkDigit93(reference, auxDigit));
        return iuv;
    }

    private static String convertiAuxDigit3(String numeroAvviso, Integer segregationCode) {
        String iuv = numeroAvviso.substring(1);
        if (iuv.length() < 5) {
            throw new IllegalArgumentException("numeroAvviso [" + numeroAvviso + "] troppo corto per AuxDigit 3");
        }
        String segregationCodeNumeroAvviso = iuv.substring(0, 2);
        if (segregationCode != null) {
            String segregationCodeAtteso = String.format("%02d", segregationCode);
            if (!segregationCodeAtteso.equals(segregationCodeNumeroAvviso)) {
                throw new IllegalArgumentException(
                        "numeroAvviso [" + numeroAvviso + "] ha codice di segregazione [" + segregationCodeNumeroAvviso
                                + "] ma il dominio e' configurato per codice di segregazione [" + segregationCodeAtteso
                                + "]");
            }
        }
        String reference = iuv.substring(2, iuv.length() - 2);
        int segregationCodeEffettivo = Integer.parseInt(segregationCodeNumeroAvviso);
        verificaCheckDigit(numeroAvviso, iuv, reference, checkDigit93(reference, 3, segregationCodeEffettivo));
        return iuv;
    }

    private static void verificaCheckDigit(String numeroAvviso, String iuv, String reference, String checkAtteso) {
        String check = iuv.substring(iuv.length() - 2);
        if (!check.equals(checkAtteso)) {
            throw new IllegalArgumentException(
                    "numeroAvviso [" + numeroAvviso + "] ha check digit [" + check + "] non coerente con il valore "
                            + "atteso [" + checkAtteso + "] per reference [" + reference + "]");
        }
    }

    private static String costruisciReference(String prefix, long progressivo, int lunghezzaTotale) {
        int cifreProgressivo = lunghezzaTotale - prefix.length();
        String reference = prefix + String.format("%0" + cifreProgressivo + "d", progressivo);
        if (reference.length() > lunghezzaTotale) {
            throw new IllegalStateException(
                    "superato il numero massimo di IUV generabili con prefisso [" + prefix + "]");
        }
        return reference;
    }

    private static String checkDigit93(String reference, int auxDigit, int code) {
        long resto93 = Long.parseLong(auxDigit + String.format("%02d", code) + reference) % 93;
        return String.format("%02d", resto93);
    }

    private static String checkDigit93(String reference, int auxDigit) {
        long resto93 = Long.parseLong(auxDigit + reference) % 93;
        return String.format("%02d", resto93);
    }
}
