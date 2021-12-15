package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Value implements Serializable {
    BigDecimal toncoins;
    private List<Currency> otherCurrencies;
}
