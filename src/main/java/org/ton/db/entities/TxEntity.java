package org.ton.db.entities;

import lombok.*;
import org.ton.executors.liteclient.api.block.Address;
import org.ton.executors.liteclient.api.block.Message;
import org.ton.executors.liteclient.api.block.Transaction;

import javax.persistence.Entity;
import java.math.BigDecimal;

@Entity
@Builder
@ToString
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TxEntity {
    Long createdAt;
    String seqno;
    Long wc;
    String shard;
    String txHash;
    String typeTx;
    String typeMsg;
    String accountAddress;
    String txLt;

    String status;
    Address from;
    Address to;
    String blockRootHash;
    String blockFileHash;
    String fromForSearch;
    String toForSearch;
    BigDecimal amount;
    BigDecimal fees;
    Transaction tx;
    Message message;

    public TxPk getPrimaryKey() {
        return TxPk.builder()
                .createdAt(createdAt)
                .seqno(seqno)
                .wc(wc)
                .shard(shard)
                .txHash(txHash)
                .typeTx(typeTx)
                .typeMsg(typeMsg)
                .accountAddress(accountAddress)
                .txLt(txLt)
                .build();
    }

    public String getShortBlock() {
        return String.format("(%d,%s,%s)", wc, shard, seqno);
    }

    public String getFullBlock() {
        return String.format("(%d,%s,%s):%s:%s", wc, shard, seqno, blockRootHash, blockFileHash);
    }
}
