package org.ton.liteclient;

import com.anarsoft.vmlens.concurrent.junit.ConcurrentTestRunner;
import com.anarsoft.vmlens.concurrent.junit.ThreadCount;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.settings.GenesisNode;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(ConcurrentTestRunner.class)
public class LiteClientExecutorThreadsTest {

    private static final String CURRENT_DIR = System.getProperty("user.dir");
    private static final String TESTNET_CONFIG_LOCATION = CURRENT_DIR + File.separator + "testnet-global.config.json";

    private LiteClient liteClient;
    private Node testNode;

    @Before
    public void executedBeforeEach() throws IOException {
        InputStream TESTNET_CONFIG = IOUtils.toBufferedInputStream(getClass().getResourceAsStream("/testnet-global.config.json"));
        Files.copy(TESTNET_CONFIG, Paths.get(TESTNET_CONFIG_LOCATION), StandardCopyOption.REPLACE_EXISTING);

        liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);

        testNode = new GenesisNode();
        testNode.extractBinaries();
        testNode.setNodeGlobalConfigLocation(TESTNET_CONFIG_LOCATION);
    }

    @Test
    @ThreadCount(6)
    public void testLiteClientLastThreads() throws Exception {
        String resultLast = liteClient.executeLast(testNode);
        assertThat(resultLast).isNotEmpty();
        ResultLastBlock resultLastBlock = LiteClientParser.parseLast(resultLast);
        log.info("testLiteClientLastThreads tonBlockId {}", resultLastBlock);
        assertThat(resultLastBlock).isNotNull();
        String resultShards = liteClient.executeAllshards(testNode, resultLastBlock);
        log.info("testLiteClientLastThreads resultShards {}", resultShards);
        assertThat(resultShards).isNotEmpty();
        String resultBlock = liteClient.executeDumpblock(testNode, resultLastBlock);
        assertThat(resultBlock).isNotEmpty();
    }
}