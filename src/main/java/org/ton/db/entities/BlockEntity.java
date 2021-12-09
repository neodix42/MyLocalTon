package org.ton.db.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.executors.liteclient.api.block.Block;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.math.BigInteger;

@Entity
@Builder
@ToString
@Setter
@Getter
@IdClass(BlockPk.class)
public class BlockEntity {
    @Id
    Long createdAt;
    @Id
    BigInteger seqno;
    @Id
    Long wc;
    @Id
    String shard;

    String roothash;
    String filehash;
    Block block;

    public BlockPk getPrimaryKey() {
        return BlockPk.builder()
                .createdAt(createdAt)
                .seqno(seqno)
                .wc(wc)
                .shard(shard)
                .build();
    }
}
