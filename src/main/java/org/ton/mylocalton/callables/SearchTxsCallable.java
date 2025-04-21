package org.ton.mylocalton.callables;

import static org.ton.mylocalton.db.DbPool.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.callables.parameters.TxCallbackParam;
import org.ton.mylocalton.db.DB2;
import org.ton.mylocalton.db.entities.TxEntity;
import org.ton.mylocalton.db.entities.TxPk;
import org.ton.mylocalton.utils.MyLocalTonUtils;

@Slf4j
public class SearchTxsCallable implements Callable<TxCallbackParam> {
  DB2 db;
  TxPk txPk;
  TxEntity foundTx;
  String searchText;

  public SearchTxsCallable(TxCallbackParam txCallbackParam) {
    this.db = txCallbackParam.getDb();
    this.txPk = txCallbackParam.getTxPk();
    this.foundTx = txCallbackParam.getFoundTx();
    this.searchText = txCallbackParam.getSearchText().toUpperCase();
  }

  public TxCallbackParam call() {
    EntityManager em = db.getEmf().createEntityManager();
    String wcShardSeqnoHash = searchText.trim();
    try {
      Long seqno;
      String shard;
      Long wc;
      String hash;
      String hexAddr;
      List<TxEntity> results = new ArrayList<>();
      TypedQuery<TxEntity> query;

      if ((wcShardSeqnoHash.charAt(0) == '(')
          && (wcShardSeqnoHash.charAt(wcShardSeqnoHash.length() - 1) == ')')) {
        String[] s = wcShardSeqnoHash.substring(1, wcShardSeqnoHash.length() - 1).split(",");
        wc = MyLocalTonUtils.parseLong(s[0]);
        shard = s[1];
        seqno = MyLocalTonUtils.parseLong(s[2]);
        query =
            em.createQuery(
                "SELECT b FROM TxEntity b where (b.seqno = :seqno) AND (b.shard = :shard) AND (b.wc = :wc) ORDER BY b.createdAt DESC",
                TxEntity.class);
        results =
            query
                .setParameter(SEQNO, seqno)
                .setParameter(SHARD, shard)
                .setParameter(WC, wc)
                // .setMaxResults(SCROLL_BAR_DELTA) // to many results
                .getResultList();
      } else if (wcShardSeqnoHash.length() == 64) { // tx hash or src/dest addr
        hash = wcShardSeqnoHash;
        query =
            em.createQuery(
                "SELECT b FROM TxEntity b where (b.txHash = :hash) OR (b.fromForSearch = :hash) OR (b.toForSearch = :hash) ORDER BY b.createdAt DESC",
                TxEntity.class);
        results =
            query
                .setParameter(HASH, hash)
                // .setMaxResults(SCROLL_BAR_DELTA) // to many results
                .getResultList();

      } else if (wcShardSeqnoHash.length() > 64) { // wc:addr
        String[] s = wcShardSeqnoHash.split(":");

        if (s.length == 2) {
          //          wc = MyLocalTonUtils.parseLong(s[0]);
          //          hexAddr = s[1];
          query =
              em.createQuery(
                  "SELECT b FROM TxEntity b where (b.fromForSearch = :hash) OR (b.toForSearch = :hash) ORDER BY b.createdAt DESC",
                  TxEntity.class);
          results =
              query
                  .setParameter(HASH, wcShardSeqnoHash)
                  // .setMaxResults(SCROLL_BAR_DELTA) // to many results
                  .getResultList();
        }
      } else {
        seqno = MyLocalTonUtils.parseLong(wcShardSeqnoHash);
        shard = wcShardSeqnoHash;
        wc = MyLocalTonUtils.parseLong(wcShardSeqnoHash);
        query =
            em.createQuery(
                "SELECT b FROM TxEntity b where (b.seqno = :seqno) OR (b.shard = :shard) OR (b.wc = :wc) ORDER BY b.createdAt DESC",
                TxEntity.class);
        results =
            query
                .setParameter(SEQNO, seqno)
                .setParameter(SHARD, shard)
                .setParameter(WC, wc)
                // .setMaxResults(SCROLL_BAR_DELTA) // to many results
                .getResultList();
      }

      log.debug("found txs {}", results.size());

      return TxCallbackParam.builder().foundTxs(results).build();
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }
}
