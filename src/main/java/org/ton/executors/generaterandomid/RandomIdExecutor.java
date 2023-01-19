package org.ton.executors.generaterandomid;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
public class RandomIdExecutor {

    private static final String RANDOM_ID_EXE = "generate-random-id.exe";
    private static final String RANDOM_ID = "generate-random-id";

    public String execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? RANDOM_ID_EXE : RANDOM_ID);
        String[] withBinaryCommand = {binaryPath};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.debug("execute: {}", String.join(" ", withBinaryCommand));

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(binaryPath).getParent()));
            Process p = pb.start();

            String result = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();

            return result;

        } catch (final IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
