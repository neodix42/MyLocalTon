package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The action phase occurs after a valid computation phase.
 */
@Builder
@ToString
@Getter
public class TransactionAction implements Serializable {
    Byte success;
    Byte valid;
    Byte noFunds;
    String statusChange;
    BigDecimal totalFwdFee;
    BigDecimal totalActionFee;
    BigInteger resultArg;
    BigInteger resultCode;
    BigInteger totActions;
    BigInteger specActions;
    BigInteger skippedActions;
    BigInteger msgsCreated;
    BigInteger totalMsgSizeCells;
    BigInteger totalMsgSizeBits;
    String actionListHash;

}
