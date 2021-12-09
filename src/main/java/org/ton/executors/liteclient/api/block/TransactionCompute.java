package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

@Builder
@ToString
@Getter
public class TransactionCompute implements Serializable {
    BigDecimal gasFees;
    BigDecimal gasUsed;
    BigDecimal gasLimit;
    BigDecimal gasCredit;
    Byte computeType;
    Byte skippedReason;
    String vmInitStateHash;
    String vmFinalStateHash;
    Byte accountActivated;
    Byte msgStateUsed;
    Byte success;
    String exitArg;
    BigInteger exitCode;
    BigInteger vmSteps;
    BigInteger mode;
}
