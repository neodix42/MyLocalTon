package org.ton.executors.dhtserver;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.executors.generaterandomid.RandomIdExecutor;
import org.ton.settings.Node;
import org.ton.utils.MyLocalTonUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static java.util.Objects.isNull;

@Slf4j
public class DhtServer {

    private static final String CURRENT_DIR = System.getProperty("user.dir");
    public static final String MY_LOCAL_TON = "myLocalTon";
    public static final String TEMPLATES = "templates";
    public static final String EXAMPLE_GLOBAL_CONFIG = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + TEMPLATES + File.separator + "example.config.json";

    public void startDhtServer(Node node, String globalConfigFile) {

        Pair<Process, Future<String>> dhtServer = new DhtServerExecutor().execute(node,
                "-v", MyLocalTonUtils.getTonLogLevel(node.getTonLogLevel()),
                "-t", "2",
                "-C", globalConfigFile,
                "-l", node.getDhtServerDir() + MyLocalTonUtils.toUtcNoSpace(System.currentTimeMillis()),
                "-D", node.getDhtServerDir(),
                "-I", node.getPublicIp() + ":" + node.getDhtPort());
        node.setDhtServerProcess(dhtServer.getLeft());

        log.info("{} dht-server started at {}", node.getNodeName(), node.getPublicIp() + ":" + node.getDhtPort());
    }

    /**
     * Creates dht-server/keyring directory with a key inside and generate config.json based on example.config.json.
     */
    public List<String> initDhtServer(Node node) throws Exception {
        if (!Files.exists(Paths.get(node.getDhtServerDir()), LinkOption.NOFOLLOW_LINKS)) {

            node.extractBinaries();

            int publicIpNum = MyLocalTonUtils.getIntegerIp(node.getPublicIp());
            log.debug("publicIpNum {}", publicIpNum);

            Files.createDirectories(Paths.get(node.getDhtServerDir()));

            log.info("Initializing DHT server"); // creating key in dht-server/keyring/hex and config.json
            Pair<Process, Future<String>> dhtServerInit = new DhtServerExecutor().execute(node,
                    "-v", MyLocalTonUtils.getTonLogLevel(node.getTonLogLevel()),
                    "-t", "1",
                    "-C", EXAMPLE_GLOBAL_CONFIG,
                    "-l", node.getDhtServerDir() + MyLocalTonUtils.toUtcNoSpace(System.currentTimeMillis()),
                    "-D", node.getDhtServerDir(),
                    "-I", node.getPublicIp() + ":" + node.getDhtPort());

            log.debug("dht-server result: {}", dhtServerInit.getRight().get()); // wait for process to exit

            Thread.sleep(100);

            if (!Files.exists(Paths.get(node.getDhtServerDir() + "config.json"), LinkOption.NOFOLLOW_LINKS)) {
                log.error("Initialization of DHT server failed. File {} was not created.", node.getDhtServerDir() + "config.json");
                System.exit(11);
            }

            MyLocalTonUtils.replaceOutPortInConfigJson(node.getDhtServerDir(), node.getDhtOutPort()); // no need - FYI - config.json update?

            return generateDhtKeys(node, publicIpNum);
        } else {
            log.debug("DHT server initialized. Skipping.");
            return new ArrayList<>();
        }
    }

    /**
     * Adds dht nodes into "nodes:[]" structure inside my-ton-global.config.json
     *
     * @param dhtNodes       - list of generated dht nodes
     * @param myGlobalConfig - global genesis config
     * @throws IOException - exception if global config not found
     */
    public void addDhtNodesToGlobalConfig(List<String> dhtNodes, String myGlobalConfig) throws IOException {
        if (dhtNodes.isEmpty()) {
            return;
        }

        String globalConfigContent = FileUtils.readFileToString(new File(myGlobalConfig), StandardCharsets.UTF_8);

        if (globalConfigContent.contains("\"nodes\": []")) { //very first creation
            log.debug("Replace NODES placeholder with dht-server entry");
            String replaced = StringUtils.replace(globalConfigContent, "\"nodes\": []", "\"nodes\": [" + String.join(",", dhtNodes) + "]");
            FileUtils.writeStringToFile(new File(myGlobalConfig), replaced, StandardCharsets.UTF_8);
            log.debug("dht-nodes added: {}", Files.readString(Paths.get(myGlobalConfig), StandardCharsets.UTF_8));
        } else { // modify existing
            log.debug("Replace current list of dht nodes with a new one");
            String existingNodes = MyLocalTonUtils.sbb(globalConfigContent, "\"nodes\": [");
            String replacedLocalConfig = StringUtils.replace(globalConfigContent, existingNodes, StringUtils.substring(existingNodes, 0, -1) + "," + String.join(",", dhtNodes) + "]");
            FileUtils.writeStringToFile(new File(myGlobalConfig), replacedLocalConfig, StandardCharsets.UTF_8);
            log.debug("dht-nodes updated: {}", Files.readString(Paths.get(myGlobalConfig), StandardCharsets.UTF_8));
        }
    }

    private List<String> generateDhtKeys(Node node, long publicIpNum) throws Exception {

        List<String> dhtNodes = new ArrayList<>();

        String[] keyFiles = new File(node.getDhtServerKeyringDir()).list();

        if (!isNull(keyFiles) && keyFiles.length == 0) {
            throw new Exception("No keyrings found in " + node.getTonDbKeyringDir());
        }
        for (String file : keyFiles) {
            if (file.length() == 64) { //take only hash files
                log.debug("found keyring file {}", file);

                if (SystemUtils.IS_OS_WINDOWS) {
                    dhtNodes.add(new RandomIdExecutor().execute(node, "-m", "dht", "-k", node.getDhtServerKeyringDir() + file,
                            "-a", "\"{\\\"@type\\\": \\\"adnl.addressList\\\",  \\\"addrs\\\":[{\\\"@type\\\": \\\"adnl.address.udp\\\", \\\"ip\\\": " + publicIpNum + ", \\\"port\\\": " + node.getDhtPort() + " } ], \\\"version\\\": 0, \\\"reinit_date\\\": 0, \\\"priority\\\": 0, \\\"expire_at\\\": 0}\""));
                } else {
                    dhtNodes.add(new RandomIdExecutor().execute(node, "-m", "dht", "-k", node.getDhtServerKeyringDir() + file,
                            "-a", "{\"@type\": \"adnl.addressList\", \"addrs\":[{\"@type\": \"adnl.address.udp\", \"ip\": " + publicIpNum + ", \"port\": " + node.getDhtPort() + " } ], \"version\": 0, \"reinit_date\": 0, \"priority\": 0, \"expire_at\": 0}"));
                }
            }
        }

        log.debug(String.join(",", dhtNodes));

        return dhtNodes;
    }
}
