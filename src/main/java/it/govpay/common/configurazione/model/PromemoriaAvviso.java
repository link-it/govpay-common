package it.govpay.common.configurazione.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PromemoriaAvviso extends PromemoriaAvvisoBase {

    private boolean allegaPdf;
}
