package org.ton.executors.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.main.Main;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.sun.javafx.PlatformUtil.isWindows;

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
}
