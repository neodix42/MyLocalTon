package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Builder
@ToString
@Getter
public class Leaf implements Serializable {
    String label;
    BigDecimal feesCollected; //import fees taken from the leaf, not from the upper edge (parent)
    Value valueImported; //import fees
    Message message; // actually can be two or more messages. exist "reimport:(msg_import_imm" followed by in_msg+tx combination
    List<Transaction> transactions;
}
