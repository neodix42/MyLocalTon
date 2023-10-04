package org.ton.executors.tonhttpapi;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.settings.Node;

import java.util.concurrent.Future;

@Slf4j
public class TonHttpApi {
    public Process startTonHttpApi(Node node, String globalConfigFile, int port) {
        Pair<Process, Future<String>> tonHttpApiExecutor = new TonHttpApiExecutor().execute(node,
                "--liteserver-config", globalConfigFile,
                "--port", String.valueOf(port));
        node.setTonHttpApiProcess(tonHttpApiExecutor.getLeft());
        return tonHttpApiExecutor.getLeft();
    }
}
