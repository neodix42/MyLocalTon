package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.callables.parameters.WalletCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;

import javax.persistence.EntityManager;
import java.util.concurrent.Callable;

@Slf4j
public class FindWalletCallable implements Callable<WalletCallbackParam> {
    DB2 db;
    WalletPk walletPk;
    WalletEntity foundWallet;

    public FindWalletCallable(WalletCallbackParam walletCallbackParam) {
        this.db = walletCallbackParam.getDb();
        this.walletPk = walletCallbackParam.getWalletPk();
        this.foundWallet = walletCallbackParam.getFoundWallet();
    }

    public WalletCallbackParam call() {
        WalletEntity foundWalletResult;
        EntityManager em = db.getEmf().createEntityManager();
        try {
            foundWalletResult = em.find(WalletEntity.class, walletPk);
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
        return WalletCallbackParam.builder().foundWallet(foundWalletResult).build();
    }
}