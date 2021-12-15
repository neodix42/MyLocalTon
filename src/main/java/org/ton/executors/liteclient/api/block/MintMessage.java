package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * InMsgDescr — The description of all messages “imported” into this
 * block (i.e., either processed by a transaction included in the block, or
 * forwarded to an output queue, in the case of a transit message travelling
 * along the path dictated by Hypercube Routing).
 */
@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MintMessage implements Serializable {
    Message inMsg;
    private List<Transaction> transactions;
}
