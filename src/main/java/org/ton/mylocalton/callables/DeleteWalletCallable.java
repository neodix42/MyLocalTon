package org.ton.mylocalton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.callables.parameters.WalletCallbackParam;
import org.ton.mylocalton.db.DB2;
import org.ton.mylocalton.db.entities.WalletEntity;
import org.ton.mylocalton.db.entities.WalletPk;

import javax.persistence.EntityManager;
import java.util.concurrent.Callable;

import static java.util.Objects.nonNull;

@Slf4j
public class DeleteWalletCallable implements Callable<WalletCallbackParam> {
  DB2 db;
  WalletPk walletPk;
  WalletEntity foundWallet;

  public DeleteWalletCallable(WalletCallbackParam walletCallbackParam) {
    this.db = walletCallbackParam.getDb();
    this.walletPk = walletCallbackParam.getWalletPk();
    this.foundWallet = walletCallbackParam.getFoundWallet();
  }

  public WalletCallbackParam call() {
    WalletEntity foundWalletResult;
    EntityManager em = db.getEmf().createEntityManager();
    try {
      WalletEntity walletEntity = em.find(WalletEntity.class, walletPk);
      if (nonNull(walletEntity)) {
        em.getTransaction().begin();
        em.remove(walletEntity);
        em.getTransaction().commit();
      }
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
    return WalletCallbackParam.builder().build();
  }
}
