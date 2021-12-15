package org.ton.executors.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.executors.liteclient.api.ResultListBlockTransactions;
import org.ton.main.Main;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.nonNull;

@Slf4j
public class LiteClientExecutor {

    private static final String LITE_CLIENT_EXE = "lite-client.exe";
    private static final String LITE_CLIENT = "lite-client";
    private boolean forked = false;

    public LiteClientExecutor() {

    }

    public LiteClientExecutor(boolean forked) {
        this.forked = forked;
    }

    public Pair<Process, Future<String>> execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (isWindows() ? LITE_CLIENT_EXE : LITE_CLIENT);

        String[] withBinaryCommand = {binaryPath, "-C", forked ? node.getNodeForkedGlobalConfigLocation() : node.getNodeGlobalConfigLocation(), "-c"};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            if (Main.appActive.get()) {
                log.debug("execute: {}", String.join(" ", withBinaryCommand));

                ExecutorService executorService = Executors.newSingleThreadExecutor();

                final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

                pb.directory(new File(new File(binaryPath).getParent()));
                Process p = pb.start();

                Future<String> future = executorService.submit(() -> {
                    try {
                        Thread.currentThread().setName("lite-client-" + node.getNodeName());

                        String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                        log.debug("{} stopped.", "lite-client-" + node.getNodeName());
                        p.getInputStream().close();
                        p.getErrorStream().close();
                        p.getOutputStream().close();

                        return resultInput;

                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                });

                executorService.shutdown();

                return Pair.of(p, future);
            }
            return null;

        } catch (final IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String getLastCommand(Node node) {
        String command = "last";

        String binaryPath = node.getTonBinDir() + (isWindows() ? LITE_CLIENT_EXE : LITE_CLIENT);

        String[] withBinaryCommand = {binaryPath, "-C", forked ? node.getNodeForkedGlobalConfigLocation() : node.getNodeGlobalConfigLocation(), "-c"};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        return String.join(" ", withBinaryCommand);
    }

    public String executeLast(Node node) {
        String command = "last";
        Pair<Process, Future<String>> result = execute(node, command);
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

    public long executeGetSeqno(Node node, String contractAddress) throws Exception {
        return LiteClientParser.parseRunMethodSeqno(executeRunMethod(node, contractAddress, "seqno", ""));
    }

    /**
     * @param seqno - is the pureBlockSeqno
     * @return string result of lite-client output
     */
    public String executeBySeqno(Node node, long wc, String shard, String seqno) throws Exception {
        final String command = String.format("byseqno %d:%s %s", wc, shard, seqno);
        Pair<Process, Future<String>> result = execute(node, command);
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
        Pair<Process, Future<String>> result = execute(node, command);
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
        final String command = String.format("dumptrans %s %d:%s %s", resultLastBlock.getFullBlockSeqno(), resultLastBlock.getWc(), tx.getAccountAddress(), tx.getLt());
        Pair<Process, Future<String>> result = execute(node, command);
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
        Pair<Process, Future<String>> result = execute(node, command);
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
        Pair<Process, Future<String>> result = execute(node, command);
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
        Pair<Process, Future<String>> result = execute(node, command);
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
        Pair<Process, Future<String>> result = execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public String executeGetAccount(Node node, String address) {
        final String command = "getaccount " + address;
        Pair<Process, Future<String>> result = execute(node, command);
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
        return execute(node, command).getRight().get().toString();
    }

    public String executeSendfile(Node node, String absolutePathFile) throws Exception {
        final String command = "sendfile " + absolutePathFile;
        Pair<Process, Future<String>> result = execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    /**
     * getconfig 34
     */
    public String executeGetCurrentValidators(Node node) throws Exception {
        final String command = "getconfig 34";
        Pair<Process, Future<String>> result = execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    /**
     * getconfig 15
     */
    public String executeGetElections(Node node) throws Exception {
        final String command = "getconfig 15";
        Pair<Process, Future<String>> result = execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    /**
     * getconfig 1
     */
    public String executeGetElectorSmcAddress(Node node) throws Exception {
        final String command = "getconfig 1";
        Pair<Process, Future<String>> result = execute(node, command);
        if (nonNull(result)) {
            return result.getRight().get();
        } else {
            return null;
        }
    }

    public long executeGetActiveElectionId(Node node, String electorAddr) throws Exception {
        return LiteClientParser.parseRunMethodSeqno(executeRunMethod(node, electorAddr, "active_election_id", ""));
    }

    public String executeGetParticipantList(Node node, String electorAddr) throws Exception {
        // parseRunMethodParticipantList
        return executeRunMethod(node, electorAddr, "participant_list", "");
    }

    public String executeComputeReturnedStake(Node node, String electorAddr, String validatorWalletAddr) throws Exception {
        // parseRunMethodComputeReturnedStake
        return executeRunMethod(node, electorAddr, "compute_returned_stake", validatorWalletAddr);
    }

    public String executeGetMinMaxStake(Node node) throws Exception {
        final String command = "getconfig 17";
        return execute(node, command).getRight().get();
    }

    public String executeGetPreviousValidators(Node node) throws Exception {
        final String command = "getconfig 32";
        return execute(node, command).getRight().get();
    }

    public String executeGetNextValidators(Node node) throws Exception {
        final String command = "getconfig 36";
        return execute(node, command).getRight().get();
    }
}
