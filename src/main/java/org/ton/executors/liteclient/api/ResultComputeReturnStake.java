package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Getter
@ToString
public class ResultComputeReturnStake implements Serializable {
    private BigDecimal stake;
}
