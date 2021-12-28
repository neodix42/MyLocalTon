package org.ton.db;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DB {
    /*
    public static final String SEQNO = "seqno";
    public static final String SHARD = "shard";
    public static final String WC = "wc";
    public static final String HEX_ADDR = "hexAddr";
    public static final String HASH = "hash";
    private static final EntityManagerFactory emf;

    private DB() {

    }

    static {
        emf = Persistence.createEntityManagerFactory("objectdb:" + MyLocalTonSettings.DB_DIR + File.separator + "myLocalTon.odb");
        log.info("DB initialized.");
    }

    // find
    public static BlockEntity findBlock(BlockPk blockPk) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(BlockEntity.class, blockPk);
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static TxEntity findTx(TxPk txPk) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(TxEntity.class, txPk);
//        } catch (Exception e) {
//            log.debug("not found since no table exist");
//            return null;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static WalletEntity findWallet(WalletPk walletPk) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(WalletEntity.class, walletPk);
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static void deleteWallet(WalletPk walletPk) {
        EntityManager em = emf.createEntityManager();
        try {
            WalletEntity walletEntity = em.find(WalletEntity.class, walletPk);
            if (nonNull(walletEntity)) {
                log.debug("delete wallet {}", walletEntity);
                em.getTransaction().begin();
                em.remove(walletEntity);
                em.getTransaction().commit();
                log.debug("wallet deleted");
            }
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    // insert
    // Transactions
    public static void insertTx(TxEntity tx) {
        EntityManager em = emf.createEntityManager();
        try {
            if (isNull(findTx(tx.getPrimaryKey()))) {
                log.debug("inserting into db tx {}", tx);
                em.getTransaction().begin();
                em.persist(tx);
                em.getTransaction().commit();
                log.debug("tx inserted into db");
            }
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    // Blocks
    public static void insertBlock(BlockEntity block) {
        EntityManager em = emf.createEntityManager();
        try {
            if (isNull(findBlock(block.getPrimaryKey()))) {
                em.getTransaction().begin();
                em.persist(block);
                em.getTransaction().commit();
            }
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
    }

    // Wallets
    public static void insertWallet(WalletEntity walletEntity) {
        EntityManager em = emf.createEntityManager();
        try {
            if (isNull(findWallet(walletEntity.getPrimaryKey()))) {
                log.debug("Inserting into db wallet {}", walletEntity.getWallet().getFullWalletAddress());
                em.getTransaction().begin();
                em.persist(walletEntity);
                em.getTransaction().commit();
                log.debug("Wallet inserted into db, {}", walletEntity);
            } else {
                log.debug("Wallet {} already exists.", walletEntity.getWallet().getFullWalletAddress());
            }
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
    }

    public static void updateWalletState(WalletEntity walletEntity, AccountState accountState) {
        log.debug("updating account state, {},  {}", walletEntity.getFullAddress(), accountState);
        EntityManager em = emf.createEntityManager();
        try {
            if (isNull(accountState.getAddress())) {
                log.debug("cannot update accountState, address is null");
                return;
            }
            WalletEntity walletFound = em.find(WalletEntity.class, walletEntity.getPrimaryKey());

            if (nonNull(walletFound)) {
                em.getTransaction().begin();
                walletFound.setAccountState(accountState);
                if ((!accountState.getStateCode().isEmpty()) && (!accountState.getStateData().isEmpty())) {
                    Pair<WalletVersion, Long> walletVersionAndId = Utils.detectWalledVersionAndId(accountState);
                    walletFound.setWalletVersion(walletVersionAndId.getLeft());
                    walletFound.getWallet().setSubWalletId(walletVersionAndId.getRight());
                }
                em.getTransaction().commit();
                log.debug("info account state, {},  {}", walletEntity.getFullAddress(), walletFound.getAccountState());
            }
        } catch (Exception e) {
            log.error("failed to update wallet state, Wallet {}, Error: {}", walletEntity.getWallet().getFullWalletAddress(), e.getMessage());
        } finally {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            if (em.isOpen())
                em.close();
        }
    }

    public static long getNumberOfPreinstalledWallets() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery("SELECT count(t) FROM WalletEntity t where t.preinstalled = true", Long.class);
            long count = query.getSingleResult();
            log.debug("getNumberOfPreinstalledWallets {}", count);
            return count;
        } catch (Exception e) {
            return 0;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static List<WalletEntity> getAllWallets() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<WalletEntity> query = em.createQuery("SELECT t FROM WalletEntity t ORDER BY t.createdAt ASC", WalletEntity.class);
            List<WalletEntity> wallets = query.getResultList();
            log.debug("getAllWallets size {}", wallets.size());
            em.close();
            return wallets;
        } catch (Exception e) {
            log.error("Error {}", e.getMessage());
            return List.of();
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static boolean existsMainWallet() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery("SELECT count(t) FROM WalletEntity t where t.mainWalletInstalled = true", Long.class);
            return query.getSingleResult() != 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static boolean existsConfigWallet() {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<Long> query = em.createQuery("SELECT count(t) FROM WalletEntity t where t.configWalletInstalled = true", Long.class);
            return query.getSingleResult() != 0;
        } catch (Exception e) {
            return false;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    // update
    // block
    public static void updateBlockDump(BlockPk blockPk, Block blockDump) {
        EntityManager em = emf.createEntityManager();
        try {
            BlockEntity blockFound = em.find(BlockEntity.class, blockPk);
            em.getTransaction().begin();
            blockFound.setBlock(blockDump);
            em.getTransaction().commit();
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    // blocks
    public static List<BlockEntity> loadBlocksBefore(long datetimeFrom) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<BlockEntity> query = em.createQuery("SELECT b FROM BlockEntity b where (b.createdAt < :datetimefrom) ORDER BY b.createdAt DESC", BlockEntity.class); // including all shards
            List<BlockEntity> results = query
                    .setParameter("datetimefrom", datetimeFrom)
                    .setMaxResults(SCROLL_BAR_DELTA)
                    .getResultList();
            log.debug("loaded blocks {}", results.size());
            return results;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    // txs
    public static List<TxEntity> loadTxsBefore(long datetimeFrom) {
        EntityManager em = emf.createEntityManager();
        try {
            TypedQuery<TxEntity> query = em.createQuery("SELECT t FROM TxEntity t where (t.createdAt < :datetimefrom) ORDER BY t.createdAt DESC", TxEntity.class);
            List<TxEntity> results = query
                    .setParameter("datetimefrom", datetimeFrom)
                    .setMaxResults(SCROLL_BAR_DELTA)
                    .getResultList();
            log.debug("loaded txs {}", results.size());
            return results;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    //search functions
    public static List<BlockEntity> searchBlocks(String wcShardSeqnoHash) {

        if (wcShardSeqnoHash.length() == 0) {
            return List.of();
        }

        EntityManager em = emf.createEntityManager();
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
                wc = Utils.parseLong(s[0]);
                shard = s[1];
                seqno = Utils.parseLong(s[2]);
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
                seqno = Utils.parseLong(wcShardSeqnoHash);
                shard = wcShardSeqnoHash;
                wc = Utils.parseLong(wcShardSeqnoHash);
                query = em.createQuery("SELECT b FROM BlockEntity b where (b.seqno = :seqno) OR (b.shard = :shard) OR (b.wc = :wc) ORDER BY b.createdAt DESC", BlockEntity.class);
                results = query
                        .setParameter(SEQNO, seqno)
                        .setParameter(SHARD, shard)
                        .setParameter(WC, wc)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();
            }

            log.debug("found blocks {}", results.size());
            return results;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static List<TxEntity> searchTxs(String wcShardSeqnoHash) {
        log.debug("searchTxs, query {}", wcShardSeqnoHash);
        if (wcShardSeqnoHash.length() == 0) {
            return List.of();
        }

        EntityManager em = emf.createEntityManager();
        try {
            Long seqno;
            String shard;
            Long wc;
            String hash;
            String hexAddr;
            List<TxEntity> results = new ArrayList<>();
            TypedQuery<TxEntity> query;

            if ((wcShardSeqnoHash.charAt(0) == '(') && (wcShardSeqnoHash.charAt(wcShardSeqnoHash.length() - 1) == ')')) {
                String[] s = wcShardSeqnoHash.substring(1, wcShardSeqnoHash.length() - 1).split(",");
                wc = Utils.parseLong(s[0]);
                shard = s[1];
                seqno = Utils.parseLong(s[2]);
                query = em.createQuery("SELECT b FROM TxEntity b where (b.seqno = :seqno) AND (b.shard = :shard) AND (b.wc = :wc) ORDER BY b.createdAt DESC", TxEntity.class);
                results = query
                        .setParameter(SEQNO, seqno)
                        .setParameter(SHARD, shard)
                        .setParameter(WC, wc)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();
            } else if (wcShardSeqnoHash.length() == 64) { //tx hash or src/dest addr
                hash = wcShardSeqnoHash;
                query = em.createQuery("SELECT b FROM TxEntity b where (b.txHash = :hash) OR (b.fromForSearch = :hash) OR (b.toForSearch = :hash) ORDER BY b.createdAt DESC", TxEntity.class);
                results = query
                        .setParameter(HASH, hash)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();


            } else if (wcShardSeqnoHash.length() > 64) { // wc:addr
                String[] s = wcShardSeqnoHash.split(":");

                if (s.length == 2) {
                    wc = Utils.parseLong(s[0]);
                    hexAddr = s[1];
                    query = em.createQuery("SELECT b FROM TxEntity b where (b.txHash = :hash) OR (b.fromForSearch = :hash) OR (b.toForSearch = :hash) ORDER BY b.createdAt DESC", TxEntity.class);
                    results = query
                            .setParameter(HASH, hexAddr)
                            //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                            .getResultList();

                }
            } else {
                seqno = Utils.parseLong(wcShardSeqnoHash);
                shard = wcShardSeqnoHash;
                wc = Utils.parseLong(wcShardSeqnoHash);
                query = em.createQuery("SELECT b FROM TxEntity b where (b.seqno = :seqno) OR (b.shard = :shard) OR (b.wc = :wc) ORDER BY b.createdAt DESC", TxEntity.class);
                results = query
                        .setParameter(SEQNO, seqno)
                        .setParameter(SHARD, shard)
                        .setParameter(WC, wc)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();
            }

            log.debug("found txs {}", results.size());
            return results;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    public static List<WalletEntity> searchAccounts(String wcShardSeqnoHash) {
        log.debug("searchAccounts, query {}", wcShardSeqnoHash);
        if (wcShardSeqnoHash.length() == 0) {
            return List.of();
        }

        EntityManager em = emf.createEntityManager();
        try {
            Long wc;
            String hexAddr;
            List<WalletEntity> results = new ArrayList<>();
            TypedQuery<WalletEntity> query;

            if (wcShardSeqnoHash.length() == 64) {
                hexAddr = wcShardSeqnoHash;
                query = em.createQuery("SELECT b FROM WalletEntity b where (b.hexAddress = :hexAddr)", WalletEntity.class);
                results = query
                        .setParameter(HEX_ADDR, hexAddr)
                        //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                        .getResultList();


            } else if (wcShardSeqnoHash.length() > 64) { // wc:addr
                String[] s = wcShardSeqnoHash.split(":");
                if (s.length == 2) {
                    wc = Utils.parseLong(s[0]);
                    hexAddr = s[1];
                    query = em.createQuery("SELECT b FROM WalletEntity b where (b.wc = :wc) AND (b.hexAddress = :hexAddr)", WalletEntity.class);
                    results = query
                            .setParameter(WC, wc)
                            .setParameter(HEX_ADDR, hexAddr)
                            //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                            .getResultList();

                }
            } else if (wcShardSeqnoHash.length() == 48) { // base64 addr
                wcShardSeqnoHash = Utils.friendlyAddrToHex(wcShardSeqnoHash);
                String[] s = wcShardSeqnoHash.split(":");
                if (s.length == 2) {
                    wc = Utils.parseLong(s[0]);
                    hexAddr = s[1];
                    query = em.createQuery("SELECT b FROM WalletEntity b where (b.wc = :wc) AND (b.hexAddress = :hexAddr)", WalletEntity.class);
                    results = query
                            .setParameter(WC, wc)
                            .setParameter(HEX_ADDR, hexAddr)
                            //.setMaxResults(SCROLL_BAR_DELTA) // to many results
                            .getResultList();
                }
            }

            log.debug("found accounts {}", results.size());
            return results;
        } finally {
            if (em.isOpen())
                em.close();
        }
    }
    */
}
