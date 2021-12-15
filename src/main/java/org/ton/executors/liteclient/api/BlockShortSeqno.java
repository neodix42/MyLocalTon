package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@ToString
@Getter
@Setter
public class BlockShortSeqno {
    private Long wc; // -1
    private String shard; // 8000000000000000 in hex, does not fit to Long
    private String seqno; // 1432551
}
