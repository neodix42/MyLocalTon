package org.ton.mylocalton.db.entities;

import java.math.BigInteger;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Entity
@Builder
@ToString
@Getter
@IdClass(TxPk.class)
public class TxEntity {
  @Id Long createdAt;
  @Id BigInteger seqno;
  @Id Long wc;
  @Id String shard;
  @Id String txHash;
  @Id String typeTx;
  @Id String typeMsg;
  @Id String accountAddress;
  @Id BigInteger txLt;

  String status;
  String from;
  String to;
  String blockRootHash;
  String blockFileHash;
  String fromForSearch;
  String toForSearch;
  BigInteger amount;
  BigInteger fees;
  org.ton.ton4j.tlb.Transaction tx;
  org.ton.ton4j.tlb.Message message;

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
