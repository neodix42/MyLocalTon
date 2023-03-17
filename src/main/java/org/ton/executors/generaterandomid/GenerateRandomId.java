package org.ton.executors.generaterandomid;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;
import org.ton.utils.Extractor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * generate-random-id utility
 */
@Slf4j
public class GenerateRandomId {

    /**
     * Creates server key in db/ and puts into db/keyring/hex.
     * Replaces CONSOLE-PORT, SERVER-ID, CLIENT-ID in control.template and puts them into db/config.json
     */
    public void generateClientCertificate(Node node, String serverIdHuman) throws Exception {
        // Generating server certificate
        if (Files.exists(Paths.get(node.getTonBinDir() + "certs" + File.separator + "client"))) {
            log.info("Found existing client certificate, skipping");
        } else {
            log.debug("Generating client certificate for remote control");
            String clientIds = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonBinDir() + "certs" + File.separator + "client");
            String[] clientIdsBoth = clientIds.split(" ");
            String clientIdHex = clientIdsBoth[0].trim();
            String clientIdBase64 = clientIdsBoth[1].trim();
            log.debug("Generated client private certificate for {}: {} {}", node.getNodeName(), clientIdHex, clientIdBase64);

            //Adding client permissions
            String content = Files.readString(Paths.get(Extractor.MY_LOCAL_TON_ROOT_DIR + "templates" + File.separator + "control.template"), StandardCharsets.UTF_8);
            String replacedTemplate = StringUtils.replace(content, "CONSOLE-PORT", String.valueOf(node.getConsolePort()));
            replacedTemplate = StringUtils.replace(replacedTemplate, "SERVER-ID", "\"" + serverIdHuman + "\"");
            replacedTemplate = StringUtils.replace(replacedTemplate, "CLIENT-ID", "\"" + clientIdBase64 + "\"");

            String configFile = Files.readString(Paths.get(node.getTonDbDir() + "config.json"), StandardCharsets.UTF_8);
            String configNew = StringUtils.replace(configFile, "\"control\" : [", replacedTemplate);
            Files.writeString(Paths.get(node.getTonDbDir() + "config.json"), configNew, StandardOpenOption.CREATE);
        }
    }

    /**
     * Creates server key in db and puts into directory db/keyring/hex.
     */
    public String generateServerCertificate(Node node) throws Exception {
        // Generating server certificate
        if (Files.exists(Paths.get(node.getTonBinDir() + "certs" + File.separator + "server"))) {
            log.info("Found existing server certificate, skipping!");
            return null;
        } else {
            log.debug("Generating and installing server certificate for remote control");
            String serverIds = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonBinDir() + "certs" + File.separator + "server");
            String[] serverIdsBoth = serverIds.split(" ");
            String serverIdHex = serverIdsBoth[0].trim();
            String serverIdBase64 = serverIdsBoth[1].trim();
            log.debug("Server IDs for {}: {} {}", node.getNodeName(), serverIdHex, serverIdBase64);
            Files.copy(Paths.get(node.getTonBinDir() + "certs" + File.separator + "server"), Paths.get(node.getTonDbKeyringDir() + serverIdHex), StandardCopyOption.REPLACE_EXISTING);

            return serverIdBase64;
        }
    }

    public Pair<String, String> generateLiteServerKeys(Node node) throws Exception {
        String liteserverKeys = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonDbKeyringDir() + "liteserver");
        String[] liteServerHexBase64 = liteserverKeys.split(" ");
        String liteServerIdHex = liteServerHexBase64[0].trim();
        String liteServerIdBase64 = liteServerHexBase64[1].trim();
        log.debug("liteServerIdHex {}, liteServerIdBase64 {} on {}", liteServerIdHex, liteServerIdBase64, node.getNodeName());

        Files.copy(Paths.get(node.getTonDbKeyringDir() + "liteserver"), Paths.get(node.getTonDbKeyringDir() + liteServerIdHex), StandardCopyOption.REPLACE_EXISTING);
        return Pair.of(liteServerIdHex, liteServerIdBase64);
    }
}
