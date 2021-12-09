package org.ton.db.entities;

import lombok.Builder;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@ToString
public class TxPk {
    Long createdAt;
    BigInteger seqno;
    Long wc;
    String shard;
    String txHash;
    String typeTx;
    String typeMsg;
    String accountAddress;
    BigInteger txLt;
}