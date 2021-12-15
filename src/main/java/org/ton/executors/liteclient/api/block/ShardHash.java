package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShardHash implements Serializable {
    Long wc;
    String seqno;
    String regMcSeqno;
    String startLt;
    String endLt;
    String rootHash;
    String fileHash;
    Byte beforeSplit;
    Byte beforeMerge;
    Byte wantSplit;
    Byte wantMerge;
    Byte nxCcUpdate;
    Byte flags;
    String nextCatchainSeqno;
    String nextValidatorShard;
    String minRefMcSeqno;
    Long genUtime;
    Value feesCollected;
    Value fundsCreated;
}
