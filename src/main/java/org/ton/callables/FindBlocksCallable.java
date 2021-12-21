package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.callables.parameters.BlockCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.BlockPk;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

import static org.ton.actions.MyLocalTon.SCROLL_BAR_DELTA;

@Slf4j
public class FindBlocksCallable implements Callable<BlockCallbackParam> {
    DB2 db;
    BlockPk blockPk;
    BlockEntity foundBlock;
    Long datetimeFrom;

    public FindBlocksCallable(BlockCallbackParam blockCallbackParam) {
        this.db = blockCallbackParam.getDb();
        this.blockPk = blockCallbackParam.getBlockPk();
        this.foundBlock = blockCallbackParam.getFoundBlock();
        this.datetimeFrom = blockCallbackParam.getDatetimeFrom();
    }

    public BlockCallbackParam call() {
        List<BlockEntity> foundBlocksResult;
        EntityManager em = db.getEmf().createEntityManager();
        try {
            TypedQuery<BlockEntity> query = em.createQuery("SELECT b FROM BlockEntity b where (b.createdAt < :datetimefrom) ORDER BY b.createdAt DESC", BlockEntity.class); // including all shards
            foundBlocksResult = query
                    .setParameter("datetimefrom", datetimeFrom)
                    .setMaxResults(SCROLL_BAR_DELTA)
                    .getResultList();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
        return BlockCallbackParam.builder().foundBlocks(foundBlocksResult).build();
    }
}