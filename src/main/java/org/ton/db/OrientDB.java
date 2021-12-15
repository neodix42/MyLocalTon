package org.ton.db;

import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.object.ODatabaseObject;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.object.db.OrientDBObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.db.entities.*;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.executors.liteclient.api.block.Block;
import org.ton.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
// in OrientDB sql statements are case-sensitive
// in update statement where clause constructed by concat, not map
public class OrientDB {
    public static final String SEQNO = "seqno";
    public static final String SHARD = "shard";
    public static final String WC = "wc";
    public static final String HEX_ADDR = "hexAddr";
    public static final String HASH = "hash";
    public static final String DB_NAME = "MyLocalTonDb";

    private OrientDB() {
    }

    private static ODatabaseObject db;
    private static OrientDBObject orientDb;

    static {

        System.setProperty("log.console.level", "INFO");
        System.setProperty("log.file.level", "INFO");
        System.setProperty("security.createDefaultUsers", "true");
        System.setProperty("environment.concurrent", "true");
        System.setProperty("environment.dumpCfgAtStartup", "false"); // show db configuration on start
        System.setProperty("memory.chunk.size", "524288000");
        System.setProperty("storage.openFiles.limit", "5000");
        System.setProperty("storage.openFiles.limit", "5000");
        System.setProperty("storage.wal.cacheSize", "3000"); // if 0 - disables cache
        System.setProperty("storage.wal.maxSize", "102400");
        System.setProperty("storage.diskCache.bufferSize", "1024"); // max used memory
        System.setProperty("db.custom.support", "true"); // ?

        orientDb = new OrientDBObject("embedded:./myLocalTon/databases/", OrientDBConfig.defaultConfig());

        if (!orientDb.exists(DB_NAME)) {
            orientDb.create(DB_NAME, ODatabaseType.PLOCAL);
            log.info("{} created", DB_NAME);

            db = orientDb.open(DB_NAME, "admin", "admin");

            log.info("{} opened", DB_NAME);
            db.getEntityManager().registerEntityClasses("org.ton.db.entities");
            db.getEntityManager().registerEntityClasses("org.ton.wallet");
            db.getEntityManager().registerEntityClasses("org.ton.executors.liteclient.api");
            db.getEntityManager().registerEntityClasses("org.ton.executors.liteclient.api.block");
            log.info("{} entities registered", DB_NAME);

            OClass object = db.getMetadata().getSchema().getClass(("BlockEntity"));
            object.createProperty("createdAt", OType.LONG);
            object.createProperty("seqno", OType.STRING);
            object.createProperty("wc", OType.LONG);
            object.createProperty("shard", OType.STRING);
//            object.createIndex("block_index", OClass.INDEX_TYPE.UNIQUE, "createdAt", "seqno", "wc", "shard");

            object = db.getMetadata().getSchema().getClass(("WalletEntity"));
            object.createProperty("wc", OType.LONG);
            object.createProperty("hexAddress", OType.STRING);
//            object.createIndex("wallet_index", OClass.INDEX_TYPE.UNIQUE, "wc", "hexAddress");

            object = db.getMetadata().getSchema().getClass(("TxEntity"));
            object.createProperty("createdAt", OType.LONG);
            object.createProperty("seqno", OType.STRING);
            object.createProperty("wc", OType.LONG);
            object.createProperty("shard", OType.STRING);
            object.createProperty("txHash", OType.STRING);
            object.createProperty("typeTx", OType.STRING);
            object.createProperty("typeMsg", OType.STRING);
            object.createProperty("accountAddress", OType.STRING);
            object.createProperty("txLt", OType.STRING);
//            object.createIndex("tx_index", OClass.INDEX_TYPE.UNIQUE, "createdAt", "seqno", "wc", "shard", "txHash", "typeTx", "typeMsg", "accountAddress", "txLt");

            log.info("{} indexes created", DB_NAME);

        } else {
            db = orientDb.open(DB_NAME, "admin", "admin");
            log.info("{} opened", DB_NAME);
            db.getEntityManager().registerEntityClasses("org.ton.db.entities");
            db.getEntityManager().registerEntityClasses("org.ton.wallet");
            db.getEntityManager().registerEntityClasses("org.ton.executors.liteclient.api");
            db.getEntityManager().registerEntityClasses("org.ton.executors.liteclient.api.block");
            log.info("{} entities registered", DB_NAME);
        }
    }

    public static ODatabaseObject getDB() {
        return db;
    }

    public static OrientDBObject getOrientDB() {
        return orientDb;
    }

    // find
    public static BlockEntity findBlock(BlockPk blockPk) {

        String query = "SELECT FROM BlockEntity WHERE " +
                "createdAt = " + blockPk.getCreatedAt() + " AND " +
                "seqno = '" + blockPk.getSeqno() + "' AND " +
                "wc = " + blockPk.getWc() + " AND " +
                "shard = '" + blockPk.getShard() + "'";
        log.debug("find block query {}", query);
        List<BlockEntity> result = db.objectQuery(query);
        if (result.isEmpty()) {
            return null;
        } else {
            return db.detachAll(result.get(0), true);
        }
    }

    public static TxEntity findTx(TxPk txPk) {

        String query = "SELECT FROM TxEntity WHERE " +
                "createdAt = " + txPk.getCreatedAt() + " AND " +
                "seqno = '" + txPk.getSeqno() + "' AND " +
                "wc = " + txPk.getWc() + " AND " +
                "shard = '" + txPk.getShard() + "' AND " +
                "txHash = '" + txPk.getTxHash() + "' AND " +
                "typeTx = '" + txPk.getTypeTx() + "' AND " +
                "typeMsg = '" + txPk.getTypeMsg() + "' AND " +
                "accountAddress = '" + txPk.getAccountAddress() + "' AND " +
                "txLt = '" + txPk.getTxLt() + "'";
        log.debug("find tx query {}", query);
        List<TxEntity> result = db.objectQuery(query);
        if (result.isEmpty()) {
            return null;
        } else {
            return db.detachAll(result.get(0), true);
        }
    }

    public static WalletEntity findWallet(WalletPk walletPk) {

        String query = "SELECT FROM WalletEntity WHERE " +
                "wc = " + walletPk.getWc() + " AND " +
                "hexAddress = '" + walletPk.getHexAddress() + "'";
        log.debug("find wallet query {}", query);
        List<WalletEntity> result = db.objectQuery(query);
        if (result.isEmpty()) {
            return null;
        } else {
            return db.detachAll(result.get(0), true);
        }
    }

    //delete
    public static void deleteWallet(WalletPk walletPk) {
        String query = "DELETE FROM WalletEntity WHERE " +
                "wc = " + walletPk.getWc() + " AND " +
                "hexAddress = '" + walletPk.getHexAddress() + "'";
        log.debug("delete wallet stmt {}", query);
        OResultSet rs = db.command(query);
        rs.close();
    }

    public static void deleteBlock(BlockPk blockPk) {
        String query = "DELETE FROM BlockEntity WHERE " +
                "createdAt = " + blockPk.getCreatedAt() + " AND " +
                "seqno = '" + blockPk.getSeqno() + "' AND " +
                "wc = " + blockPk.getWc() + " AND " +
                "shard = '" + blockPk.getShard() + "'";
        log.debug("delete block stmt {}", query);
        OResultSet rs = db.command(query);
        db.commit();
        rs.close();
    }

    // insert
    public static synchronized void insertTx(TxEntity tx) { // was synchronized
        if (isNull(findTx(tx.getPrimaryKey()))) {
            db.save(tx);
        }
    }

    public static synchronized void insertBlock(BlockEntity block) {
        if (isNull(findBlock(block.getPrimaryKey()))) {
            db.save(block);
        }
    }

    public static synchronized void insertWallet(WalletEntity wallet) {
        if (isNull(findWallet(wallet.getPrimaryKey()))) {
            db.save(wallet);
        }
    }

    public static void updateWalletState(WalletEntity walletEntity, AccountState accountState) { // was synchronized
        log.debug("updating account state, {},  {}", walletEntity.getFullAddress(), accountState);

        String update = "UPDATE WalletEntity SET " +
                "accountState = :accountState " +
                "WHERE " +
                "wc = " + walletEntity.getWc() + " AND " +
                "hexAddress = '" + walletEntity.getHexAddress() + "'";

        Map<String, Object> params = new HashMap<>();
        params.put("accountState", accountState);
        //params.put("wc", walletEntity.getWc());
        //params.put("hexAddress", walletEntity.getHexAddress());

        if ((!accountState.getStateCode().isEmpty()) && (!accountState.getStateData().isEmpty())) {
            update = "UPDATE WalletEntity SET " +
                    "accountState = :accountState, " +
                    "walletVersion = :walletVersion, " +
                    "wallet.subWalletId = :subWalletId " +
                    "WHERE " +
                    "wc = " + walletEntity.getWc() + " AND " +
                    "hexAddress = '" + walletEntity.getHexAddress() + "'";
            Pair<String, Long> walletVersionAndId = Utils.detectWalledVersionAndId(accountState);
            params.put("walletVersion", walletVersionAndId.getLeft());
            params.put("subWalletId", walletVersionAndId.getRight());
        }

        log.debug("update wallet stmt {}", update);

        OResultSet rs = db.command(update, params);
        db.commit();
        rs.close();
    }

    public static void updateBlockDump(BlockPk blockPk, Block newBlock) {
        String update = "UPDATE BlockEntity SET " +
                "block = :block " +
                "WHERE " +
                "createdAt = " + blockPk.getCreatedAt() + " AND " +
                "seqno = '" + blockPk.getSeqno() + "' AND " +
                "wc = " + blockPk.getWc() + " AND " +
                "shard = '" + blockPk.getShard() + "'";

        log.debug("update block stmt {}", update);
        Map<String, Object> params = new HashMap<>();
        params.put("block", newBlock);
        OResultSet rs = db.command(update, params);
        db.commit();
        rs.close();
    }

//    public static List<BlockEntity> getAllBlocks() {
//
//        List<BlockEntity> result = new ArrayList<>();
//
//        for (BlockEntity block : db.browseClass(BlockEntity.class)) {
//            result.add(block);
//        }
//        return result;
//    }

    public static List<BlockEntity> getAllBlocks() {
        log.debug("query blocks stmt SELECT FROM BlockEntity ORDER BY createdAt desc");
        return db.objectQuery("SELECT FROM BlockEntity ORDER BY createdAt desc"); // works
    }

    public static List<WalletEntity> getAllWallets() {
        log.debug("query wallets stmt {}", "SELECT FROM WalletEntity ORDER BY createdAt ASC");
        return db.objectQuery("SELECT FROM WalletEntity ORDER BY createdAt ASC"); // works
    }

    public static List<TxEntity> getAllTxs() {
        log.debug("query txs stmt {}", "SELECT FROM TxEntity ORDER BY createdAt ASC");
        return db.objectQuery("SELECT FROM TxEntity ORDER BY createdAt ASC"); // works
    }

    public static long getNumberOfPreinstalledWallets() {
        log.debug("query wallets stmt {}", "SELECT FROM WalletEntity WHERE preinstalled = true");
        List<WalletEntity> result = db.objectQuery("SELECT FROM WalletEntity WHERE preinstalled = true");

        return result.isEmpty() ? 0L : result.size(); // works
    }

    public static boolean existsMainWallet() {
        try {
            String query = "SELECT FROM WalletEntity WHERE mainWalletInstalled = true";
            return !db.objectQuery(query).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean existsConfigWallet() {
        try {
            String query = "SELECT FROM WalletEntity WHERE configWalletInstalled = true";
            return !db.objectQuery(query).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // find multiple
    // blocks
    public static List<BlockEntity> loadBlocksBefore(long datetimeFrom) {
        String query = "SELECT FROM BlockEntity WHERE createdAt < " + datetimeFrom + " ORDER BY createdAt DESC";
        log.info("query blocks stmt {}", query);
        List<BlockEntity> result = db.objectQuery(query);
        if (result.isEmpty()) {
            return new ArrayList<>();
        } else {
            log.info("loadBlocksBefore result.size {}", result.size());
            return result;
        }
    }

    public static List<TxEntity> loadTxsBefore(long datetimeFrom) {
        String query = "SELECT FROM TxEntity WHERE createdAt < " + datetimeFrom + " ORDER BY createdAt DESC";
        log.debug("query blocks stmt {}", query);
        List<TxEntity> result = db.objectQuery(query);
        if (result.isEmpty()) {
            return new ArrayList<>();
        } else {
            log.info("loadTxsBefore result.size {}", result.size());
            return result;
        }
    }

    //search functions
    public static List<BlockEntity> searchBlocks(String wcShardSeqnoHash) {

        if (wcShardSeqnoHash.length() == 0) {
            return List.of();
        }

        log.debug("searchBlocks, query {}", wcShardSeqnoHash);

        Long seqno;
        String shard;
        Long wc;
        String hash;
        String query;

        if ((wcShardSeqnoHash.charAt(0) == '(') && (wcShardSeqnoHash.charAt(wcShardSeqnoHash.length() - 1) == ')')) {
            String[] s = wcShardSeqnoHash.substring(1, wcShardSeqnoHash.length() - 1).split(",");
            wc = Utils.parseLong(s[0]);
            shard = s[1];
            seqno = Utils.parseLong(s[2]);
            query = "SELECT FROM BlockEntity WHERE (seqno = '" + seqno + "') AND (shard = '" + shard + "') AND (wc = " + wc + ") ORDER BY createdAt DESC";
        } else if (wcShardSeqnoHash.length() == 64) { // hashes
            hash = wcShardSeqnoHash;
            query = "SELECT FROM BlockEntity WHERE (roothash = '" + hash + "') OR (filehash = '" + hash + "') ORDER BY createdAt DESC";

        } else {
            seqno = Utils.parseLong(wcShardSeqnoHash);
            shard = wcShardSeqnoHash;
            wc = Utils.parseLong(wcShardSeqnoHash);
            query = "SELECT FROM BlockEntity WHERE (seqno = '" + seqno + "') OR (shard = '" + shard + "') OR (wc = " + wc + ") ORDER BY createdAt DESC";
        }

        List<BlockEntity> result = db.objectQuery(query);

        if (result.isEmpty()) {
            log.info("no blocks found");
            return new ArrayList<>();
        } else {
            log.info("found blocks {}", result.size());
            return result;
        }
    }

    public static List<TxEntity> searchTxs(String wcShardSeqnoHash) {
        log.debug("searchTxs, query {}", wcShardSeqnoHash);
        if (wcShardSeqnoHash.length() == 0) {
            return List.of();
        }

        Long seqno;
        String shard;
        Long wc;
        String hash;
        String hexAddr;
        String query = "";

        if ((wcShardSeqnoHash.charAt(0) == '(') && (wcShardSeqnoHash.charAt(wcShardSeqnoHash.length() - 1) == ')')) {
            String[] s = wcShardSeqnoHash.substring(1, wcShardSeqnoHash.length() - 1).split(",");
            wc = Utils.parseLong(s[0]);
            shard = s[1];
            seqno = Utils.parseLong(s[2]);
            query = "SELECT FROM TxEntity WHERE (seqno = '" + seqno + "') AND (shard = '" + shard + "') AND (wc = " + wc + ") ORDER BY createdAt DESC";

        } else if (wcShardSeqnoHash.length() == 64) { //tx hash or src/dest addr
            hash = wcShardSeqnoHash;
            query = "SELECT FROM TxEntity WHERE (txHash = '" + hash + "') OR (fromForSearch = '" + hash + "') OR (toForSearch = '" + hash + "') ORDER BY createdAt DESC";
        } else if (wcShardSeqnoHash.length() > 64) { // wc:addr
            String[] s = wcShardSeqnoHash.split(":");
            if (s.length == 2) {
                wc = Utils.parseLong(s[0]);
                hexAddr = s[1];
                query = "SELECT FROM TxEntity WHERE (txHash = '" + hexAddr + "') OR (fromForSearch = '" + hexAddr + "') OR (toForSearch = '" + hexAddr + "') ORDER BY createdAt DESC";
            }
        } else {
            seqno = Utils.parseLong(wcShardSeqnoHash);
            shard = wcShardSeqnoHash;
            wc = Utils.parseLong(wcShardSeqnoHash);
            query = "SELECT FROM TxEntity WHERE (seqno =  '" + seqno + "') OR (shard = '" + shard + "') OR (wc = " + wc + ") ORDER BY createdAt DESC";
        }

        if (query.equals("")) {
            return new ArrayList<>();
        }

        List<TxEntity> result = db.objectQuery(query);

        if (result.isEmpty()) {
            log.info("no txs found");
            return new ArrayList<>();
        } else {
            log.info("found txs {}", result.size());
            return result;
        }
    }

    public static List<WalletEntity> searchAccounts(String wcShardSeqnoHash) {
        log.debug("searchAccounts, query {}", wcShardSeqnoHash);
        if (wcShardSeqnoHash.length() == 0) {
            return List.of();
        }

        Long wc;
        String hexAddr;
        String query = "";

        if (wcShardSeqnoHash.length() == 64) {
            hexAddr = wcShardSeqnoHash;
            query = "SELECT FROM WalletEntity WHERE (hexAddress = '" + hexAddr + "')";
        } else if (wcShardSeqnoHash.length() > 64) { // wc:addr
            String[] s = wcShardSeqnoHash.split(":");
            if (s.length == 2) {
                wc = Utils.parseLong(s[0]);
                hexAddr = s[1];
                query = "SELECT FROM WalletEntity WHERE (wc = '" + wc + "') AND (hexAddress = '" + hexAddr + "')";
            }
        } else if (wcShardSeqnoHash.length() == 48) { // base64 addr
            wcShardSeqnoHash = Utils.friendlyAddrToHex(wcShardSeqnoHash);
            String[] s = wcShardSeqnoHash.split(":");
            if (s.length == 2) {
                wc = Utils.parseLong(s[0]);
                hexAddr = s[1];
                query = "SELECT FROM WalletEntity WHERE (wc = '" + wc + "') AND (hexAddress = '" + hexAddr + "')";
            }
        }

        if (query.equals("")) {
            return new ArrayList<>();
        }

        List<WalletEntity> result = db.objectQuery(query);

        if (result.isEmpty()) {
            log.info("no accounts found");
            return new ArrayList<>();
        } else {
            log.info("found accounts {}", result.size());
            return result;
        }
    }
}
