package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ResultConfig17 implements Serializable {
    private BigInteger minStake;
    private BigInteger maxStake;
    private BigInteger minTotalStake;
    private BigInteger maxStakeFactor;
}

