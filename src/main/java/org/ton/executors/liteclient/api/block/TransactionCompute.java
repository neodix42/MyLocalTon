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
    Long exitCode;
    Long vmSteps;
    Long mode;
}
