package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@ToString
@Getter
public class TransactionCredit implements Serializable {
    BigDecimal dueFeesCollected;
    Value credit;
}
