package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.callables.parameters.WalletCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.utils.Utils;
import org.ton.wallet.WalletVersion;

import javax.persistence.EntityManager;
import java.util.concurrent.Callable;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class UpdateAccountStateCallable implements Callable<WalletCallbackParam> {
    DB2 db;
    WalletPk walletPk;
    AccountState accountState;
    Long seqno;

    public UpdateAccountStateCallable(WalletCallbackParam walletCallbackParam) {
        this.db = walletCallbackParam.getDb();
        this.walletPk = walletCallbackParam.getWalletPk();
        this.accountState = walletCallbackParam.getAccountState();
        this.seqno = walletCallbackParam.getSeqno();
    }

    public WalletCallbackParam call() {
        EntityManager em = db.getEmf().createEntityManager();
        try {
            if (isNull(accountState.getAddress())) {
                log.debug("cannot update accountState, address is null");
            } else {
                WalletEntity walletFound = em.find(WalletEntity.class, walletPk);

                if (nonNull(walletFound)) {
                    em.getTransaction().begin();
                    walletFound.setAccountState(accountState);
                    walletFound.setSeqno(seqno);
                    if ((!accountState.getStateCode().isEmpty()) && (!accountState.getStateData().isEmpty())) {
                        Pair<WalletVersion, Long> walletVersionAndId = Utils.detectWalledVersionAndId(accountState);
                        walletFound.setWalletVersion(walletVersionAndId.getLeft());
                        walletFound.getWallet().setSubWalletId(walletVersionAndId.getRight());
                    }
                    em.getTransaction().commit();
                }
            }
        } catch (Exception e) {
            log.error("Error {}", e.getMessage());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
        return WalletCallbackParam.builder().build();
    }
}