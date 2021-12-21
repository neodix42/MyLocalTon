package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.callables.parameters.TxCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.TxPk;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

import static org.ton.actions.MyLocalTon.SCROLL_BAR_DELTA;

@Slf4j
public class FindTxsCallable implements Callable<TxCallbackParam> {
    DB2 db;
    TxPk txPk;
    TxEntity txEntity;
    Long datetimeFrom;

    public FindTxsCallable(TxCallbackParam txCallbackParam) {
        this.db = txCallbackParam.getDb();
        this.txPk = txCallbackParam.getTxPk();
        this.txEntity = txCallbackParam.getFoundTx();
        this.datetimeFrom = txCallbackParam.getDatetimeFrom();
    }

    public TxCallbackParam call() {
        List<TxEntity> foundTxsResult;
        EntityManager em = db.getEmf().createEntityManager();
        try {
            TypedQuery<TxEntity> query = em.createQuery("SELECT t FROM TxEntity t where (t.createdAt < :datetimefrom) ORDER BY t.createdAt DESC", TxEntity.class);
            foundTxsResult = query
                    .setParameter("datetimefrom", datetimeFrom)
                    .setMaxResults(SCROLL_BAR_DELTA)
                    .getResultList();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
        return TxCallbackParam.builder().foundTxs(foundTxsResult).build();
    }
}