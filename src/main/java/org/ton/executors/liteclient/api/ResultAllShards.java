package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ResultAllShards implements Serializable {
    private Long wc;
    private String shard;
    private BigInteger seqno;
    private ResultLastBlock resultLastBlock;
    private long createdAt;
    private BigInteger startLt;
    private BigInteger endLt;
    private String rootHash;
    private String fileHash;
}
