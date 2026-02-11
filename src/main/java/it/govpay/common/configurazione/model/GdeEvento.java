package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GdeEvento {

    public enum LogEnum { SEMPRE, MAI, SOLO_ERRORE }

    public enum DumpEnum { SEMPRE, MAI, SOLO_ERRORE }

    private LogEnum log;
    private DumpEnum dump;
}
