package org.ton.mylocalton.db;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.smartcontract.types.WalletVersion;
import org.ton.ton4j.tlb.Account;
import org.ton.mylocalton.callables.ConfigWalletExistsCallable;
import org.ton.mylocalton.callables.DeleteWalletCallable;
import org.ton.mylocalton.callables.FindBlockCallable;
import org.ton.mylocalton.callables.FindBlocksCallable;
import org.ton.mylocalton.callables.FindTxCallable;
import org.ton.mylocalton.callables.FindTxsCallable;
import org.ton.mylocalton.callables.FindWalletCallable;
import org.ton.mylocalton.callables.GetWalletsCallable;
import org.ton.mylocalton.callables.MainWalletExistsCallable;
import org.ton.mylocalton.callables.SearchBlocksCallable;
import org.ton.mylocalton.callables.SearchTxsCallable;
import org.ton.mylocalton.callables.SearchWalletsCallable;
import org.ton.mylocalton.callables.UpdateAccountStateCallable;
import org.ton.mylocalton.callables.WalletsCountCallable;
import org.ton.mylocalton.callables.parameters.BlockCallbackParam;
import org.ton.mylocalton.callables.parameters.TxCallbackParam;
import org.ton.mylocalton.callables.parameters.WalletCallbackParam;
import org.ton.mylocalton.db.entities.BlockEntity;
import org.ton.mylocalton.db.entities.BlockPk;
import org.ton.mylocalton.db.entities.TxEntity;
import org.ton.mylocalton.db.entities.TxPk;
import org.ton.mylocalton.db.entities.WalletEntity;
import org.ton.mylocalton.db.entities.WalletPk;
import org.ton.mylocalton.settings.MyLocalTonSettings;
import org.ton.mylocalton.utils.MyLocalTonUtils;

/**
 * DbPool class used to overcome ObjectDB evaluation limit, where only 1mio objects per DB are
 * allowed. Once the limit is reached, we spawn a new DB. At the same time only one DB is active for
 * a write operations. For a read operations multiple DBs are queried in parallel. This class can be
 * deleted once ObjectDB comes up with OEM license for open-source non-profit projects.
 */
@Slf4j
public class DbPool {
  public static final String SEQNO = "seqno";
  public static final String SHARD = "shard";
  public static final String WC = "wc";
  public static final String HEX_ADDR = "hexAddr";
  public static final String HASH = "hash";

  public static final String TOO_MANY_PERSISTENT_OBJECTS_1000000 =
      "Too many persistent objects (>1000000)";
  private final AtomicBoolean spawned = new AtomicBoolean(false);
  private final MyLocalTonSettings settings;
  private final Map<String, String> poolInSettings;
  private final List<DB2> allDBs;
  private final Cache<WalletPk, WalletEntity> walletCache =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10000).build();
  private final Cache<TxPk, TxEntity> txCache =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10000).build();
  private final Cache<BlockPk, BlockEntity> blockCache =
      CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10000).build();
  DB2 activeDB;

  public DbPool(MyLocalTonSettings loadedSettings) {

    this.settings = loadedSettings;
    this.poolInSettings = settings.getDbPool();
    this.allDBs = new ArrayList<>();

    // pick active db
    for (Map.Entry<String, String> entry : poolInSettings.entrySet()) {
      DB2 db = new DB2(entry.getKey());
      if (entry.getValue().equals("ACTIVE")) {
        activeDB = db;
      }
      allDBs.add(db);
    }

    if (isNull(activeDB)) { // initialize first DB
      spawnNewDb();
      spawned.set(false);
    }
  }

  // update block
  /*
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
  */

  private void spawnNewDb() {
    if (!spawned.getAndSet(true)) {

      String dbName = UUID.randomUUID().toString();
      log.debug("Spawning new DB {}", dbName);

      activeDB = new DB2(dbName);
      allDBs.add(activeDB);

      poolInSettings.replaceAll((key, value) -> "FILLED");
      poolInSettings.put(dbName, "ACTIVE");

      settings.setDbPool(poolInSettings);
      MyLocalTonUtils.saveSettingsToGson(settings);

      ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
      try {
        scheduler.schedule(
            () -> {
              log.debug("new spawning prepared");
              spawned.set(false);
            },
            5,
            TimeUnit.SECONDS);
      } finally {
        scheduler.shutdown();
      }
    }
  }

  /**
   * Async search of wallet by primary key in all DBs
   *
   * @param walletPk wallet's to find primary key
   * @return return any found wallet
   */
  public WalletEntity findWallet(WalletPk walletPk) {

    WalletEntity result = null;
    WalletEntity cachedWallet = walletCache.getIfPresent(walletPk);
    if (cachedWallet != null) {
      log.debug("Wallet {} found in cache", cachedWallet);
      return cachedWallet;
    }
    try {
      List<FindWalletCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        FindWalletCallable callable =
            new FindWalletCallable(WalletCallbackParam.builder().db(db).walletPk(walletPk).build());
        callablesList.add(callable);
      }
      List<Future<WalletCallbackParam>> futures =
          ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<WalletCallbackParam> future : futures) {
        WalletEntity wallet = future.get().getFoundWallet(); // pick any wallet
        if (nonNull(wallet)) {
          result = wallet;
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error findWallet(), {}", e.getMessage(), e);
      return null;
    }
  }

  public TxEntity findTx(TxPk txPk) {
    TxEntity result = null;
    TxEntity cachedTx = txCache.getIfPresent(txPk);
    if (cachedTx != null) {
      log.debug("Tx {} found in cache", cachedTx);
      return cachedTx;
    }

    try {
      List<FindTxCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        FindTxCallable callable =
            new FindTxCallable(TxCallbackParam.builder().db(db).txPk(txPk).build());
        callablesList.add(callable);
      }

      List<Future<TxCallbackParam>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<TxCallbackParam> future : futures) {
        TxEntity tx = future.get().getFoundTx(); // pick any wallet
        if (nonNull(tx)) {
          result = tx;
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error findTx(), {}", e.getMessage(), e);
      return null;
    }
  }

  public BlockEntity findBlock(BlockPk blockPk) {
    BlockEntity result = null;
    BlockEntity cachedBlock = blockCache.getIfPresent(blockPk);
    if (cachedBlock != null) {
      log.debug("Block {} found in cache", blockPk);
      return cachedBlock;
    }

    try {
      List<FindBlockCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        FindBlockCallable callable =
            new FindBlockCallable(BlockCallbackParam.builder().db(db).blockPk(blockPk).build());
        callablesList.add(callable);
      }

      List<Future<BlockCallbackParam>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<BlockCallbackParam> future : futures) {
        BlockEntity block = future.get().getFoundBlock(); // pick any wallet
        if (nonNull(block)) {
          result = block;
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error findBlock(), {}", e.getMessage(), e);
      return null;
    }
  }

  public void insertBlock(BlockEntity block) {
    if (activeDB.getEmf().isOpen()) {
      EntityManager em = activeDB.getEmf().createEntityManager();
      try {
        if (isNull(findBlock(block.getPrimaryKey()))) {
          em.getTransaction().begin();
          em.persist(block);
          em.getTransaction().commit();
        }
      } catch (PersistenceException e) {
        if (e.getMessage().contains(TOO_MANY_PERSISTENT_OBJECTS_1000000)) {
          spawnNewDb();
          insertBlock(block); // repeat failed insert into newly spawned db
        }
      } finally {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
      }
    }
  }

  public void insertTx(TxEntity tx) {
    if (activeDB.getEmf().isOpen()) {
      EntityManager em = activeDB.getEmf().createEntityManager();
      try {
        if (isNull(findTx(tx.getPrimaryKey()))) {
          em.getTransaction().begin();
          em.persist(tx);
          em.getTransaction().commit();
        }
      } catch (PersistenceException e) {
        if (e.getMessage().contains(TOO_MANY_PERSISTENT_OBJECTS_1000000)) {
          spawnNewDb();
          insertTx(tx); // repeat failed insert into newly spawned db
        }
        if (nonNull(e.getCause())) {
          log.error(
              "Error insertTx(), {}, {}", e.getMessage(), e.getCause().getCause().getMessage());
        }
      } finally {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
      }
    }
  }

  public void insertWallet(WalletEntity walletEntity) {
    if (activeDB.getEmf().isOpen()) {
      EntityManager em = activeDB.getEmf().createEntityManager();
      try {
        WalletPk pk = walletEntity.getPrimaryKey();

        if (isNull(findWallet(pk))) {
          log.debug("Inserting into db wallet {}", walletEntity.getWallet().getFullWalletAddress());
          em.getTransaction().begin();
          em.persist(walletEntity);
          em.getTransaction().commit();
          log.debug("Wallet inserted into db, {}", walletEntity);
        } else {
          log.debug("Wallet {} already exists.", walletEntity.getWallet().getFullWalletAddress());
        }
      } catch (PersistenceException e) {
        if (e.getMessage().contains(TOO_MANY_PERSISTENT_OBJECTS_1000000)) {
          spawnNewDb();
          insertWallet(walletEntity); // repeat failed insert into newly spawned db
        } else {
          log.error("Error inserting wallet into DB. Error: {}", e.getMessage());
        }
      } catch (Exception e) {
        log.error("Error inserting wallet into DB. Error: {}", e.getMessage());
      } finally {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
      }
    }
  }

  public long getNumberOfWalletsFromAllDBsSync() {
    long result = 0;
    for (Map.Entry<String, String> entry : poolInSettings.entrySet()) {

      DB2 db = new DB2(entry.getKey());
      EntityManager em = db.getEmf().createEntityManager();
      try {
        long found =
            em.createQuery("SELECT count(t) FROM WalletEntity t", Long.class).getSingleResult();
        log.debug("{} wallets in DB {}", found, entry.getKey());
        result = result + found;
      } catch (Exception e) {
        log.error("Error {}", e.getMessage());
      } finally {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
        if (em.isOpen()) em.close();
      }
    }
    return result;
  }

  public long getNumberOfWalletsFromAllDBsAsync() {
    long result = 0;
    try {
      List<WalletsCountCallable> callablesList = new ArrayList<>();
      for (Map.Entry<String, String> entry : poolInSettings.entrySet()) {
        WalletsCountCallable callable = new WalletsCountCallable(entry.getKey());
        callablesList.add(callable);
      }

      List<Future<String>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<String> future : futures) {
        result = result + Long.parseLong(future.get());
      }

      return result;
    } catch (Exception e) {
      log.error("Error getNumberOfWalletsFromAllDBsAsync(), {}", e.getMessage());
      return result;
    }
  }

  // blocks
  public List<BlockEntity> loadBlocksBefore(long datetimeFrom) {

    List<BlockEntity> result = new ArrayList<>();
    try {
      List<FindBlocksCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        FindBlocksCallable callable =
            new FindBlocksCallable(
                BlockCallbackParam.builder().db(db).datetimeFrom(datetimeFrom).build());
        callablesList.add(callable);
      }

      List<Future<BlockCallbackParam>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<BlockCallbackParam> future : futures) {
        List<BlockEntity> block = future.get().getFoundBlocks();
        if (nonNull(block)) {
          result = block;
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error loadBlocksBefore(), {}", e.getMessage());
      return null;
    }
  }

  // txs
  public List<TxEntity> loadTxsBefore(long datetimeFrom) {
    List<TxEntity> result = null;
    try {
      List<FindTxsCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        FindTxsCallable callable =
            new FindTxsCallable(
                TxCallbackParam.builder().db(db).datetimeFrom(datetimeFrom).build());
        callablesList.add(callable);
      }

      List<Future<TxCallbackParam>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<TxCallbackParam> future : futures) {
        List<TxEntity> tx = future.get().getFoundTxs();
        if (nonNull(tx)) {
          result = tx;
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error loadTxsBefore(), {}", e.getMessage());
      return null;
    }
  }

  public void updateWalletStateAndSeqno(
      WalletEntity walletEntity, Account accountState, long seqno, WalletVersion walletVersion) {
    //    log.debug("updating account state in db");
    try {

      List<UpdateAccountStateCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        UpdateAccountStateCallable callable =
            new UpdateAccountStateCallable(
                WalletCallbackParam.builder()
                    .db(db)
                    .walletPk(walletEntity.getPrimaryKey())
                    .accountState(accountState) // todo review
                    .seqno(seqno)
                    .walletVersion(walletVersion)
                    .build());
        callablesList.add(callable);
      }

      ForkJoinPool.commonPool().invokeAll(callablesList);

    } catch (Exception e) {
      log.error("Error updating account's state, {}", e.getMessage());
    }
  }

  public List<WalletEntity> getAllWallets() {
    List<WalletEntity> result = new ArrayList<>();
    try {
      List<GetWalletsCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        GetWalletsCallable callable =
            new GetWalletsCallable(WalletCallbackParam.builder().db(db).build());
        callablesList.add(callable);
      }

      List<Future<WalletCallbackParam>> futures =
          ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<WalletCallbackParam> future : futures) {
        List<WalletEntity> wallets = future.get().getFoundWallets();
        if (!wallets.isEmpty()) {
          result.addAll(wallets);
        }
      }

      return result;
    } catch (Exception e) {
      // log.error("Error getAllWallets(), {}" + e.getMessage());
      return new ArrayList<>();
    }
  }

  public void deleteWallet(WalletPk walletPk) {
    try {
      List<DeleteWalletCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        DeleteWalletCallable callable =
            new DeleteWalletCallable(
                WalletCallbackParam.builder().db(db).walletPk(walletPk).build());
        callablesList.add(callable);
      }

      ForkJoinPool.commonPool().invokeAll(callablesList);
    } catch (Exception e) {
      log.error("Error deleteWallet(), {}", e.getMessage());
    }
  }

  public long existsMainWallet() {
    long result = 0;
    try {
      List<MainWalletExistsCallable> callablesList = new ArrayList<>();
      for (Map.Entry<String, String> entry : poolInSettings.entrySet()) {
        MainWalletExistsCallable callable = new MainWalletExistsCallable(entry.getKey());
        callablesList.add(callable);
      }

      List<Future<String>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<String> future : futures) {
        result = result + Long.parseLong(future.get());
      }

      return result;
    } catch (Exception e) {
      log.error("Error existsMainWallet(), {}", e.getMessage());
      return result;
    }
  }

  public long existsConfigWallet() {
    long result = 0;
    try {
      List<ConfigWalletExistsCallable> callablesList = new ArrayList<>();
      for (Map.Entry<String, String> entry : poolInSettings.entrySet()) {
        ConfigWalletExistsCallable callable = new ConfigWalletExistsCallable(entry.getKey());
        callablesList.add(callable);
      }

      List<Future<String>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<String> future : futures) {
        result = result + Long.parseLong(future.get());
      }
      return result;
    } catch (Exception e) {
      log.error("Error existsConfigWallet(), {}", e.getMessage());
      return result;
    }
  }

  public List<BlockEntity> searchBlocks(String wcShardSeqnoHash) {
    log.debug("searchBlocks, query {}", wcShardSeqnoHash);
    if (wcShardSeqnoHash.isEmpty()) {
      return List.of();
    }

    List<BlockEntity> result = new ArrayList<>();

    try {
      List<SearchBlocksCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        SearchBlocksCallable callable =
            new SearchBlocksCallable(
                BlockCallbackParam.builder().db(db).searchText(wcShardSeqnoHash).build());
        callablesList.add(callable);
      }

      List<Future<BlockCallbackParam>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<BlockCallbackParam> future : futures) {
        List<BlockEntity> blocks = future.get().getFoundBlocks();
        if (!blocks.isEmpty()) {
          result.addAll(blocks);
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error searchBlocks(), {}", e.getMessage());
      return result;
    }
  }

  public List<WalletEntity> searchAccounts(String wcShardSeqnoHash) {
    log.debug("searchAccounts, query {}", wcShardSeqnoHash);
    if (wcShardSeqnoHash.isEmpty()) {
      return List.of();
    }

    List<WalletEntity> result = new ArrayList<>();

    try {
      List<SearchWalletsCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        SearchWalletsCallable callable =
            new SearchWalletsCallable(
                WalletCallbackParam.builder().db(db).searchText(wcShardSeqnoHash).build());
        callablesList.add(callable);
      }

      List<Future<WalletCallbackParam>> futures =
          ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<WalletCallbackParam> future : futures) {
        List<WalletEntity> wallets = future.get().getFoundWallets();
        if (!wallets.isEmpty()) {
          result.addAll(wallets);
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error searchAccounts(), {}", e.getMessage());
      return result;
    }
  }

  public List<TxEntity> searchTxs(String wcShardSeqnoHash) {
    log.debug("searchTxs, query {}", wcShardSeqnoHash);
    if (wcShardSeqnoHash.isEmpty()) {
      return List.of();
    }

    List<TxEntity> result = new ArrayList<>();

    try {
      List<SearchTxsCallable> callablesList = new ArrayList<>();
      for (DB2 db : allDBs) {
        SearchTxsCallable callable =
            new SearchTxsCallable(
                TxCallbackParam.builder().db(db).searchText(wcShardSeqnoHash).build());
        callablesList.add(callable);
      }

      List<Future<TxCallbackParam>> futures = ForkJoinPool.commonPool().invokeAll(callablesList);

      for (Future<TxCallbackParam> future : futures) {
        List<TxEntity> wallets = future.get().getFoundTxs();
        if (!wallets.isEmpty()) {
          result.addAll(wallets);
        }
      }

      return result;
    } catch (Exception e) {
      log.error("Error searchTxs(), {}", e.getMessage());
      return result;
    }
  }

  public void closeDbs() {
    log.info("Closing database...");
    for (DB2 db : allDBs) {
      db.getEmf().close();
    }
  }
}
