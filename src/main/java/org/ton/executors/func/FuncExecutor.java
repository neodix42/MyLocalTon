package org.ton.executors.func;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
public class FuncExecutor {

    private static final String FUNC_EXE = "func.exe";
    private static final String FUNC = "func";
    private static final String CURRENT_DIR = System.getProperty("user.dir");

    private String installDir;

    public FuncExecutor(String pInstallDir) throws IOException {
        log.info("Working Directory = " + CURRENT_DIR);
        installDir = pInstallDir;
        extractFunc();
    }

    private void extractFunc() throws IOException {
        synchronized (FuncExecutor.class) {
            if (SystemUtils.IS_OS_WINDOWS) {
                if (Files.notExists(Paths.get(installDir + File.separator + FUNC_EXE), LinkOption.NOFOLLOW_LINKS)) {
                    log.info("extracting {} on Windows", FUNC_EXE);
                    Files.createDirectories(Paths.get(installDir));
                    InputStream windowsFunc = FuncExecutor.class.getClassLoader().getResourceAsStream("org/ton/binaries/" + FUNC + "/win64/" + FUNC_EXE); // TODO
                    Files.copy(windowsFunc, Paths.get(installDir + File.separator + FUNC_EXE), StandardCopyOption.REPLACE_EXISTING);
                    windowsFunc.close();
                    log.info("windows {} path: {}", FUNC_EXE, installDir + File.separator + FUNC_EXE);
                } else {
                    log.info("{} on Windows already extracted into {}, use it.", FUNC_EXE, installDir + File.separator + FUNC_EXE);
                }
            } else if (SystemUtils.IS_OS_LINUX) {
                if (Files.notExists(Paths.get(installDir + File.separator + FUNC), LinkOption.NOFOLLOW_LINKS)) {
                    log.info("extracting {} on Unix", FUNC);
                    Files.createDirectories(Paths.get(installDir));
                    InputStream ubuntuFunc = FuncExecutor.class.getClassLoader().getResourceAsStream("org/ton/binaries/" + FUNC + "/ubuntu/" + FUNC);
                    Files.copy(ubuntuFunc, Paths.get(installDir + File.separator + FUNC), StandardCopyOption.REPLACE_EXISTING);
                    ubuntuFunc.close();
                    new ProcessBuilder("chmod", "755", installDir + File.separator + FUNC).start();
                    log.info("unix {} path: {}", FUNC, installDir + File.separator + FUNC);
                } else {
                    log.info("{} on Unix already extracted into {}, use it.", FUNC, installDir + File.separator + FUNC);
                }
            } else {
                log.error("You are running neither on Windows nor Unix. We don't have compiled client for it.");
            }
        }
    }

    public String execute(final String command) {

        String binaryPath = installDir + File.separator + (SystemUtils.IS_OS_WINDOWS ? FUNC_EXE : FUNC);
        log.debug("binaryPath {}", binaryPath);

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
