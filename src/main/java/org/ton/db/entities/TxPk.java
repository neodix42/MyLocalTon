package org.ton.db.entities;

import lombok.*;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TxPk {
    Long createdAt;
    String seqno;
    Long wc;
    String shard;
    String txHash;
    String typeTx;
    String typeMsg;
    String accountAddress;
    String txLt;
}