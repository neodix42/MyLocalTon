package org.ton.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.*;
import org.ton.executors.liteclient.api.block.Block;
import org.ton.executors.liteclient.api.block.MintMessage;
import org.ton.executors.liteclient.api.block.RecoverCreateMessage;
import org.ton.executors.liteclient.api.block.Transaction;
import org.ton.executors.liteclient.exception.IncompleteDump;
import org.ton.executors.liteclient.exception.ParsingError;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

@Slf4j
@RunWith(JUnit4.class)
public class LiteClientParserTest {

    private static final BigInteger BLOCK_SEQNO = new BigInteger("2268701");
    private static final BigDecimal ZERO_LONG = BigDecimal.ZERO;

    @Test
    public void TestParseCreateHardFork() throws IOException {
        String newBlockOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/create-hardfork.log")), StandardCharsets.UTF_8);
        ResultLastBlock newBlock = LiteClientParser.parseCreateHardFork(newBlockOutput);
        assertThat(newBlock).isNotNull();
        assertThat(newBlock.getFullBlockSeqno()).isNotNull();
    }

    @Test
    public void TestParseGetSeqno() {
        Long seqno = LiteClientParser.parseRunMethodSeqno("result:  [ 21234120 ]");
        assertThat(seqno).isEqualTo(21234120L);
    }

    @Test
    public void TestParseLastCommand() throws IOException {
        // given
        String lastCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/last.log")), StandardCharsets.UTF_8);

        // when
        ResultLastBlock result = LiteClientParser.parseLast(lastCommandOutput);

        // then
        assertThat(result).isNotNull();
        assertEquals(BLOCK_SEQNO, result.getSeqno());
        assertEquals("C137C6DBB60DC57F1ABCE617D0C624D768D53F5DE37B901B8983A60BE39A7AFB", result.getRootHash());
        assertEquals("993488879F727254F8FC20D5424272C51AA8ECAA46953902012C261550727CC7", result.getFileHash());
        assertEquals(Long.valueOf(1581949176L), result.getCreatedAt());
    }

    @Test
    public void TestParseBySeqCommand() throws IOException, IncompleteDump, ParsingError {
        // given
        String bySeqCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/byseqno.log")), StandardCharsets.UTF_8);
        // when
        ResultLastBlock result = LiteClientParser.parseBySeqno(bySeqCommandOutput);

        // then
        assertThat(result).isNotNull();
        assertEquals(BLOCK_SEQNO, result.getSeqno());
        assertEquals(159, result.getFullBlockSeqno().length());
        assertEquals("C137C6DBB60DC57F1ABCE617D0C624D768D53F5DE37B901B8983A60BE39A7AFB", result.getRootHash());
        assertEquals("993488879F727254F8FC20D5424272C51AA8ECAA46953902012C261550727CC7", result.getFileHash());
        assertEquals(Long.valueOf(1581949176L), result.getCreatedAt());
    }

    @Test
    public void TestParseListTransCommand() throws IOException {
        // given
        String parseListblocktransCommand = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/listblocktrans.log")), StandardCharsets.UTF_8);
        // when
        List<ResultListBlockTransactions> txs = LiteClientParser.parseListBlockTrans(parseListblocktransCommand);
        // then
        assertThat(txs).isNotNull();
        assertEquals(5, txs.size());
    }

    @Test
    public void TestNegativeParseListTransCommand() {
        // given
        String parseListblocktransCommand = "";
        // when
        List<ResultListBlockTransactions> txs = LiteClientParser.parseListBlockTrans(parseListblocktransCommand);
        // then
        assertThat(txs).isNotNull();
        assertThat(txs.isEmpty()).isTrue();
    }

    @Test
    public void TestParseDumptransNoTxCommand() throws IOException {
        // given
        String dumptransNoTxsCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumptrans_nothing.log")), StandardCharsets.UTF_8);
        // when
        Transaction result = LiteClientParser.parseDumpTrans(dumptransNoTxsCommandOutput, true);
        // then
        assertThat(result.getInMsg()).isNull();
        assertThat(result.getOutMsgs()).isEmpty();
    }

    @Test
    public void TestParseDumptransTxCommand() throws IOException {
        // given
        String dumptransTxsCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumptrans.log")), StandardCharsets.UTF_8);
        // when
        Transaction result = LiteClientParser.parseDumpTrans(dumptransTxsCommandOutput, true);
        // then
        assertThat(result).isNotNull();
        assertEquals(0, result.getDescription().getAborted().byteValue());
        assertEquals(1, result.getDescription().getAction().getSuccess().byteValue());
        assertEquals(2700000000L, result.getInMsg().getValue().getToncoins().longValue());
    }

    @Test
    public void TestParseDumptransTxSrcNoneCommand() throws IOException {
        // given
        String dumptransTxsCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumptrans_src_addr_none_ext_msg.log")), StandardCharsets.UTF_8);
        // when
        Transaction result = LiteClientParser.parseDumpTrans(dumptransTxsCommandOutput, true);
        // then
        assertThat(result).isNotNull();
        assertEquals(0, result.getDescription().getAborted().byteValue());
        assertEquals(1, result.getDescription().getAction().getSuccess().byteValue());
        assertEquals(0, result.getInMsg().getValue().getToncoins().longValue());

    }

    @Test
    public void TestParseDumptransTxOutMsgs() throws IOException {
        // given
        String dumptransTxsCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumptrans_outmsgs.log")), StandardCharsets.UTF_8);
        // when
        Transaction result = LiteClientParser.parseDumpTrans(dumptransTxsCommandOutput, true);
        // then
        assertThat(result).isNotNull();
        assertEquals(0, result.getDescription().getAborted().byteValue());
        assertEquals(1, result.getDescription().getAction().getSuccess().byteValue());
        assertEquals(0, result.getInMsg().getValue().getToncoins().longValue());
        assertEquals(2, result.getInMsg().getBody().getCells().size());

    }

    @Test
    public void TestParseAllShardsCommand2() throws IOException, IncompleteDump, ParsingError {
        // given
        String allShardsCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/allshards_1.log")), StandardCharsets.UTF_8);
        // when
        List<ResultLastBlock> result = LiteClientParser.parseAllShards(allShardsCommandOutput);
        // then
        assertThat(result).isNotNull();
        assertEquals(83, result.size());
    }

    @Test
    public void TestParseAllShardsCommand() throws IOException, IncompleteDump, ParsingError {
        // given
        String allShardsCommandOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/allshards.log")), StandardCharsets.UTF_8);
        // when
        List<ResultLastBlock> result = LiteClientParser.parseAllShards(allShardsCommandOutput);
        // then
        assertThat(result).isNotNull();
        assertEquals(4, result.size());
    }

    @Test
    public void TestParseBlock3() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock_3.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, false);
        block.listBlockTrans().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));
        assertEquals(-239L, block.getGlobalId().longValue());

    }

    @Test
    public void TestParseBlock2() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock_2.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, false);
        block.listBlockTrans().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));
        assertEquals(-239L, block.getGlobalId().longValue());

    }

    @Test
    public void TestParseBlock() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, false);
        block.listBlockTrans().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));

        block.allShards().forEach(x -> log.info("wc: {} shard: {}, seqno: {} root_hash: {} file_hash: {} utime: {}, start_lt: {} end_lt: {}",
                x.getWc(), new BigInteger(x.getNextValidatorShard()).toString(16), x.getSeqno(), x.getRootHash(), x.getFileHash(), x.getGenUtime(), x.getStartLt(), x.getEndLt()));

        //then
        log.info("{}", block);

        // block info
        assertEquals(2259597L, block.getInfo().getPrevKeyBlockSeqno().longValue());
        assertEquals(32746L, block.getInfo().getGetCatchainSeqno().longValue());
        assertEquals(1, block.getInfo().getWantMerge().byteValue());
        assertEquals(-1, block.getInfo().getWc().longValue());
        assertEquals(1581948886L, block.getInfo().getGenUtime().longValue());
        assertEquals(3349752000000L, block.getInfo().getStartLt().longValue());
        assertEquals(-239L, block.getGlobalId().longValue());
        assertEquals("6708901E4C92A869EADA75AB3589B00ED41B5FEA5EBD8DE4C51A8F6DAFEA75E6", block.getInfo().getPrevRootHash());
        assertEquals("60C056A5E6A0BD33D617F9EA4CF88253DF3F1A94A6EFBE32E5B48E4CB8AC4498", block.getInfo().getPrevFileHash());

        // value flow
        assertEquals(4996204450013194057L, block.getValueFlow().getPrevBlock().getToncoins().longValue());
        assertEquals(666666666666L, block.getValueFlow().getPrevBlock().getOtherCurrencies().get(0).getValue().longValue());
        assertEquals("000001DF_", block.getValueFlow().getPrevBlock().getOtherCurrencies().get(0).getLabel());
        assertEquals(1000000000000L, block.getValueFlow().getPrevBlock().getOtherCurrencies().get(1).getValue().longValue());
        assertEquals("FFFFFFDF_", block.getValueFlow().getPrevBlock().getOtherCurrencies().get(1).getLabel());
        assertEquals(4996204453713194057L, block.getValueFlow().getNextBlock().getToncoins().longValue());
        assertEquals(666666666666L, block.getValueFlow().getNextBlock().getOtherCurrencies().get(0).getValue().longValue());
        assertEquals(ZERO_LONG, block.getValueFlow().getImported().getToncoins());
        assertEquals(ZERO_LONG, block.getValueFlow().getExported().getToncoins());
        assertEquals(3700000000L, block.getValueFlow().getFeesCollected().getToncoins().longValue());
        assertEquals(2000000000L, block.getValueFlow().getFeesImported().getToncoins().longValue());
        assertEquals(1700000000L, block.getValueFlow().getCreated().getToncoins().longValue());
        assertEquals(ZERO_LONG, block.getValueFlow().getMinted().getToncoins());

        //state update
        assertThat(block.getShardState()).isNull();

        // extra block
        assertEquals("47A6AE11AD2410E18E1CE160A98D9995619DFD1D80FE792968EAFBAB4D6F0A3A", block.getExtra().getRandSeed());
        assertEquals("ED1DE6FF8F508F6E6EF480E5DB0D1E332F138865C51348049D4A939A3AC68D2D", block.getExtra().getCreatedBy());

        // out msg descr
        assertEquals(0, block.getExtra().getOutMsgsDescrs().getLeaf().size());
        /*
        //InMsgDescriptor
        assertEquals("xA09EE8EBD0CBBC53671E1EAA5ABE0B0C2B281FE2C2D09C99274A80A087438D58", block.getExtra().getInMsgDescrs().getLabel());
        assertEquals(1581948886L, block.getExtra().getInMsgDescrs().getMessage().getCreatedAt().longValue());
        assertEquals(3349752000000L, block.getExtra().getInMsgDescrs().getMessage().getCreatedLt().longValue());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getValueImported().getGrams());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getFeesCollected());

        // message
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", block.getExtra().getInMsgDescrs().getMessage().getSrcAddr().getAddr());
        assertEquals("3333333333333333333333333333333333333333333333333333333333333333", block.getExtra().getInMsgDescrs().getMessage().getDestAddr().getAddr());
        assertEquals(-1L, block.getExtra().getInMsgDescrs().getMessage().getDestAddr().getWc().longValue());
        assertEquals(3700000000L, block.getExtra().getInMsgDescrs().getMessage().getValue().getGrams().longValue());
        assertEquals(1L, block.getExtra().getInMsgDescrs().getMessage().getBounce().longValue());
        assertEquals(1L, block.getExtra().getInMsgDescrs().getMessage().getIhrDisabled().longValue());
        assertEquals(0, block.getExtra().getInMsgDescrs().getMessage().getBounced().byteValue());
        assertThat(block.getExtra().getInMsgDescrs().getMessage().getValue().getOtherCurrencies().isEmpty()).isTrue();

        // transaction
        assertEquals("3333333333333333333333333333333333333333333333333333333333333333", block.getExtra().getInMsgDescrs().getTransaction().getAccountAddr());
        assertEquals(1581948886L, block.getExtra().getInMsgDescrs().getTransaction().getNow().longValue());
        assertEquals("acc_state_active", block.getExtra().getInMsgDescrs().getTransaction().getOrigStatus());
        assertEquals(1, block.getExtra().getInMsgDescrs().getTransaction().getInMsg().getBounce().byteValue());
        assertEquals(0, block.getExtra().getInMsgDescrs().getTransaction().getInMsg().getBounced().byteValue());
        assertEquals(1, block.getExtra().getInMsgDescrs().getTransaction().getInMsg().getIhrDisabled().byteValue());
        assertEquals(3700000000L, block.getExtra().getInMsgDescrs().getTransaction().getInMsg().getValue().getGrams().longValue());
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
                block.getExtra().getInMsgDescrs().getTransaction().getInMsg().getSrcAddr().getAddr());
        assertEquals("3333333333333333333333333333333333333333333333333333333333333333",
                block.getExtra().getInMsgDescrs().getTransaction().getInMsg().getDestAddr().getAddr());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getInMsg().getIhrFee());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTotalFees().getGrams());
        assertEquals("1F0816C8E551E2698159F0EDE1A194B3C891DF8653AF99347474C51A0B2A1E96",
                block.getExtra().getInMsgDescrs().getTransaction().getOldHash());
        assertEquals("C15893028872494504D7C852E503E9CFC2C0F5E9F2E17D25FDE68AEC027CD9E9",
                block.getExtra().getInMsgDescrs().getTransaction().getNewHash());

        //description
        assertEquals(0, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAborted().byteValue());
        assertEquals(0, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getDestroyed().byteValue());
        assertThat(block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getBounce()).isNull();
        assertEquals(0, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCreditFirst().longValue());

        //storage
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getStorage().getFeesCollected());
        assertThat(block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getStorage().getFeesDue()).isZero();
        assertEquals("acst_unchanged", block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getStorage().getStatusChange());

        //credit
        assertEquals(3700000000L, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCredit().getCredit().getGrams().longValue());
        assertThat(block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCredit().getDueFeesCollected()).isZero();

        //compute
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getGasFees());
        assertEquals(5798L, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getGasUsed().longValue());
        assertEquals(370000L, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getGasLimit().longValue());
        assertThat(block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getGasCredit()).isZero();
        assertEquals(1, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getSuccess().byteValue());
        assertEquals(0, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getAccountActivated().byteValue());
        assertEquals("nothing", block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getExitArg());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getExitCode());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getMode());
        assertEquals(101L, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getVmSteps().longValue());
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getVmInitStateHash());
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getCompute().getVmFinalStateHash());

        //action
        assertEquals(1, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getSuccess().byteValue());
        assertEquals(1, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getValid().byteValue());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getMsgsCreated());
        assertEquals(0, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getNoFunds().byteValue());
        assertEquals("acst_unchanged", block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getStatusChange());
        assertThat(block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getTotalFwdFee()).isZero();
        assertEquals("96A296D224F285C67BEE93C30F8A309157F0DAA35DC5B87E410B78630A09CFC7", block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getActionListHash());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getTotalMsgSizeBits());
        assertEquals(ZERO_LONG, block.getExtra().getInMsgDescrs().getTransaction().getTxDescription().getAction().getTotalMsgSizeCells());

        

        // outMsgDescriptor
        assertEquals(ZERO_LONG, block.getExtra().getOutMsgsDescrs().getValue().getGrams());
        */

        //account blocks
        assertEquals("3333333333333333333333333333333333333333333333333333333333333333", block.getExtra().getAccountBlock().getTransactions().get(0).getAccountAddr());
        assertEquals("3333333333333333333333333333333333333333333333333333333333333333", block.getExtra().getAccountBlock().getTransactions().get(1).getAccountAddr());
        assertEquals("34517C7BDF5187C55AF4F8B61FDC321588C7AB768DEE24B006DF29106458D7CF", block.getExtra().getAccountBlock().getTransactions().get(2).getAccountAddr());
        assertEquals("34517C7BDF5187C55AF4F8B61FDC321588C7AB768DEE24B006DF29106458D7CF", block.getExtra().getAccountBlock().getTransactions().get(3).getAccountAddr());
        assertEquals("5555555555555555555555555555555555555555555555555555555555555555", block.getExtra().getAccountBlock().getTransactions().get(4).getAccountAddr());

        assertEquals(3349752000001L, block.getExtra().getAccountBlock().getTransactions().get(0).getLt().longValue());
        assertEquals("Active", block.getExtra().getAccountBlock().getTransactions().get(0).getOrigStatus());
        assertEquals(8145L, block.getExtra().getAccountBlock().getTransactions().get(0).getDescription().getCompute().getGasUsed().longValue());
        assertEquals(10000000L, block.getExtra().getAccountBlock().getTransactions().get(0).getDescription().getCompute().getGasLimit().longValue());
        assertEquals(1, block.getExtra().getAccountBlock().getTransactions().get(0).getDescription().getAction().getSuccess().byteValue());

        assertEquals(3700000000L, block.getExtra().getAccountBlock().getTransactions().get(1).getInMsg().getValue().getToncoins().longValue());
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", block.getExtra().getAccountBlock().getTransactions().get(1).getInMsg().getSrcAddr().getAddr());
        assertEquals("3333333333333333333333333333333333333333333333333333333333333333", block.getExtra().getAccountBlock().getTransactions().get(1).getInMsg().getDestAddr().getAddr());
        assertEquals(-1, block.getExtra().getAccountBlock().getTransactions().get(1).getInMsg().getDestAddr().getWc().longValue());
        assertEquals(ZERO_LONG, block.getExtra().getAccountBlock().getTransactions().get(1).getTotalFees().getToncoins());
        assertThat(block.getExtra().getAccountBlock().getTransactions().get(1).getOutMsgs()).isEmpty();

        //masterchain block
        assertEquals(0, block.getExtra().getMasterchainBlock().getWc().longValue());
        assertEquals(4, block.getExtra().getMasterchainBlock().getShardHashes().size());
        assertEquals(0, block.getExtra().getMasterchainBlock().getShardFees().size());

    }

    @Test
    public void TestParseShardBlock4() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock_shard_4.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, false);
        block.getExtra().getAccountBlock().getTransactions().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));
        log.info("{}", block);
        assertEquals(Long.valueOf(-239), block.getGlobalId());
    }

    @Test
    public void TestParseGenesisBlock() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock_genesis.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, true);
        block.getExtra().getAccountBlock().getTransactions().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));
        log.info("{}", block);
        assertEquals(Long.valueOf(-239), block.getGlobalId());
        MintMessage mintMsg = block.getExtra().getMasterchainBlock().getMintMsg();
        RecoverCreateMessage recoverCreateMessage = block.getExtra().getMasterchainBlock().getRecoverCreateMsg();
        log.info("mint msg {}", mintMsg);
        log.info("recover create msg {}", recoverCreateMessage);
        assertNotNull(mintMsg.getInMsg().getDestAddr().getAddr());
        assertFalse(recoverCreateMessage.getTransactions().isEmpty());

        // in msg descr
        assertEquals(5, block.getExtra().getInMsgDescrs().getLeaf().size());
        assertEquals("04F64C6AFBFF3DD10D8BA6707790AC9670D540F37A9448B0337BAA6A5A92ACAC", block.getExtra().getInMsgDescrs().getLeaf().get(0).getMessage().getSrcAddr().getAddr());
        assertEquals("0000000000000000000000000000000000000000000000000000000000000000", block.getExtra().getInMsgDescrs().getLeaf().get(0).getTransactions().get(0).getInMsg().getDestAddr().getAddr());
        // out msg descr
        assertEquals(7, block.getExtra().getOutMsgsDescrs().getLeaf().size());
        assertEquals("53F4842035396DB18AFFAE1895E66161C33360373C002A2AAAC7B1C6682599E", block.getExtra().getOutMsgsDescrs().getLeaf().get(0).getLabel());
        assertEquals("53F4842035396DB18AFFAE1895E66161C33360373C002A2AAAC7B1C6682599E", block.getExtra().getOutMsgsDescrs().getLeaf().get(0).getLabel());

        assertEquals("04F64C6AFBFF3DD10D8BA6707790AC9670D540F37A9448B0337BAA6A5A92ACAC", block.getExtra().getOutMsgsDescrs().getLeaf().get(0).getMessage().getSrcAddr().getAddr());
        assertEquals("E8CAE6E880EB79A2", block.getExtra().getOutMsgsDescrs().getLeaf().get(0).getMessage().getDestAddr().getAddr());

        assertEquals("04F64C6AFBFF3DD10D8BA6707790AC9670D540F37A9448B0337BAA6A5A92ACAC", block.getExtra().getOutMsgsDescrs().getLeaf().get(0).getTransactions().get(0).getOutMsgs().get(0).getSrcAddr().getAddr());
        assertEquals("E8CAE6E880EB79A2", block.getExtra().getOutMsgsDescrs().getLeaf().get(0).getTransactions().get(0).getOutMsgs().get(1).getDestAddr().getAddr());
        assertEquals("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEF", block.getExtra().getOutMsgsDescrs().getLeaf().get(0).getTransactions().get(0).getOutMsgs().get(2).getDestAddr().getAddr());

        assertEquals("04F64C6AFBFF3DD10D8BA6707790AC9670D540F37A9448B0337BAA6A5A92ACAC", block.getExtra().getOutMsgsDescrs().getLeaf().get(1).getMessage().getSrcAddr().getAddr());
        assertEquals(1634828580L, block.getExtra().getOutMsgsDescrs().getLeaf().get(1).getMessage().getCreatedAt().longValue());
        assertEquals("E8CAE6E880EB79A2", block.getExtra().getOutMsgsDescrs().getLeaf().get(1).getTransactions().get(0).getOutMsgs().get(1).getDestAddr().getAddr());

        assertEquals("E8CAE6E880EB79A2", block.getExtra().getOutMsgsDescrs().getLeaf().get(6).getTransactions().get(0).getOutMsgs().get(1).getDestAddr().getAddr());

    }

    @Test
    public void TestParseBasechainBlock() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock_basechain.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, false);
        block.getExtra().getAccountBlock().getTransactions().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));
        log.info("{}", block);
        assertEquals(Long.valueOf(-239), block.getGlobalId());
    }

    @Test
    public void TestParseShardBlock3() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock_shard_3.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, false);
        block.getExtra().getAccountBlock().getTransactions().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));
        log.info("{}", block);
        assertEquals(Long.valueOf(-239), block.getGlobalId());
    }

    @Test
    public void TestParseShardBlock() throws IOException, IncompleteDump, ParsingError {
        // given
        String blockDump = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/dumpblock_shard_2.log")), StandardCharsets.UTF_8);
        // when
        Block block = LiteClientParser.parseDumpblock(blockDump, false, false);
        block.getExtra().getAccountBlock().getTransactions().forEach(x -> log.info("account: {} lt: {} hash: {}", x.getAccountAddr(), x.getLt(), x.getNewHash()));
        log.info("{}", block);
        assertEquals(Long.valueOf(-239), block.getGlobalId());
    }

    @Test
    public void TestParseParticipantList() throws IOException {
        // given
        String participantListOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/participant_list.log")), StandardCharsets.UTF_8);
        // when
        List<ResultListParticipants> participants = LiteClientParser.parseRunMethodParticipantList(participantListOutput);

        assertEquals(4, participants.size());
        assertEquals("10003000000000", participants.get(0).getWeight());

    }

    @Test
    public void TestParseComputeReturnStake() throws IOException {
        // given
        String computeReturnStake = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/compute_returnstake.log")), StandardCharsets.UTF_8);
        // when
        ResultComputeReturnStake stake = LiteClientParser.parseRunMethodComputeReturnStake(computeReturnStake);

        assertEquals(stake.getStake(), BigDecimal.ZERO);
    }

    @Test
    public void TestParseParticipantListEmpty() throws IOException {
        // given
        String participantListOutput = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/participant_list_empty.log")), StandardCharsets.UTF_8);
        // when
        List<ResultListParticipants> participants = LiteClientParser.parseRunMethodParticipantList(participantListOutput);

        assertEquals(0, participants.size());
    }

    @Test
    public void TestParseConfig0() throws IOException {
        // given
        String getConfig0 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config0_configaddr.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig0 result = LiteClientParser.parseConfig0(getConfig0);

        // then
        assertThat(result.getConfigSmcAddr()).isEqualTo("-1:5555555555555555555555555555555555555555555555555555555555555555");
        log.info(result.toString());
    }

    @Test
    public void TestParseConfig1() throws IOException {
        // given
        String getConfig1 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config1_electoraddr.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig1 result = LiteClientParser.parseConfig1(getConfig1);

        // then
        assertThat(result.getElectorSmcAddress()).isEqualTo("-1:3333333333333333333333333333333333333333333333333333333333333333");
        log.info(result.toString());
    }

    @Test
    public void TestParseConfig2() throws IOException {
        // given
        String getConfig2 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config2_minteraddr.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig2 result = LiteClientParser.parseConfig2(getConfig2);

        // then
        assertThat(result.getMinterSmcAddress()).isEqualTo("-1:0000000000000000000000000000000000000000000000000000000000000000");
        log.info(result.toString());
    }

    @Test
    public void TestParseConfig12() throws IOException {
        // given
        String getConfig12 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config12_blokchain_info.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig12 result = LiteClientParser.parseConfig12(getConfig12);

        // then
        assertThat(result.getEnabledSince()).isEqualTo(1643393486L);
        assertThat(result.getMaxSplit()).isEqualTo(32L);
        assertThat(result.getFileHash()).isEqualTo("23BCB250A65922A69C61782EE0FFE49E70EA2E936B7D386D617FBB2EE1A526FF");
        log.info(result.toString());
    }

    @Test
    public void TestParseConfig15() throws IOException {
        // given
        String getConfig15 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config15_elections.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig15 result = LiteClientParser.parseConfig15(getConfig15);

        // then
        assertThat(result.getValidatorsElectedFor()).isEqualTo(4000L);
        assertThat(result.getStakeHeldFor()).isEqualTo(1000L);
        log.info(result.toString());
    }

    @Test
    public void TestParseConfig17() throws IOException {
        // given
        String getConfig17 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config17_minmaxstakes.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig17 result = LiteClientParser.parseConfig17(getConfig17);

        // then
        assertThat(result.getMinStake()).isEqualTo(10000000000000L);
        assertThat(result.getMaxStake()).isEqualTo(10000000000000000L);
        assertThat(result.getMaxStakeFactor()).isEqualTo(196608L);
        log.info(result.toString());
    }

    @Test
    public void TestParseConfig32() throws IOException {
        // given
        String getConfig32 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config32_previousvalidators.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig32 result = LiteClientParser.parseConfig32(getConfig32);
        // then
        assertThat(result.getValidators().getSince()).isEqualTo(1642862540L);
        assertThat(result.getValidators().getUntil()).isEqualTo(1642866540);
        assertThat(result.getValidators().getTotal()).isEqualTo(11L);
        assertThat(result.getValidators().getMain()).isEqualTo(11L);
        assertThat(result.getValidators().getTotalWeight()).isEqualTo(new BigInteger("3458764513820540919"));
        log.info(result.getValidators().toString());
    }

    @Test
    public void TestParseConfig34() throws IOException {
        // given
        String getConfig34 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config34_listcurrentvalidators.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig34 result = LiteClientParser.parseConfig34(getConfig34);
        // then
        assertThat(result.getValidators().getUntil()).isEqualTo(1644344873L);
        assertThat(result.getValidators().getTotal()).isEqualTo(2L);
        assertThat(result.getValidators().getMain()).isEqualTo(2L);
        assertThat(result.getValidators().getTotalWeight()).isEqualTo(1152921504606846976L);
        log.info(result.getValidators().toString());
    }

    @Test
    public void TestParseConfig36() throws IOException {
        // given
        String getConfig36 = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream("/config36_listnextvalidators.log")), StandardCharsets.UTF_8);

        // when
        ResultConfig36 result = LiteClientParser.parseConfig36(getConfig36);
        // then
        assertThat(result.getValidators().getUntil()).isEqualTo(1619649627L);
        assertThat(result.getValidators().getTotal()).isEqualTo(4L);
        assertThat(result.getValidators().getMain()).isEqualTo(4L);
        assertThat(result.getValidators().getTotalWeight()).isEqualTo(new BigInteger("1152921504606846976"));
        log.info(result.getValidators().toString());
    }
}