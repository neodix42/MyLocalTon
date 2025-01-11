package org.ton.executors.createhardfork;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;

@Slf4j
public class HardforkExecutor {

    private static final String HARDFORK_EXE = "create-hardfork.exe";
    private static final String HARDFORK = "create-hardfork";

    public Pair<Process, Future<String>> execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? HARDFORK_EXE : HARDFORK);

        String[] withBinaryCommand = {binaryPath};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.debug("execute: {}", String.join(" ", withBinaryCommand));

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(binaryPath).getParent()));
            Process p = pb.start();
            p.waitFor(5, TimeUnit.SECONDS);
          Future<String> future = ForkJoinPool.commonPool().submit(() -> {
                try {
                    Thread.currentThread().setName("create-hardfork-" + node.getNodeName());

                    String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                    log.debug("{} stopped", "create-hardfork-" + node.getNodeName());
                    p.getInputStream().close();
                    p.getErrorStream().close();
                    p.getOutputStream().close();

                    return resultInput;

                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            return Pair.of(p, future);

        } catch (final IOException e) {
            log.error(e.getMessage());
            return null;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
