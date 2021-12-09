package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@ToString
@Getter
public class ShardHash implements Serializable {
    Long wc;
    BigInteger seqno;
    BigInteger regMcSeqno;
    BigInteger startLt;
    BigInteger endLt;
    String rootHash;
    String fileHash;
    Byte beforeSplit;
    Byte beforeMerge;
    Byte wantSplit;
    Byte wantMerge;
    Byte nxCcUpdate;
    Byte flags;
    BigInteger nextCatchainSeqno;
    String nextValidatorShard;
    BigInteger minRefMcSeqno;
    Long genUtime;
    Value feesCollected;
    Value fundsCreated;
}
