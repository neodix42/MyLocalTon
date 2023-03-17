package org.ton.callables;

import lombok.extern.slf4j.Slf4j;
import org.ton.callables.parameters.BlockCallbackParam;
import org.ton.db.DB2;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.BlockPk;
import org.ton.utils.MyLocalTonUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

import static org.ton.db.DbPool.*;

@Slf4j
public class SearchBlocksCallable implements Callable<BlockCallbackParam> {
    DB2 db;
    BlockPk blockPk;
    BlockEntity foundBlock;
    Long datetimeFrom;
    String searchText;

    public SearchBlocksCallable(BlockCallbackParam blockCallbackParam) {
        this.db = blockCallbackParam.getDb();
        this.blockPk = blockCallbackParam.getBlockPk();
        this.foundBlock = blockCallbackParam.getFoundBlock();
        this.datetimeFrom = blockCallbackParam.getDatetimeFrom();
        this.searchText = blockCallbackParam.getSearchText().toUpperCase();
    }

    public BlockCallbackParam call() {
        EntityManager em = db.getEmf().createEntityManager();
        String wcShardSeqnoHash = searchText;
        try {
            log.debug("searchBlocks, query {}", wcShardSeqnoHash);

            Long seqno;
            String shard;
            Long wc;
            String hash;
            List<BlockEntity> results;
            TypedQuery<BlockEntity> query;

            if ((wcShardSeqnoHash.charAt(0) == '(') && (wcShardSeqnoHash.charAt(wcShardSeqnoHash.length() - 1) == ')')) {
                String[] s = wcShardSeqnoHash.substring(1, wcShardSeqnoHash.length() - 1).split(",");
                wc = MyLocalTonUtils.parseLong(s[0]);
                shard = s[1];
                seqno = MyLocalTonUtils.parseLong(s[2]);
                query = em.createQuery("SELECT b FROM BlockEntity b where (b.seqno = :seqno) AND (b.shard = :shard) AND (b.wc = :wc) ORDER BY b.createdAt DESC", BlockEntity.class);
                results = query
                        .setParameter(SEQNO, seqno)
                        .setParameter(SHARD, shard)
                        .setParameter(WC, wc)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();
            } else if (wcShardSeqnoHash.length() == 64) { // hashes
                hash = wcShardSeqnoHash;
                query = em.createQuery("SELECT b FROM BlockEntity b where (b.roothash = :hash) OR (b.filehash = :hash) ORDER BY b.createdAt DESC", BlockEntity.class);
                results = query
                        .setParameter(HASH, hash)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();
            } else {
                seqno = MyLocalTonUtils.parseLong(wcShardSeqnoHash);
                shard = wcShardSeqnoHash;
                wc = MyLocalTonUtils.parseLong(wcShardSeqnoHash);
                query = em.createQuery("SELECT b FROM BlockEntity b where (b.seqno = :seqno) OR (b.shard = :shard) OR (b.wc = :wc) ORDER BY b.createdAt DESC", BlockEntity.class);
                results = query
                        .setParameter(SEQNO, seqno)
                        .setParameter(SHARD, shard)
                        .setParameter(WC, wc)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();
            }

            log.debug("found blocks {}", results.size());
            return BlockCallbackParam.builder().foundBlocks(results).build();
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
    }
}