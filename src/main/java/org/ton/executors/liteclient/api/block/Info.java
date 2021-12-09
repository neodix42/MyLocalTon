package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * Block header. Another non-split component of a shardchain block
 * is the block header, which contains general information such as (w, s) (i.e.,
 * the workchain_id and the common binary prefix of all account_ids assigned
 * to the current shardchain), the block’s sequence number (defined to be the
 * smallest non-negative integer larger than the sequence numbers of its predecessors),
 * logical time, and generation unixtime. It also contains the hash of
 * the immediate antecessor of the block (or of its two immediate antecessors
 * in the case of a preceding shardchain merge event), the hashes of its initial
 * and final states (i.e., of the states of the shardchain immediately before and
 * immediately after processing the current block), and the hash of the most
 * recent masterchain block known when the shardchain block was generated.
 */
@Builder
@ToString
@Getter
public class Info implements Serializable {
    Long wc;
    BigInteger seqNo;
    BigInteger prevKeyBlockSeqno;
    BigInteger version;
    Byte notMaster;
    BigInteger keyBlock;
    BigInteger vertSeqnoIncr;
    BigInteger vertSeqno;
    BigInteger getValidatorListHashShort;
    BigInteger getCatchainSeqno;
    BigInteger minRefMcSeqno;
    Byte wantSplit;
    Byte wantMerge;
    Byte afterMerge;
    Byte afterSplit;
    Byte beforeSplit;
    Long genUtime;
    Integer flags;
    BigInteger startLt;
    BigInteger endLt;
    Long prevBlockSeqno;
    BigInteger prevEndLt;
    String prevRootHash;
    String prevFileHash;
}
