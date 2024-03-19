package org.ton.executors.blockchainexplorer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;
import org.ton.utils.MyLocalTonUtils;

import java.util.concurrent.Future;

@Slf4j
public class BlockchainExplorer {

    public static final String MY_LOCAL_TON = "myLocalTon";

    public Process startBlockchainExplorer(Node node, String globalConfigFile, int port) {
        Pair<Process, Future<String>> blockchainExplorerExecutor = new BlockchainExplorerExecutor().execute(node, "-v", MyLocalTonUtils.getTonLogLevel(node.getTonLogLevel()),
                "-C", globalConfigFile,
                "-H", String.valueOf(port));
        // "-a", node.getPublicIp() + ":" + node.getLiteServerPort());
        node.setBlockchainExplorerProcess(blockchainExplorerExecutor.getLeft());
        log.info("{} native blockchain-explorer started at {}", node.getNodeName(), node.getPublicIp() + ":" + port);
        return blockchainExplorerExecutor.getLeft();
    }
}
