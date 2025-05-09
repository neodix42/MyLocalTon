package org.ton.mylocalton.callables;

import static org.ton.mylocalton.db.DbPool.HEX_ADDR;
import static org.ton.mylocalton.db.DbPool.WC;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.callables.parameters.WalletCallbackParam;
import org.ton.mylocalton.db.DB2;
import org.ton.mylocalton.db.entities.WalletEntity;
import org.ton.mylocalton.db.entities.WalletPk;
import org.ton.mylocalton.utils.MyLocalTonUtils;

@Slf4j
public class SearchWalletsCallable implements Callable<WalletCallbackParam> {
  DB2 db;
  WalletPk walletPk;
  WalletEntity foundWallet;
  String searchText;

  public SearchWalletsCallable(WalletCallbackParam walletCallbackParam) {
    this.db = walletCallbackParam.getDb();
    this.walletPk = walletCallbackParam.getWalletPk();
    this.foundWallet = walletCallbackParam.getFoundWallet();
    this.searchText = walletCallbackParam.getSearchText().toUpperCase();
  }

  public WalletCallbackParam call() {
    EntityManager em = db.getEmf().createEntityManager();
    String wcShardSeqnoHash = searchText;
    try {
      Long wc;
      String hexAddr;
      List<WalletEntity> results = new ArrayList<>();
      TypedQuery<WalletEntity> query;

      if (wcShardSeqnoHash.length() == 64) {
        hexAddr = wcShardSeqnoHash;
        query =
            em.createQuery(
                "SELECT b FROM WalletEntity b where (b.hexAddress = :hexAddr)", WalletEntity.class);
        results =
            query
                .setParameter(HEX_ADDR, hexAddr)
                // .setMaxResults(SCROLL_BAR_DELTA) // to many results
                .getResultList();
      } else if (wcShardSeqnoHash.length() > 64) { // wc:addr
        String[] s = wcShardSeqnoHash.split(":");
        if (s.length == 2) {
          wc = MyLocalTonUtils.parseLong(s[0]);
          hexAddr = s[1];
          query =
              em.createQuery(
                  "SELECT b FROM WalletEntity b where (b.wc = :wc) AND (b.hexAddress = :hexAddr)",
                  WalletEntity.class);
          results =
              query
                  .setParameter(WC, wc)
                  .setParameter(HEX_ADDR, hexAddr)
                  // .setMaxResults(SCROLL_BAR_DELTA) // to many results
                  .getResultList();
        }
      } else if (wcShardSeqnoHash.length() == 48) { // base64 addr
        wcShardSeqnoHash = MyLocalTonUtils.friendlyAddrToHex(wcShardSeqnoHash);
        String[] s = wcShardSeqnoHash.split(":");
        if (s.length == 2) {
          wc = MyLocalTonUtils.parseLong(s[0]);
          hexAddr = s[1];
          query =
              em.createQuery(
                  "SELECT b FROM WalletEntity b where (b.wc = :wc) AND (b.hexAddress = :hexAddr)",
                  WalletEntity.class);
          results =
              query
                  .setParameter(WC, wc)
                  .setParameter(HEX_ADDR, hexAddr)
                  // .setMaxResults(SCROLL_BAR_DELTA) // to many results
                  .getResultList();
        }
      }

      log.debug("found accounts {}", results.size());
      return WalletCallbackParam.builder().foundWallets(results).build();
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
  }
}
