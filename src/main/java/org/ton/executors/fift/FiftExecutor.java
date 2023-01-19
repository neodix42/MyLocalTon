package org.ton.executors.fift;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class FiftExecutor {

    private static final String FIFT_EXE = "fift.exe";
    private static final String FIFT = "fift";

    public Pair<Process, Future<String>> execute(Node node, String... command) {

        final String fiftBinaryPath = node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? FIFT_EXE : FIFT);
        String[] withBinaryCommand = {fiftBinaryPath, "-s"};
        String[] commandTrimmed = ArrayUtils.removeAllOccurences(command, "");
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, commandTrimmed);

        try {
            log.debug("execute: {}", String.join(" ", withBinaryCommand));

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("FIFTPATH", node.getTonBinDir() + "lib");

            pb.directory(new File(new File(fiftBinaryPath).getParent()));
            Process p = pb.start();

            Future<String> future = executorService.submit(() -> {
                try {
                    Thread.currentThread().setName("fift-" + node.getNodeName());

                    String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                    log.debug("{} stopped", "fift-" + node.getNodeName());
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
