package org.ton.executors.createstate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static com.sun.javafx.PlatformUtil.isWindows;

@Slf4j
public class CreateStateExecutor {

    private static final String CREATE_STATE_EXE = "create-state.exe";
    private static final String CREATE_STATE = "create-state";

    public String execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (isWindows() ? CREATE_STATE_EXE : CREATE_STATE);
        String[] withBinaryCommand = {binaryPath};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.debug("execute: {}", String.join(" ", withBinaryCommand));

            ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("FIFTPATH", node.getTonBinDir() + "lib" + ":" + node.getTonBinDir() + "smartcont");
            pb.directory(new File(node.getTonBinDir() + "zerostate" + File.separator));
            Process p = pb.start();

            String result = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

            p.getInputStream().close();
            p.getErrorStream().close();
            p.getOutputStream().close();

            return result;

        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
