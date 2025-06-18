package org.ton.mylocalton.callables;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.mylocalton.callables.parameters.WalletCallbackParam;
import org.ton.mylocalton.db.DB2;
import org.ton.mylocalton.db.entities.WalletEntity;
import org.ton.mylocalton.db.entities.WalletPk;
import org.ton.ton4j.smartcontract.types.WalletVersion;
import org.ton.ton4j.tonlib.types.RawAccountState;

import javax.persistence.EntityManager;
import java.util.concurrent.Callable;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class UpdateAccountStateCallable implements Callable<WalletCallbackParam> {
  DB2 db;
  WalletPk walletPk;
  RawAccountState accountState;
  Long seqno;
  WalletVersion walletVersion;

  public UpdateAccountStateCallable(WalletCallbackParam walletCallbackParam) {
    this.db = walletCallbackParam.getDb();
    this.walletPk = walletCallbackParam.getWalletPk();
    this.accountState = walletCallbackParam.getAccountState();
    this.seqno = walletCallbackParam.getSeqno();
    this.walletVersion = walletCallbackParam.getWalletVersion();
  }

  public WalletCallbackParam call() {
    EntityManager em = db.getEmf().createEntityManager();
    try {
      if (isNull(accountState)) {
        log.info("cannot update accountState, address is null");
      } else {
        WalletEntity walletFound = em.find(WalletEntity.class, walletPk);

        if (nonNull(walletFound)) {
          em.getTransaction().begin();
          walletFound.setAccountState(accountState);
          walletFound.setSeqno(seqno);
          walletFound.setWalletVersion(walletVersion);
          em.getTransaction().commit();
        }
      }
    } catch (Exception e) {
      log.error("Error {}", e.getMessage());
      log.error(ExceptionUtils.getStackTrace(e));
    } finally {
      if (em.getTransaction().isActive()) em.getTransaction().rollback();
      if (em.isOpen()) em.close();
    }
    return WalletCallbackParam.builder().build();
  }
}
