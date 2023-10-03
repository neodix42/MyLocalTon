package org.ton.executors.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.ResultListBlockTransactions;
import org.ton.settings.Node;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Future;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class LiteClient {

    private static final String LITE_CLIENT_EXE = "lite-client.exe";
    private static final String LITE_CLIENT = "lite-client";
    private static LiteClientEnum config;
    private static LiteClient singleInstance = null;

    private LiteClient() {

    }

    public static LiteClient getInstance(LiteClientEnum pConfig) {
        if (isNull(singleInstance)) {
            config = pConfig;
            singleInstance = new LiteClient();
        }

        return singleInstance;
    }

//    public LiteClient(LiteClientEnum config) {
//        this.config = config;
//    }

    public String getLastCommand(Node node) {
        String command = "last";

        String binaryPath = node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? LITE_CLIENT_EXE : LITE_CLIENT);

        String[] withBinaryCommand;
        switch (config) {
            case GLOBAL:
                withBinaryCommand = new String[]{binaryPath, "-t", "10", "-C", node.getNodeGlobalConfigLocation(), "-c"};
                break;
            case LOCAL:
                withBinaryCommand = new String[]{binaryPath, "-t", "10", "-C", node.getNodeLocalConfigLocation(), "-c"};
                break;
            case FORKED:
                withBinaryCommand = new String[]{binaryPath, "-t", "10", "-C", node.getNodeForkedGlobalConfigLocation(), "-c"};
                break;
            default:
                withBinaryCommand = new String[]{binaryPath, "-t", "10", "-C", node.getNodeGlobalConfigLocation(), "-c"};
        }
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        return String.join(" ", withBinaryCommand);
    }

    public String executeLast(Node node) {
        String command = "last";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.error("executeLast error {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public long executeGetSeqno(Node node, String contractAddress) {
        try {
            return LiteClientParser.parseRunMethodSeqno(executeRunMethod(node, contractAddress, "seqno", ""));
        } catch (Exception e) {
            return -1L;
        }
    }

    public long executeGetSubWalletId(Node node, String contractAddress) {
        try {
            return LiteClientParser.parseRunMethodSeqno(executeRunMethod(node, contractAddress, "get_subwallet_id", ""));
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * @param seqno - is the pureBlockSeqno
     * @return string result of lite-client output
     */
    public String executeBySeqno(Node node, long wc, String shard, BigInteger seqno) throws Exception {
        final String command = String.format("byseqno %d:%s %d", wc, shard, seqno);
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    /**
     * @param resultLastBlock      - full block id
     * @param amountOfTransactions - if zero defaults to 100000
     * @return string result of lite-client output
     */
    public String executeListblocktrans(Node node, final ResultLastBlock resultLastBlock, final long amountOfTransactions) {
        final String command = String.format("listblocktrans %s %d", resultLastBlock.getFullBlockSeqno(),
                (amountOfTransactions == 0) ? 100000 : amountOfTransactions);
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.error("executeListblocktrans error {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeDumptrans(Node node, final ResultLastBlock resultLastBlock, final ResultListBlockTransactions tx) {
        final String command = String.format("dumptrans %s %d:%s %d", resultLastBlock.getFullBlockSeqno(), resultLastBlock.getWc(), tx.getAccountAddress(), tx.getLt());
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.error("executeDumptrans error {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeDumptrans(Node node, String tx) {
        final String command = String.format("dumptrans %s", tx);
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.error("executeDumptrans error {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeDumpblock(Node node, final ResultLastBlock resultLastBlock) {
        final String command = String.format("dumpblock %s", resultLastBlock.getFullBlockSeqno());
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.error("executeDumpblock error {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeDumpblock(Node node, String fullBlockSeqno) {
        final String command = String.format("dumpblock %s", fullBlockSeqno);
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.error("executeDumpblock error {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeAllshards(Node node, final ResultLastBlock resultLastBlock) throws Exception {
        final String command = "allshards " + resultLastBlock.getFullBlockSeqno();
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetAccount(Node node, String address) {
        final String command = "getaccount " + address;
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            try {
                return result.getRight().get();
            } catch (Exception e) {
                log.error("executeGetAccount error {}", e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    public String executeRunMethod(Node node, String address, String methodId, String params) throws Exception {
        final String command = String.format("runmethod %s %s %s", address, methodId, params);
        return LiteClientExecutor.getInstance(config).execute(node, command).getRight().get();
    }

    public String executeSendfile(Node node, String absolutePathFile) throws Exception {
        final String command = "sendfile " + absolutePathFile;
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeBlockchainInfo(Node node) throws Exception {
        //
        final String command = "getconfig 12";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetElections(Node node) throws Exception {
        //
        final String command = "getconfig 15";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetConfigSmcAddress(Node node) throws Exception {
        final String command = "getconfig 0";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetElectorSmcAddress(Node node) throws Exception {
        final String command = "getconfig 1";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetMinterSmcAddress(Node node) throws Exception {
        final String command = "getconfig 2";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    // start of the validation cycle
    public long executeGetActiveElectionId(Node node, String electorAddr) throws Exception {
        return LiteClientParser.parseRunMethodSeqno(executeRunMethod(node, electorAddr, "active_election_id", ""));
    }

    public String executeGetParticipantList(Node node, String electorAddr) throws Exception {
        // parseRunMethodParticipantList
        return executeRunMethod(node, electorAddr, "participant_list", "");
    }

    public String executeComputeReturnedStake(Node node, String electorAddr, String validatorWalletAddr) throws Exception {
        // parseRunMethodComputeReturnedStake
        //final String command = String.format("runmethod %s %s 0x%s", electorAddr, "compute_returned_stake", validatorWalletAddr);
        //log.info(command);
        return executeRunMethod(node, electorAddr, "compute_returned_stake", "0x" + validatorWalletAddr.trim().toLowerCase());
    }

    public String executeGetMinMaxStake(Node node) throws Exception {
        final String command = "getconfig 17";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetPreviousValidators(Node node) throws Exception {
        final String command = "getconfig 32";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetCurrentValidators(Node node) throws Exception {
        final String command = "getconfig 34";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetNextValidators(Node node) throws Exception {
        final String command = "getconfig 36";
        Pair<Process, Future<String>> result = LiteClientExecutor.getInstance(config).execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public List<ResultLastBlock> getShardsFromBlock(Node node, ResultLastBlock lastBlock) {
        try {
            List<ResultLastBlock> foundShardsInBlock = LiteClientParser.parseAllShards(executeAllshards(node, lastBlock));
            log.debug("found {} shards in block {}", foundShardsInBlock.size(), foundShardsInBlock);
            return foundShardsInBlock;
        } catch (Exception e) {
            log.error("Error retrieving shards from the block, {}", e.getMessage());
            return null;
        }
    }
}
