package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
@ToString
public class Block implements Serializable {
    Long globalId;
    Info info;
    ValueFlow valueFlow;
    String shardState;
    Extra extra;

    public List<Transaction> listBlockTrans() {
        return extra.getAccountBlock().getTransactions();
    }

    public List<ShardHash> allShards() {
        return extra.getMasterchainBlock().getShardHashes();
    }
}
