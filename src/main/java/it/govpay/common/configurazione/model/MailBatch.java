package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MailBatch {

    private boolean abilitato;
    private MailServer mailserver;
}
