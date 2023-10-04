package org.ton.executors.validatorengineconsole;

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

//@Component
@Slf4j
public class ValidatorEngineConsoleExecutor {

    private static final String VALIDATOR_ENGINE_CONSOLE_EXE = "validator-engine-console.exe";
    private static final String VALIDATOR_ENGINE_CONSOLE = "validator-engine-console";

    public Pair<Process, Future<String>> execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? VALIDATOR_ENGINE_CONSOLE_EXE : VALIDATOR_ENGINE_CONSOLE);

        String[] withBinaryCommand = {binaryPath, "-t", "10",};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.debug("execute: {}", String.join(" ", withBinaryCommand));

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(binaryPath).getParent()));
            Process p = pb.start();

            Future<String> future = executorService.submit(() -> {
                try {
                    Thread.currentThread().setName("validator-engine-console" + node.getNodeName());

                    String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                    log.debug("{} stopped", "validator-engine-console " + node.getNodeName());
                    log.debug("validator-console exit output: {} ", resultInput);
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
