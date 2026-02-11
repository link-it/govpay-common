package it.govpay.common.configurazione.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppIOBatch {

    private boolean abilitato;
    private BigDecimal timeToLive;
    private String url;
}
