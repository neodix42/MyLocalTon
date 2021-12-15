package org.ton.db.entities;

import lombok.*;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BlockPk {
    Long createdAt;
    String seqno;
    Long wc;
    String shard;
}