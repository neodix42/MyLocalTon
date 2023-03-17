package org.ton.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.tonlib.types.RawAccountState;
import org.ton.settings.MyLocalTonSettings;
import org.ton.utils.Extractor;
import org.ton.wallet.WalletAddress;

import javax.persistence.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.ton.settings.MyLocalTonSettings.MY_LOCAL_TON;

@Slf4j
@RunWith(JUnit4.class)
public class ObjectDbTest {

    protected static final String CURRENT_DIR = System.getProperty("user.dir");
    protected static final String DB_DIR = CURRENT_DIR + File.separator + "testDB" + File.separator;
    protected static final String DB_SETTINGS_CONF = "objectsdb.conf";
    protected static final String SETTINGS_FILE = "settings.json";

    private static EntityManagerFactory emf;

    @BeforeClass
    public static void executedBeforeClass() throws IOException {
        System.setProperty("objectdb.home", DB_DIR);
        System.setProperty("objectdb.conf", DB_DIR + DB_SETTINGS_CONF);
        Files.createDirectories(Paths.get(DB_DIR));
        Files.createDirectories(Paths.get(CURRENT_DIR + File.separator + MY_LOCAL_TON));

        InputStream dbConfig = Extractor.class.getClassLoader().getResourceAsStream("org/ton/db/objectsdb.conf");
        Files.copy(dbConfig, Paths.get(DB_DIR + DB_SETTINGS_CONF), StandardCopyOption.REPLACE_EXISTING);
        dbConfig.close();

        emf = Persistence.createEntityManagerFactory("objectdb:" + DB_DIR + "myLocalTon.odb");
    }

    /**
     * Warning! Generates around 2.5 GB of test data on disk.
     */
    @Test
    public void testLimits() {
        String walletToFind = null;
        String randomAddress;
        try {
            for (int i = 0; i < 1200000; i++) {
                EntityManager em = emf.createEntityManager();
                randomAddress = UUID.randomUUID().toString();
                WalletEntity walletEntity = WalletEntity.builder()
                        .wallet(WalletAddress.builder().hexWalletAddress("KJNDFKJYTUYGJ871623981y23DAJDFNAKLKJ981238123").wc(1L).build())
                        .walletVersion(WalletVersion.V3R2)
                        .configWalletInstalled(false)
                        .createdAt(234243L)
                        .wc(0L)
                        .hexAddress(randomAddress)
                        .accountState(RawAccountState.builder().balance("10").build())
                        .build();

                em.getTransaction().begin();
                em.persist(walletEntity);
                em.getTransaction().commit();

                if ((i % 50000) == 0) {
                    log.info("index {}", i);
                    walletToFind = randomAddress;
                    //em.flush();
                    //em.clear();
                }

                if (i > 1048500) {
                    log.info("index {}", i);
                }
                if (em.isOpen())
                    em.close();
            }
        } catch (PersistenceException e) {

            log.error("error {}", e.getMessage());
            EntityManager em = emf.createEntityManager();
            TypedQuery<Long> query = em.createQuery("SELECT count(t) FROM WalletEntity t", Long.class);
            long count = query.getSingleResult();
            log.info("count {}", count); // 1048575, 151420 , 151425 (not lost)
            assertTrue(e.getMessage().contains("Too many persistent objects (>1000000)"));
        }

        //test search performance
        log.info("searching wallet {}", walletToFind);
        EntityManager em = emf.createEntityManager();

        try {
            WalletEntity foundWallet = em.find(WalletEntity.class, WalletPk.builder()
                    .hexAddress(walletToFind)
                    .wc(0L).build());
            log.info("found wallet {}", foundWallet);
        } finally {
            if (em.isOpen())
                em.close();
        }
    }

    @Test
    public void testMultiThreading() throws InterruptedException {

        for (int i = 0; i < 100; i++) {
            int finalI = i;
            Thread t = new Thread(() -> {
                Thread.currentThread().setName("TestThread " + finalI);
                EntityManager em = emf.createEntityManager();

                WalletEntity walletEntity = WalletEntity.builder()
                        .wc(0L)
                        .hexAddress(UUID.randomUUID().toString())
                        .wallet(WalletAddress.builder().hexWalletAddress("KJNDFKJYTUYGJ871623981y23DAJDFNAKLKJ981238123").wc(1L).build())
                        .build();

                em.getTransaction().begin();
                em.persist(walletEntity);
                em.getTransaction().commit();
                log.info("inserted " + finalI);
                em.close();
            });
            t.start();
        }

        Thread.sleep(5000);
        EntityManager em = emf.createEntityManager();
        TypedQuery<Long> query = em.createQuery("SELECT count(t) FROM WalletEntity t", Long.class);
        long count = query.getSingleResult();
        log.info("count {}", count);
        assertEquals(100, count);
        em.close();
    }

    @Test
    public void testMultiThreadingViaPool() throws InterruptedException {

        MyLocalTonSettings settings = loadSettings();
        MyLocalTonSettings.DB_DIR = DB_DIR;
        saveSettings(settings);

        DbPool pool = new DbPool(settings);

        for (int i = 0; i < 100; i++) {
            int finalI = i;
            Thread t = new Thread(() -> {
                Thread.currentThread().setName("TestThread " + finalI);
                EntityManager em = emf.createEntityManager();

                WalletEntity walletEntity = WalletEntity.builder()
                        .wc(0L)
                        .hexAddress(UUID.randomUUID().toString())
                        .wallet(WalletAddress.builder().hexWalletAddress("KJNDFKJYTUYGJ871623981y23DAJDFNAKLKJ981238123").wc(1L).build())
                        .build();

                pool.insertWallet(walletEntity);
                log.info("inserted" + finalI);

            });
            t.start();
        }

        Thread.sleep(5000);
        long count = pool.getNumberOfWalletsFromAllDBsAsync();
        log.info("total number of wallets {}", count);
        assertEquals(100, count);
        count = pool.getNumberOfWalletsFromAllDBsSync();
        log.info("total number of wallets {}", count);
        assertEquals(100, count);
    }

    @Test
    public void testDbPoolOvercomesLimit() throws InterruptedException {

        MyLocalTonSettings settings = loadSettings();
        MyLocalTonSettings.DB_DIR = DB_DIR;
        saveSettings(settings);

        DbPool pool = new DbPool(settings);

        log.info("Active DB {}", pool.activeDB.getDbFileName());

        long start = System.currentTimeMillis();

        CountDownLatch latch = new CountDownLatch(50);

        for (int i = 0; i < 50; i++) {
            int finalI = i;
            Thread t = new Thread(() -> {
                Thread.currentThread().setName("TestThread " + finalI);

                for (int j = 0; j < 30000; j++) {
                    String hexAddr = UUID.randomUUID().toString();
                    WalletEntity walletEntity = WalletEntity.builder()
                            .wallet(WalletAddress.builder().hexWalletAddress("13413ACD13413ACD").wc(0L).fullWalletAddress("0:13413ACD13413ACD").build())
                            .wc(0L)
                            .hexAddress(hexAddr)
                            .build();

                    pool.insertWallet(walletEntity);

                    BlockEntity blockEntity = BlockEntity.builder()
                            .createdAt(System.currentTimeMillis())
                            .wc(0L)
                            .seqno(BigInteger.ONE)
                            .shard(UUID.randomUUID().toString())
                            .build();

                    pool.insertBlock(blockEntity);

                    TxEntity txEntity = TxEntity.builder()
                            .createdAt(System.currentTimeMillis())
                            .wc(0L)
                            .seqno(BigInteger.ONE)
                            .shard(UUID.randomUUID().toString())
                            .txHash(UUID.randomUUID().toString())
                            .typeTx("Tx")
                            .typeMsg("Tick")
                            .accountAddress("13413ACD13413ACD")
                            .txLt(BigInteger.TWO)
                            .build();

                    pool.insertTx(txEntity);

                    if ((j != 0) && (j % 20000) == 0) {
                        log.info("index {}, walletToFind {}, blockToFind {}, txToFind {}", j, hexAddr, blockEntity, txEntity);
                    }
                }
                log.info("TestThread {}, done ", finalI);
                latch.countDown();
            });
            t.start();
        }

        latch.await();

        log.info("elapsed {} ", System.currentTimeMillis() - start);

        long count = pool.getNumberOfWalletsFromAllDBsAsync();
        log.info("total number of wallets {}", count);
        assertEquals(50 * 30000, count);
    }

    @Test
    public void testExceptionVsFindPerformance() {

        MyLocalTonSettings settings = loadSettings();
        MyLocalTonSettings.DB_DIR = DB_DIR;
        saveSettings(settings);

        DbPool pool = new DbPool(settings);

        log.info("Active DB {}", pool.activeDB.getDbFileName());

        long start = System.currentTimeMillis();
        String hexAddr = UUID.randomUUID().toString();
        for (int j = 0; j < 10000; j++) {

            if ((j % 5000) != 0) { // every 5th record make duplicate
                hexAddr = UUID.randomUUID().toString();
            }
            WalletEntity walletEntity = WalletEntity.builder()
                    .wallet(WalletAddress.builder().hexWalletAddress("13413ACD13413ACD").wc(0L).fullWalletAddress("0:13413ACD13413ACD").build())
                    .wc(0L)
                    .hexAddress(hexAddr)
                    .build();

            pool.insertWallet(walletEntity);
            if ((j != 0) && (j % 5000) == 0) {
                log.info("index {}", j);
            }
        }
        log.info("elapsed {} ", System.currentTimeMillis() - start);

        long totalWalletsSync = pool.getNumberOfWalletsFromAllDBsSync();
        log.info("totalWallets (sync) {}", totalWalletsSync);

        long totalWalletsAsync = pool.getNumberOfWalletsFromAllDBsAsync();
        log.info("totalWallets (async) {}", totalWalletsAsync);
    }

    @Test
    public void testFind() {
        //given
        MyLocalTonSettings settings = loadSettings();
        MyLocalTonSettings.DB_DIR = DB_DIR;
        Map<String, String> dbs = new HashMap<>();
        dbs.put("25e6a777-79cd-4fa9-9bd5-cd6e1129ccb9", "ACTIVE");
        dbs.put("e5ce3827-c6e6-425c-aa7f-e175cdc4aca9", "FILLED");
        dbs.put("78b9e783-96ea-4b2f-bbec-c11477b2b577", "FILLED");
        settings.setDbPool(dbs);
        saveSettings(settings);

        DbPool pool = new DbPool(settings);
        WalletPk walletToFind = WalletPk.builder()
                .wc(0L)
                .hexAddress("57fda72c-7b52-4b54-aac9-42a0ced9c6de")
                .build();
        WalletEntity foundWallet = pool.findWallet(walletToFind);
        log.info("found wallet {}", foundWallet);
        assertNotNull(foundWallet);
    }

    @Test
    public void testNumberOfWalletsSyncVsAsync() {
        //given
        MyLocalTonSettings settings = loadSettings();
        MyLocalTonSettings.DB_DIR = DB_DIR;
        Map<String, String> dbs = new HashMap<>();
        dbs.put("0d6645a2-468a-46e9-b804-525bdc3bbc83", "ACTIVE");
        dbs.put("fe55a5cd-3804-4775-9dc4-cc8213193ff4", "FILLED");
        settings.setDbPool(dbs);
        saveSettings(settings);

        DbPool pool = new DbPool(settings);

        log.info("Active DB {}", pool.activeDB.getDbFileName());

        //when
        long start = System.currentTimeMillis();
        long totalWalletsSync = pool.getNumberOfWalletsFromAllDBsSync();
        long elapsedSyncTime = System.currentTimeMillis() - start;
        log.info("elapsed (sync) {} ", elapsedSyncTime);
        log.info("totalWallets (sync) {}", totalWalletsSync);

        start = System.currentTimeMillis();
        long totalWalletsAsync = pool.getNumberOfWalletsFromAllDBsAsync();
        long elapsedAsyncTime = System.currentTimeMillis() - start;
        log.info("elapsed (async) {} ", elapsedSyncTime);
        log.info("totalWallets (async) {}", totalWalletsAsync);

        //then
        assertEquals(totalWalletsAsync, totalWalletsSync);
        assertThat(elapsedSyncTime).isGreaterThan(elapsedAsyncTime);
    }

    private MyLocalTonSettings loadSettings() {
        try {
            if (Files.exists(Paths.get(DB_DIR + SETTINGS_FILE), LinkOption.NOFOLLOW_LINKS)) {
                return new Gson().fromJson(new FileReader(new File(DB_DIR + SETTINGS_FILE)), MyLocalTonSettings.class);
            } else {
                log.debug("No settings.json found. Very first launch with default settings.");
                return new MyLocalTonSettings();
            }
        } catch (Exception e) {
            log.error("Can't load settings file: {}, error {}", DB_DIR + SETTINGS_FILE, e.getMessage());
            return null;
        }
    }

    private void saveSettings(MyLocalTonSettings settings) {
        try {
            String abJson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(settings);
            FileUtils.writeStringToFile(new File(DB_DIR + SETTINGS_FILE), abJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void executedAfterClass() {
        emf.close();
        //FileUtils.deleteDirectory(new File(DB_DIR));
    }
}
