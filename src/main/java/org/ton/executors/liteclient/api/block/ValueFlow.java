package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * basically shows the flow of toincoins (value) within a block
 */
@Builder
@ToString
@Getter
public class ValueFlow implements Serializable {
    Value prevBlock;
    Value nextBlock;
    Value feesImported; // Amount of import fees in non gram currencies.
    Value feesCollected;
    Value created;
    Value exported; // Amount of non gram crypto-currencies exported.
    Value imported; // Amount of non gram crypto-currencies imported.
    Value minted;
    Value recovered;
}
