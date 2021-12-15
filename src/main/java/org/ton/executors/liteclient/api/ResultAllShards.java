package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@ToString
@Getter
@Setter
public class ResultAllShards implements Serializable {
    private Long wc;
    private String shard;
    private String seqno;
    private ResultLastBlock resultLastBlock;
    private long createdAt;
    private String startLt;
    private String endLt;
    private String rootHash;
    private String fileHash;
}
