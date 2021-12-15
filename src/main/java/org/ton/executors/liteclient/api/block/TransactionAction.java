package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * The action phase occurs after a valid computation phase.
 */
@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionAction implements Serializable {
    Byte success;
    Byte valid;
    Byte noFunds;
    String statusChange;
    BigDecimal totalFwdFee;
    BigDecimal totalActionFee;
    Long resultArg;
    Long resultCode;
    Long totActions;
    Long specActions;
    Long skippedActions;
    Long msgsCreated;
    String totalMsgSizeCells;
    String totalMsgSizeBits;
    String actionListHash;

}
