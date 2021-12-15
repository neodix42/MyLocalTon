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
public class TransactionStorage implements Serializable {
    BigDecimal feesCollected;
    BigDecimal feesDue;
    String statusChange;
}
