package org.ton.db.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.executors.liteclient.api.block.LiteClientAddress;
import org.ton.executors.liteclient.api.block.Message;
import org.ton.executors.liteclient.api.block.Transaction;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import java.math.BigDecimal;
import java.math.BigInteger;

@Entity
@Builder
@ToString
@Getter
@IdClass(TxPk.class)
public class TxEntity {
    @Id
    Long createdAt;
    @Id
    BigInteger seqno;
    @Id
    Long wc;
    @Id
    String shard;
    @Id
    String txHash;
    @Id
    String typeTx;
    @Id
    String typeMsg;
    @Id
    String accountAddress;
    @Id
    BigInteger txLt;

    String status;
    LiteClientAddress from;
    LiteClientAddress to;
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
        return "(" + wc + "," + shard + "," + seqno + ")";
    }

    public String getFullBlock() {
        return "(" + wc + "," + shard + "," + seqno + "):" + blockRootHash + ":" + blockFileHash;
    }
}
