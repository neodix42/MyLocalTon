package org.ton.executors.dhtserver;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.actions.MyLocalTon;
import org.ton.executors.generaterandomid.RandomIdExecutor;
import org.ton.settings.Node;
import org.ton.utils.Utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.isNull;

@Slf4j
public class DhtServer {
    public Process startDhtServer(Node node, String globalConfigFile) {
        // start dht-server in background
        log.info("genesis dht-server started at {}", node.getPublicIp() + ":" + node.getDhtPort());
        Pair<Process, Future<String>> dhtServer = new DhtServerExecutor().execute(node,
                "-v", Utils.getTonLogLevel(MyLocalTon.getInstance().getSettings().getLogSettings().getTonLogLevel()),
                "-t", "1",
                "-C", globalConfigFile,
                "-l", node.getDhtServerDir() + Utils.toUtcNoSpace(System.currentTimeMillis()),
                "-D", node.getDhtServerDir(),
                "-I", node.getPublicIp() + ":" + node.getDhtPort());
        node.setDhtServerProcess(dhtServer.getLeft());
        return dhtServer.getLeft();
    }

    /**
     * creates dht-server/keyring directory with a key inside and generate config.json based on example.config.json.
     * Also replaces [NODES] in my-ton-global.config.json
     */
    public void initDhtServer(Node node, String exampleConfigJson, String myGlobalConfig) throws Exception {
        int publicIpNum = Utils.getIntegerIp(node.getPublicIp());
        log.debug("publicIpNum {}", publicIpNum);
        Files.createDirectories(Paths.get(node.getDhtServerDir()));

        log.info("Initializing dht-server, creating key in dht-server/keyring/hex and config.json...");
        Pair<Process, Future<String>> dhtServerInit = new DhtServerExecutor().execute(node,
                "-v", Utils.getTonLogLevel(MyLocalTon.getInstance().getSettings().getLogSettings().getTonLogLevel()),
                "-t", "1",
                "-C", exampleConfigJson,
                "-D", node.getDhtServerDir(),
                "-I", node.getPublicIp() + ":" + node.getDhtPort());
        log.debug("dht-server result: {}", dhtServerInit.getRight().get());

        Utils.replaceOutPortInConfigJson(node.getDhtServerDir(), node.getDhtOutPort()); // no need - FYI - config.json update?

        List<String> dhtNodes = generateDhtKeys(node, publicIpNum);

        String content = FileUtils.readFileToString(new File(myGlobalConfig), StandardCharsets.UTF_8);
        if (content.contains("NODES")) { //very first creation
            log.debug("Replace NODES placeholder with dht-server entry");
            String replaced = StringUtils.replace(content, "NODES", String.join(",", dhtNodes));
            FileUtils.writeStringToFile(new File(myGlobalConfig), replaced, StandardCharsets.UTF_8);
            log.debug("dht-nodes added: {}", Files.readString(Paths.get(myGlobalConfig), StandardCharsets.UTF_8));
        } else { // modify existing
            log.debug("Replace current list of dht nodes with a new one");
            String existingNodes = Utils.sbb(content, "\"nodes\": [");
            String backToTemlate = StringUtils.replace(content, existingNodes, "[NODES]");
            String replacedLocalConfig = StringUtils.replace(backToTemlate, "NODES", String.join(",", dhtNodes));
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

                if (isWindows()) {
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
