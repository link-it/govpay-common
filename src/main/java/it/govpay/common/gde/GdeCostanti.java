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
package it.govpay.common.gde;

/**
 * Costanti per i nomi degli eventi GDE (Giornale degli Eventi).
 */
public final class GdeCostanti {

    private GdeCostanti() {}

    // ==================== Nomi Eventi API pagoPA ====================

    public static final String APIPAGOPA_TIPOEVENTO_PAAVERIFICARPT = "paaVerificaRPT";
    public static final String APIPAGOPA_TIPOEVENTO_PAAATTIVARPT = "paaAttivaRPT";
    public static final String APIPAGOPA_TIPOEVENTO_PAAINVIAESITOSTORNO = "paaInviaEsitoStorno";
    public static final String APIPAGOPA_TIPOEVENTO_PAAINVIARICHIESTAREVOCA = "paaInviaRichiestaRevoca";
    public static final String APIPAGOPA_TIPOEVENTO_PAAINVIART = "paaInviaRT";
    public static final String APIPAGOPA_TIPOEVENTO_PASENDRT = "paSendRT";
    public static final String APIPAGOPA_TIPOEVENTO_PAVERIFYPAYMENTNOTICE = "paVerifyPaymentNotice";
    public static final String APIPAGOPA_TIPOEVENTO_PAGETPAYMENT = "paGetPayment";

    public static final String APIPAGOPA_TIPOEVENTO_GETORGANIZATIONRECEIPTIUR = "getOrganizationReceiptIur";
    public static final String APIPAGOPA_TIPOEVENTO_GETORGANIZATIONRECEIPTIUVIUR = "getOrganizationReceiptIuvIur";
    public static final String APIPAGOPA_TIPOEVENTO_HEALTHCHECK = "healthCheck";

    public static final String APIPAGOPA_TIPOEVENTO_NODOINVIARPT = "nodoInviaRPT";
    public static final String APIPAGOPA_TIPOEVENTO_NODOINVIACARRELLORPT = "nodoInviaCarrelloRPT";
    public static final String APIPAGOPA_TIPOEVENTO_NODOCHIEDISTATORPT = "nodoChiediStatoRPT";
    public static final String APIPAGOPA_TIPOEVENTO_NODOCHIEDICOPIART = "nodoChiediCopiaRT";
    public static final String APIPAGOPA_TIPOEVENTO_NODOCHIEDILISTAPENDENTIRPT = "nodoChiediListaPendentiRPT";
    public static final String APIPAGOPA_TIPOEVENTO_NODOINVIARICHIESTASTORNO = "nodoInviaRichiestaStorno";
    public static final String APIPAGOPA_TIPOEVENTO_NODOINVIARISPOSTAREVOCA = "nodoInviaRispostaRevoca";
    public static final String APIPAGOPA_TIPOEVENTO_NODOCHIEDIELENCOFLUSSIRENDICONTAZIONE = "nodoChiediElencoFlussiRendicontazione";
    public static final String APIPAGOPA_TIPOEVENTO_NODOCHIEDIFLUSSORENDICONTAZIONE = "nodoChiediFlussoRendicontazione";

    public static final String APIPAGOPA_TIPOEVENTO_INVIAFLUSSORENDICONTAZIONE = "inviaFlussoRendicontazione";
    public static final String APIPAGOPA_TIPOEVENTO_INVIARPP = "inviaRpp";
    public static final String APIPAGOPA_TIPOEVENTO_INVIASINTESIFLUSSIRENDICONTAZIONE = "inviaSintesiFlussiRendicontazione";
    public static final String APIPAGOPA_TIPOEVENTO_INVIASINTESIPAGAMENTI = "inviaSintesiPagamenti";

    // ==================== Nomi Eventi API pagoPA GPD ====================

    public static final String APIPAGOPA_TIPOEVENTO_GPD_GETORGANIZATIONDEBTPOSITIONS = "getOrganizationDebtPositions";
    public static final String APIPAGOPA_TIPOEVENTO_GPD_CREATEPOSITION = "createPosition";
    public static final String APIPAGOPA_TIPOEVENTO_GPD_GETORGANIZATIONDEBTPOSITIONBYIUPD = "getOrganizationDebtPositionByIUPD";
    public static final String APIPAGOPA_TIPOEVENTO_GPD_UPDATEPOSITION = "updatePosition";
    public static final String APIPAGOPA_TIPOEVENTO_GPD_DELETEPOSITION = "deletePosition";
    public static final String APIPAGOPA_TIPOEVENTO_GPD_PUBLISHPOSITION = "publishPosition";
    public static final String APIPAGOPA_TIPOEVENTO_GPD_INVALIDATEPOSITION = "invalidatePosition";
    public static final String APIPAGOPA_TIPOEVENTO_GPD_HEALTHCHECK = "healthCheck";

    // ==================== Nomi Eventi API MyPivot ====================

    public static final String APIMYPIVOT_TIPOEVENTO_MYPIVOTINVIATRACCIATOEMAIL = "pivotInviaTracciatoEmail";
    public static final String APIMYPIVOT_TIPOEVENTO_MYPIVOTINVIATRACCIATOFILESYSTEM = "pivotInviaTracciatoFileSystem";
    public static final String APIMYPIVOT_TIPOEVENTO_PIVOTSILAUTORIZZAIMPORTFLUSSO = "pivotSILAutorizzaImportFlusso";
    public static final String APIMYPIVOT_TIPOEVENTO_PIVOTSILCHIEDISTATOIMPORTFLUSSO = "pivotSILChiediStatoImportFlusso";
    public static final String APIMYPIVOT_TIPOEVENTO_PIVOTSILINVIAFLUSSO = "pivotSILInviaFlusso";

    // ==================== Nomi Eventi API Secim ====================

    public static final String APISECIM_TIPOEVENTO_SECIMINVIATRACCIATOEMAIL = "secimInviaTracciatoEmail";
    public static final String APISECIM_TIPOEVENTO_SECIMINVIATRACCIATOFILESYSTEM = "secimInviaTracciatoFileSystem";

    // ==================== Nomi Eventi API GovPay ====================

    public static final String APIGOVPAY_TIPOEVENTO_GOVPAYINVIATRACCIATOEMAIL = "govpayInviaTracciatoEmail";
    public static final String APIGOVPAY_TIPOEVENTO_GOVPAYINVIATRACCIATOFILESYSTEM = "govpayInviaTracciatoFileSystem";
    public static final String APIGOVPAY_TIPOEVENTO_GOVPAYINVIATRACCIATOREST = "govpayInviaTracciatoRest";

    // ==================== Nomi Eventi API HyperSIC APKappa ====================

    public static final String APIHYPERSICAPKAPPA_TIPOEVENTO_HYPERSIC_APKINVIATRACCIATOEMAIL = "hyperSicAPKappaInviaTracciatoEmail";
    public static final String APIHYPERSICAPKAPPA_TIPOEVENTO_HYPERSIC_APKINVIATRACCIATOFILESYSTEM = "hyperSicAPKappaInviaTracciatoFileSystem";

    // ==================== Sottotipi Evento ====================

    public static final String APIPAGOPA_SOTTOTIPOEVENTO_FLUSSO_RENDICONTAZIONE_DUPLICATO = "FlussoRendicontazioneDuplicato";
    public static final String GOVPAY_TIPOEVENTO_GOVPAYPAGAMENTOESEGUITOSENZARPT = "govpayPagamentoEseguitoSenzaRPT";
    public static final String SOTTOTIPO_EVENTO_NOTA = "nota";
}
