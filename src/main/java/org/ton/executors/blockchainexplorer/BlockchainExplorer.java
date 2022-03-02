package org.ton.executors.blockchainexplorer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.actions.MyLocalTon;
import org.ton.settings.Node;
import org.ton.utils.Utils;

import java.util.concurrent.Future;

@Slf4j
public class BlockchainExplorer {

    public static final String MY_LOCAL_TON = "myLocalTon";

    public Process startBlockchainExplorer(Node node, String globalConfigFile, int port) {
        Pair<Process, Future<String>> blockchainExplorerExecutor = new BlockchainExplorerExecutor().execute(node,
                "-v", Utils.getTonLogLevel(MyLocalTon.getInstance().getSettings().getLogSettings().getTonLogLevel()),
                //"-v", "1",
                "-C", globalConfigFile,
                "-H", String.valueOf(port));
        // "-a", node.getPublicIp() + ":" + node.getLiteServerPort());
        node.setBlockchainExplorerProcess(blockchainExplorerExecutor.getLeft());
        return blockchainExplorerExecutor.getLeft();
    }
}
