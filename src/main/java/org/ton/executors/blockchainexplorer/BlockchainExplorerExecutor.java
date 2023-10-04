package org.ton.executors.blockchainexplorer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class BlockchainExplorerExecutor {

    private static final String BLOCKCHAIN_EXPLORER_EXE = "blockchain-explorer.exe";
    private static final String BLOCKCHAIN_EXPLORER = "blockchain-explorer";

    public Pair<Process, Future<String>> execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? BLOCKCHAIN_EXPLORER_EXE : BLOCKCHAIN_EXPLORER);

        String[] withBinaryCommand = {binaryPath};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.info("execute: {}", String.join(" ", withBinaryCommand));

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(binaryPath).getParent()));
            Process p = pb.start();

            Future<String> future = executorService.submit(() -> {
                try {
                    Thread.currentThread().setName("blockchain-explorer-" + node.getNodeName());

                    String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                    log.debug("{} stopped", "blockchain-explorer-" + node.getNodeName());
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

        } catch (final IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
