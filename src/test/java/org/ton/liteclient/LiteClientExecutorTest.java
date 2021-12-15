package org.ton.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.ton.executors.liteclient.LiteClientExecutor;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.ResultListBlockTransactions;
import org.ton.executors.liteclient.api.block.Block;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.settings.GenesisNode;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.junit.Assert.*;

/**
 * Integration Tests that run against live testnet
 */
@Slf4j
public class LiteClientExecutorTest {
    private static final String CURRENT_DIR = System.getProperty("user.dir");
    private static final String TESTNET_CONFIG_LOCATION = CURRENT_DIR + File.separator + "testnet-global.config.json";

    private LiteClientExecutor liteClientExecutor;
    private Node testNode;

    @Before
    public void executedBeforeEach() throws IOException {
        InputStream TESTNET_CONFIG = IOUtils.toBufferedInputStream(getClass().getResourceAsStream("/testnet-global.config.json"));
        Files.copy(TESTNET_CONFIG, Paths.get(TESTNET_CONFIG_LOCATION), StandardCopyOption.REPLACE_EXISTING);

        liteClientExecutor = new LiteClientExecutor(); //new LiteClientExecutor(nodes.toArray(new String[0]));

        testNode = new GenesisNode();
        testNode.extractBinaries();
        testNode.setNodeGlobalConfigLocation(TESTNET_CONFIG_LOCATION);
    }

    @Test
    public void testLastExecuted() {
        //given
        String stdout = liteClientExecutor.executeLast(testNode);
        // then
        assertThat(stdout).isNotNull().contains("last masterchain block is").contains("server time is");
    }

    @Test
    public void testRunmethod() throws Exception {
        final String result = liteClientExecutor.executeRunMethod(testNode, "EQBdFkus6WRkJ1PP6z24Fw5C6E1YKet_nSJ6K1H7HHuOdwMC", "seqno", "");
        log.info(result);
        assertThat(result).contains("arguments").contains("result");
    }

    @Test
    public void testSendfile() throws Exception {
        final InputStream bocFile = IOUtils.toBufferedInputStream(getClass().getResourceAsStream("/new-wallet.boc"));
        final File targetFile = new File(CURRENT_DIR + File.separator + "new-wallet.boc");
        FileUtils.copyInputStreamToFile(bocFile, targetFile);
        final String result = liteClientExecutor.executeSendfile(testNode, targetFile.getAbsolutePath());
        log.info(result);
        assertThat(result).contains("sending query from file").contains("external message status is 1");
    }

    @Test
    public void testGetAccount() {
        final String result = liteClientExecutor.executeGetAccount(testNode, "EQBdFkus6WRkJ1PP6z24Fw5C6E1YKet_nSJ6K1H7HHuOdwMC");
        log.info(result);
        AccountState accountState = LiteClientParser.parseGetAccount(result);
        log.info(accountState.toString());
        assertThat(accountState.getBalance().getToncoins()).isNotNull();
    }

    @Test
    public void testListblocktransExecuted() {
        //given
        String resultLast = liteClientExecutor.executeLast(testNode);
        log.info("testListblocktransExecuted resultLast received");
        ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
        log.info("testListblocktransExecuted tonBlockId {}", resultLastBlock);
        // when
        String stdout = liteClientExecutor.executeListblocktrans(testNode, resultLastBlock, 2000);
        System.out.println(stdout);
        // then
        assertThat(stdout).isNotNull().contains("last masterchain block is").contains("obtained block").contains("transaction #").contains("account").contains("hash");
    }

    @Test
    public void testAllShardsExecuted() throws Exception {
        //given
        String resultLast = liteClientExecutor.executeLast(testNode);
        log.info("testAllShardsExecuted resultLast received");
        assertThat(resultLast).isNotEmpty();
        ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
        // when
        String stdout = liteClientExecutor.executeAllshards(testNode, resultLastBlock);
        // then
        assertThat(stdout).isNotNull()
                .contains("last masterchain block is")
                .contains("obtained block")
                .contains("got shard configuration with respect to block")
                .contains("shard configuration is").contains("shard #");
    }

    @Test
    public void testParseBySeqno() throws Exception {
        // given
        // 9MB size block (0,f880000000000000,4166691):6101667C299D3DD8C9E4C68F0BCEBDBA5473D812953C291DBF6D69198C34011B:608F5FC6D6CFB8D01A3D4A2F9EA5C353D82B4A08D7D755D8267D0141358329F1
        String resultLast = liteClientExecutor.executeLast(testNode);
        assertThat(resultLast).isNotEmpty();
        ResultLastBlock blockIdLast = LiteClientParser.parseLast(resultLast);
        assertThatObject(blockIdLast).isNotNull();
        assertNotNull(blockIdLast.getRootHash());
        // when
        String stdout = liteClientExecutor.executeBySeqno(testNode, blockIdLast.getWc(), blockIdLast.getShard(), blockIdLast.getSeqno());
        log.info(stdout);
        ResultLastBlock blockId = LiteClientParser.parseBySeqno(stdout);
        // then
        assertEquals(-1L, blockId.getWc().longValue());
        assertNotEquals(0L, blockId.getShard());
        assertNotEquals("0", blockId.getSeqno());
    }

    @Test
    public void testDumpBlockRealTimeExecuted() {
        log.info("testDumpBlockRealTimeExecuted test executes against the most recent state of TON blockchain, if it fails means the return format has changed - react asap.");
        //given
        String resultLast = liteClientExecutor.executeLast(testNode);
        log.info("testDumpBlockRealTimeExecuted resultLast received");
        assertThat(resultLast).isNotEmpty();
        ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
        log.info("testDumpBlockRealTimeExecuted tonBlockId {}", resultLastBlock);

        // when
        String stdout = liteClientExecutor.executeDumpblock(testNode, resultLastBlock);
        log.info(stdout);
        // then
        assertThat(stdout).isNotNull()
                .contains("last masterchain block is").contains("got block download request for")
                .contains("block header of")
                .contains("block contents is (block global_id")
                .contains("state_update:(raw@(MERKLE_UPDATE")
                .contains("extra:(block_extra")
                .contains("shard_fees:(")
                .contains("x{11EF55AAFFFFFF11}");
    }

    @Test
    public void testParseLastParsed() {
        // given
        String stdout = liteClientExecutor.executeLast(testNode);
        assertNotNull(stdout);
        // when
        ResultLastBlock blockId = LiteClientParser.parseLast(stdout);
        // then
        assertNotNull(blockId);
        assertNotNull(blockId.getFileHash());
        assertNotNull(blockId.getRootHash());
        assertEquals(-1L, blockId.getWc().longValue());
        assertNotEquals(0L, blockId.getShard());
        assertNotEquals("0", blockId.getSeqno());
    }

    @Test
    public void testParseListBlockTrans() {
        //given
        String stdoutLast = liteClientExecutor.executeLast(testNode);
        // when
        assertNotNull(stdoutLast);
        ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);

        String stdoutListblocktrans = liteClientExecutor.executeListblocktrans(testNode, blockIdLast, 0);
        log.info(stdoutListblocktrans);
        //then
        assertNotNull(stdoutListblocktrans);
        List<ResultListBlockTransactions> txs = LiteClientParser.parseListBlockTrans(stdoutListblocktrans);
        txs.forEach(System.out::println);
        assertEquals(1, txs.get(0).getTxSeqno());
    }

    @Test
    public void testParseAllShards() throws Exception {
        //given
        String stdoutLast = liteClientExecutor.executeLast(testNode);
        // when
        assertNotNull(stdoutLast);
        ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);
        String stdoutAllShards = liteClientExecutor.executeAllshards(testNode, blockIdLast);
        log.info(stdoutAllShards);
        //then
        assertNotNull(stdoutAllShards);
        List<ResultLastBlock> shards = LiteClientParser.parseAllShards(stdoutAllShards);

        shards.forEach(System.out::println);
        assertTrue(new BigInteger(shards.get(0).getSeqno()).compareTo(BigInteger.ZERO) > 0);
    }

    @Test
    public void testParseDumptransNew() {
        //given
        String stdoutLast = liteClientExecutor.executeLast(testNode);
        assertNotNull(stdoutLast);
        ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);

        String stdoutListblocktrans = liteClientExecutor.executeListblocktrans(testNode, blockIdLast, 0);
        assertNotNull(stdoutListblocktrans);
        log.info(stdoutListblocktrans);
        List<ResultListBlockTransactions> txs = LiteClientParser.parseListBlockTrans(stdoutListblocktrans);

        for (ResultListBlockTransactions tx : txs) {
            String stdoutDumptrans = liteClientExecutor.executeDumptrans(testNode, blockIdLast, tx);
            assertNotNull(stdoutDumptrans);
            Transaction txdetails = LiteClientParser.parseDumpTrans(stdoutDumptrans, true);
            if (!isNull(txdetails)) {
                log.info(txdetails.toString());
                assertNotEquals("0", txdetails.getLt());
            }
        }
    }

    @Test
    public void testParseAllSteps() throws Exception {
        //given
        String stdoutLast = liteClientExecutor.executeLast(testNode);
        assertNotNull(stdoutLast);
        ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);

        String stdoutAllShards = liteClientExecutor.executeAllshards(testNode, blockIdLast);
        //log.info(stdoutAllShards);

        String stdoutListblocktrans = liteClientExecutor.executeListblocktrans(testNode, blockIdLast, 0);
        assertNotNull(stdoutListblocktrans);
        log.info(stdoutListblocktrans);
        List<ResultListBlockTransactions> txs = LiteClientParser.parseListBlockTrans(stdoutListblocktrans);

        //then
        assertNotNull(stdoutAllShards);
        List<ResultLastBlock> shards = LiteClientParser.parseAllShards(stdoutAllShards);
        for (ResultLastBlock shard : shards) {
            String stdoutListblocktrans2 = liteClientExecutor.executeListblocktrans(testNode, shard, 0);
            List<ResultListBlockTransactions> txs2 = LiteClientParser.parseListBlockTrans(stdoutListblocktrans2);
            txs.addAll(txs2);
        }

        txs.forEach(System.out::println);

        for (ResultListBlockTransactions tx : txs) {
            String stdoutDumptrans = liteClientExecutor.executeDumptrans(testNode, blockIdLast, tx);
            assertNotNull(stdoutDumptrans);
            Transaction txdetails = LiteClientParser.parseDumpTrans(stdoutDumptrans, true);
            if (!isNull(txdetails)) {
                assertNotEquals("0", txdetails.getLt());
            }
        }
    }

    @Test
    public void testParseDumpblock() throws Exception {
        //given
        String stdoutLast = liteClientExecutor.executeLast(testNode);
        assertNotNull(stdoutLast);
        ResultLastBlock blockIdLast = LiteClientParser.parseLast(stdoutLast);
        String stdoutDumpblock = liteClientExecutor.executeDumpblock(testNode, blockIdLast);

        Block block = LiteClientParser.parseDumpblock(stdoutDumpblock, false, false);
        assertNotNull(block);

        assertNotEquals(0L, block.getGlobalId().longValue());
        assertNotEquals("0", block.getInfo().getSeqNo());
        assertNotNull(block.getInfo().getPrevFileHash());
        block.listBlockTrans().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));

        block.allShards().forEach(x -> log.info("wc: {} shard: {}, seqno: {} root_hash: {} file_hash: {} utime: {}, start_lt: {} end_lt: {}",
                x.getWc(), new BigInteger(x.getNextValidatorShard()).toString(16), x.getSeqno(), x.getRootHash(), x.getFileHash(), x.getGenUtime(), x.getStartLt(), x.getEndLt()));
    }
}
