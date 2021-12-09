package org.ton.executors.validatorengineconsole;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static com.sun.javafx.PlatformUtil.isWindows;

//@Component
@Slf4j
public class ValidatorEngineConsoleExecutor {

    private static final String VALIDATOR_ENGINE_CONSOLE_EXE = "validator-engine-console.exe";
    private static final String VALIDATOR_ENGINE_CONSOLE = "validator-engine-console";

    public Pair<String, Process> execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (isWindows() ? VALIDATOR_ENGINE_CONSOLE_EXE : VALIDATOR_ENGINE_CONSOLE);

        String[] withBinaryCommand = {binaryPath};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.debug("execute: {}", String.join(" ", withBinaryCommand));

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(binaryPath).getParent()));
            Process p = pb.start();

            String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();

            return Pair.of(resultInput, p);

        } catch (final IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
