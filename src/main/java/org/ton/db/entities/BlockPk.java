package org.ton.db.entities;

import lombok.Builder;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@ToString
public class BlockPk {
    Long createdAt;
    BigInteger seqno;
    Long wc;
    String shard;
}