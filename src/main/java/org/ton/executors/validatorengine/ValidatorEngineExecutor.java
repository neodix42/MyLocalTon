package org.ton.executors.validatorengine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.main.Main;
import org.ton.settings.Node;
import org.ton.utils.MyLocalTonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.ton.main.App.mainController;

@Slf4j
public class ValidatorEngineExecutor {

    private static final String VALIDATOR_ENGINE_EXE = "validator-engine.exe";
    private static final String VALIDATOR_ENGINE = "validator-engine";

    public Pair<Process, Future<String>> execute(Node node, String... command) {

        String binaryPath = node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? VALIDATOR_ENGINE_EXE : VALIDATOR_ENGINE);

        String[] withBinaryCommand = {binaryPath};
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

        try {
            log.debug("execute: {}", String.join(" ", withBinaryCommand));

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

            pb.directory(new File(new File(binaryPath).getParent()));
            Process p = pb.start();

            CompletableFuture onProcessExit = p.onExit();

            onProcessExit.thenAccept(ph -> {
                log.debug("node {} with PID {} has stopped, exitValue {}", node.getNodeName(), ((Process) ph).pid(), ((Process) ph).exitValue());

                if ((((Process) ph).exitValue() > 0) && (Main.appActive.get())) {

                    if (node.getFlag().equals("cloned")) {
                        log.info("re-starting validator {}...", node.getNodeName());
                        long pid = new ValidatorEngine().startValidator(node, node.getNodeGlobalConfigLocation()).pid();
                        log.info("re-started validator {} with pid {}", node.getNodeName(), pid);
                    } else {
                        if (!node.getNodeName().equals("genesis")) {
                            log.info("failed {} creation, delete it", node.getNodeName());
                            ExecutorService service = Executors.newSingleThreadExecutor();
                            service.execute(() -> {
                                MyLocalTonUtils.doDelete(node.getNodeName());
                                mainController.showDialogMessage("Error", "Validator " + node.getNodeName() + " could not be created. For more information refer to the log files.");
                            });
                            service.shutdown();
                        }
                    }
                }
            });

            Future<String> future = executorService.submit(() -> {
                try {
                    Thread.currentThread().setName("validator-engine-" + node.getNodeName());

                    String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                    log.info("validator-engine-{} stopped", node.getNodeName());
                    log.debug("validator exit output: {} ", resultInput);
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
