package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Currency implements Serializable {
    Byte len;
    String label;
    BigDecimal value;
}
