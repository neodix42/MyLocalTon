package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.callables.parameters.BlockCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.BlockPk;

import javax.persistence.EntityManager;
import java.util.concurrent.Callable;

@Slf4j
public class FindBlockCallable implements Callable<BlockCallbackParam> {
    DB2 db;
    BlockPk blockPk;
    BlockEntity foundBlock;

    public FindBlockCallable(BlockCallbackParam blockCallbackParam) {
        this.db = blockCallbackParam.getDb();
        this.blockPk = blockCallbackParam.getBlockPk();
        this.foundBlock = blockCallbackParam.getFoundBlock();
    }

    public BlockCallbackParam call() {
        BlockEntity foundBlockResult;
        EntityManager em = db.getEmf().createEntityManager();
        try {
            foundBlockResult = em.find(BlockEntity.class, blockPk);
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
        return BlockCallbackParam.builder().foundBlock(foundBlockResult).build();
    }
}