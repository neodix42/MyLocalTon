package org.ton.db.entities;

import lombok.*;
import org.ton.executors.liteclient.api.block.Block;

import javax.persistence.Entity;
import javax.persistence.Version;
import java.io.Serializable;

@Entity
@Builder
@ToString
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BlockEntity implements Serializable {
    Long createdAt;
    String seqno;
    Long wc;
    String shard;
    String roothash;
    String filehash;
    Block block;

    @Version
    Integer version;

    public BlockPk getPrimaryKey() {
        return BlockPk.builder()
                .createdAt(createdAt)
                .seqno(seqno)
                .wc(wc)
                .shard(shard)
                .build();
    }
}
