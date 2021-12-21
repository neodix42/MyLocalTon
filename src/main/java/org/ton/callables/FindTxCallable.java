package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.callables.parameters.TxCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.TxPk;

import javax.persistence.EntityManager;
import java.util.concurrent.Callable;

@Slf4j
public class FindTxCallable implements Callable<TxCallbackParam> {
    DB2 db;
    TxPk txPk;
    TxEntity txEntity;

    public FindTxCallable(TxCallbackParam txCallbackParam) {
        this.db = txCallbackParam.getDb();
        this.txPk = txCallbackParam.getTxPk();
        this.txEntity = txCallbackParam.getFoundTx();
    }

    public TxCallbackParam call() {
        TxEntity foundTxResult;
        EntityManager em = db.getEmf().createEntityManager();
        try {
            foundTxResult = em.find(TxEntity.class, txPk);
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
        return TxCallbackParam.builder().foundTx(foundTxResult).build();
    }
}