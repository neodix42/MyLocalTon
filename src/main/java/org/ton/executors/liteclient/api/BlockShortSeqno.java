package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@ToString
public class BlockShortSeqno {
    private Long wc; // -1
    private String shard; // 8000000000000000 in hex, does not fit to Long
    private BigInteger seqno; // 1432551
}
