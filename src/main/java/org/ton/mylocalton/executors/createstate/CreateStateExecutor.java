package org.ton.mylocalton.executors.createstate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;
import org.ton.java.utils.Utils;
import org.ton.mylocalton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CreateStateExecutor {

  private static final String CREATE_STATE_EXE = "create-state.exe";
  private static final String CREATE_STATE = "create-state";

  public String execute(Node node, String... command) {

    String binaryPath =
        node.getTonBinDir() + (SystemUtils.IS_OS_WINDOWS ? CREATE_STATE_EXE : CREATE_STATE);
    String[] withBinaryCommand = {binaryPath};
    withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, command);

    try {
      log.debug("execute: {}", String.join(" ", withBinaryCommand));

      ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);
      Map<String, String> env = pb.environment();
      if ((Utils.getOS() == Utils.OS.WINDOWS) || (Utils.getOS() == Utils.OS.WINDOWS_ARM)) {
        log.debug(
            "set FIFTPATH="
                + node.getTonBinDir()
                + "lib"
                + "@"
                + node.getTonBinDir()
                + "smartcont");
        env.put("FIFTPATH", node.getTonBinDir() + "lib" + "@" + node.getTonBinDir() + "smartcont");
      } else {
        log.debug(
            "export FIFTPATH="
                + node.getTonBinDir()
                + "lib"
                + ":"
                + node.getTonBinDir()
                + "smartcont");
        env.put("FIFTPATH", node.getTonBinDir() + "lib" + ":" + node.getTonBinDir() + "smartcont");
      }
      pb.directory(new File(node.getTonBinDir() + "zerostate" + File.separator));
      Process p = pb.start();
      p.waitFor(5, TimeUnit.SECONDS);
      String result = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());

      p.getInputStream().close();
      p.getErrorStream().close();
      p.getOutputStream().close();

      return result;

    } catch (IOException e) {
      log.error(e.getMessage());
      return null;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
