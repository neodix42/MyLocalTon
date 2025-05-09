package org.ton.mylocalton.executors.validatorengine;

import static org.ton.mylocalton.main.App.mainController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.mylocalton.main.Main;
import org.ton.mylocalton.settings.Node;
import org.ton.mylocalton.utils.MyLocalTonUtils;

@Slf4j
public class ValidatorEngineExecutor {

  private static final String VALIDATOR_ENGINE_EXE = "validator-engine.exe";
  private static final String VALIDATOR_ENGINE = "validator-engine";

  public Pair<Process, Future<String>> execute(Node node, String... command) {

    String binaryPath =
        node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? VALIDATOR_ENGINE_EXE : VALIDATOR_ENGINE);

    String[] withBinaryCommand = {binaryPath};
    withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

    try {
      log.debug("execute: {}", String.join(" ", withBinaryCommand));

      final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);

      pb.directory(new File(new File(binaryPath).getParent()));
      Process p = pb.start();
      p.waitFor(5, TimeUnit.SECONDS);
      CompletableFuture<Process> onProcessExit = p.onExit();

      onProcessExit.thenAccept(
          ph -> {
            log.debug(
                "node {} with PID {} has stopped, exitValue {}",
                node.getNodeName(),
                ph.pid(),
                ph.exitValue());

            if (ph.exitValue() == 1) {
              log.error(
                  "node {} with PID {} has stopped. FATAL ERROR - see node's log file!",
                  node.getNodeName(),
                  ph.pid());
            }

            if ((ph.exitValue() > 0) && (Main.appActive.get())) {

              if (node.getFlag().equals("cloned")) {
                log.info("re-starting validator {}...", node.getNodeName());
                long pid =
                    new ValidatorEngine()
                        .startValidator(node, node.getNodeGlobalConfigLocation())
                        .pid();
                log.info("re-started validator {} with pid {}", node.getNodeName(), pid);
              } else {
                if (!node.getNodeName().equals("genesis")) {
                  log.info("failed {} creation, delete it", node.getNodeName());
                  ForkJoinPool.commonPool()
                      .execute(
                          () -> {
                            MyLocalTonUtils.doDelete(node.getNodeName());
                            mainController.showDialogMessage(
                                "Error",
                                "Validator "
                                    + node.getNodeName()
                                    + " could not be created. For more information refer to the log files.");
                          });
                }
              }
            }
          });

      Future<String> future =
          ForkJoinPool.commonPool()
              .submit(
                  () -> {
                    try {
                      Thread.currentThread().setName("validator-engine-" + node.getNodeName());

                      String resultInput =
                          IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                      log.info("validator-engine-{} stopped", node.getNodeName());
                      //                    log.debug("validator exit output: {} ", resultInput);
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
