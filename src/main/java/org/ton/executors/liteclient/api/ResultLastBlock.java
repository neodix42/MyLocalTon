package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ResultLastBlock implements Serializable {
    //private String fullBlockSeqno; //(-1,8000000000000000,1432551):8128C13B9E81D86A261AD4ECA74F7C831822697A6EFE442C5491539A412AF295:8DD4D47161D9A7296BB3906BE8F1D6F0B827EE8C9CD5EB10F697853A23893376
    private String rootHash; // 8128C13B9E81D86A261AD4ECA74F7C831822697A6EFE442C5491539A412AF295
    private String fileHash; // 8DD4D47161D9A7296BB3906BE8F1D6F0B827EE8C9CD5EB10F697853A23893376
    private Long wc; // -1
    private String shard; // 8000000000000000 in hex, does not fit to Long
    private BigInteger seqno; // 1432551
    private Long createdAt;
    private Long syncedSecondsAgo;

    /**
     * Returns block in format (-1,8000000000000000,1432551)
     *
     * @return String
     */
    public String getShortBlockSeqno() {
        return String.format("(%d,%s,%d)", wc, shard, seqno);
    }

    public String getFullBlockSeqno() {
        return String.format("(%d,%s,%d):%s:%s", wc, shard, seqno, rootHash, fileHash);
    }
}
