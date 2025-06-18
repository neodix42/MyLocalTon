package org.ton.mylocalton.executors.tonhttpapi;

import static org.ton.mylocalton.main.App.mainController;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.utils.Utils;
import org.ton.mylocalton.settings.Node;

@Slf4j
public class TonHttpApiExecutor {

  public Pair<Process, Future<String>> execute(Node node, String... command) {

    try {
      String binaryPath = "";

      if (SystemUtils.IS_OS_WINDOWS) {
        binaryPath = "ton-http-api";
      } else if (SystemUtils.IS_OS_LINUX) {
        binaryPath = System.getenv("HOME") + "/.local/bin/ton-http-api";
      } else if (SystemUtils.IS_OS_MAC) {
        binaryPath = "ton-http-api";
      } else {
        log.error("unsupported OS");
      }

      String[] withBinaryCommand = {binaryPath};
      withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

      log.info("execute: {}", String.join(" ", withBinaryCommand));
      ExecutorService executorService = Executors.newSingleThreadExecutor();

      final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand);

      Process p = pb.start();
      Utils.sleep(5);
      Future<String> future =
          executorService.submit(
              () -> {
                try {
                  Thread.currentThread().setName("ton-http-api-" + node.getNodeName());

                  String resultInput =
                      IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

                  log.info("{} stopped", "ton-http-api-" + node.getNodeName());
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
      mainController.showErrorMsg("Error starting starting ton-http-api", 5);
      return null;
    }
  }
}
