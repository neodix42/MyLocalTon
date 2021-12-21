package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.callables.parameters.WalletCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
public class GetWalletsCallable implements Callable<WalletCallbackParam> {
    DB2 db;
    WalletPk walletPk;
    WalletEntity foundWallet;

    public GetWalletsCallable(WalletCallbackParam walletCallbackParam) {
        this.db = walletCallbackParam.getDb();
        this.walletPk = walletCallbackParam.getWalletPk();
        this.foundWallet = walletCallbackParam.getFoundWallet();
    }

    public WalletCallbackParam call() {
        EntityManager em = db.getEmf().createEntityManager();
        try {
            TypedQuery<WalletEntity> query = em.createQuery("SELECT t FROM WalletEntity t ORDER BY t.createdAt ASC", WalletEntity.class);
            List<WalletEntity> foundWalletResult = query.getResultList();
            return WalletCallbackParam.builder().foundWallets(foundWalletResult).build();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
    }
}