package org.ton.executors.createhardfork;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.settings.Node;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

@Slf4j
public class Hardfork {

    private static final String MY_TON_FORKED_CONFIG_JSON = "my-ton-forked.config.json";

    //public void createHardFork(Node node, ResultLastBlock lastBlock, String externalMsgLocation) {
    public void createHardFork(Node forkedNode, Node toNode) {
        /*

        getStats(forkedNode);

        log.info("run create-hardfork");
        //String externalMsgLocation = createExternalMessage(node, toNode);
        //log.info("sleep 7sec");
        //Thread.sleep(7000);
        //get last block id
        //ResultLastBlock lastBlock = getLastBlock(node);

        WalletAddress fromWalletAddress = settings.getNode(forkedNode).getWalletAddress();
        SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                .executionNode(forkedNode)
                .fromWallet(fromWalletAddress)
                .fromWalletVersion(WalletVersion.V1)
                .fromSubWalletId(-1L)
                .destAddr(toNode.getWalletAddress().getBounceableAddressBase64url())
                .amount(new BigDecimal(123L))
                .build();
        String externalMsgLocation = new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
        //String externalMsgLocation = new Wallet().getSeqNoAndPrepareBoc(node, fromWalletAddress, toNode.getWalletAddress().getBounceableAddress(), new BigDecimal(123L), null);
        settings.setExternalMsgLocation(externalMsgLocation);
        saveSettingsToGson();

        log.info("sleep 5sec");
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); // 10 toncoins was
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
        Thread.sleep(3 * 1000L);

        log.info("**************** {} balance before: {}", forkedNode.getNodeName(), LiteClientParser.parseGetAccount(LiteClientExecutor.getInstance().executeGetAccount(forkedNode, toNode.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());

        //get last block id
        ResultLastBlock forkFromBlock = getLastBlock(forkedNode);

        Thread.sleep(60 * 1000L);

        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); //20toncoins
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);

        getStats(forkedNode);

        Thread.sleep(60 * 1000L);

        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); // 30 toncoins was
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);

        getStats(forkedNode);

        Thread.sleep(60 * 1000L);

        getStats(forkedNode);

        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam); // 40 toncoins was
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);
        Thread.sleep(3 * 1000L);
        new Wallet().getSeqNoAndSendTonCoins(sendToncoinsParam);

        //        settings.getGenesisNode().getNodeProcess().destroy();
        //        settings.getNode2().getNodeProcess().destroy();
        //        settings.getNode3().getNodeProcess().destroy();
        //        settings.getNode4().getNodeProcess().destroy();
        //
        //        settings.getNode5().getNodeProcess().destroy();
        //        settings.getNode6().getNodeProcess().destroy();
        //        settings.getNode7().getNodeProcess().destroy();

        log.info(LiteClientExecutor.getInstance().executeGetCurrentValidators(forkedNode)); // TODO PARSING
        //stop instance
        forkedNode.getNodeProcess().destroy();
        //newNode1.getNodeProcess().destroy();
        //newNode2.getNodeProcess().destroy();

        Thread.sleep(1000L);
        ResultLastBlock newBlock = generateNewBlock(forkedNode, forkFromBlock, externalMsgLocation);

        // reuse dht-server and use lite-server of node4
        addHardForkEntryIntoMyGlobalConfig(forkedNode, forkedNode.getNodeGlobalConfigLocation(), newBlock);

        startValidator(forkedNode, forkedNode.getNodeForkedGlobalConfigLocation());

        FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(settings.getNode2().getNodeForkedGlobalConfigLocation()));
        FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(settings.getNode3().getNodeForkedGlobalConfigLocation()));
        FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(settings.getNode2().getTonDbStaticDir()));
        FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(settings.getNode3().getTonDbStaticDir()));

        settings.getNode2().getNodeProcess().destroy();
        settings.getNode3().getNodeProcess().destroy();

        startValidator(settings.getNode2(), settings.getNode2().getNodeForkedGlobalConfigLocation());
        startValidator(settings.getNode3(), forkedNode.getNodeForkedGlobalConfigLocation());

        log.info("All forked nodes are started. Sleep 60 sec");
        Thread.sleep(60 * 1000L);
        // but with forked config


    /*
            //recreateDhtServer(genesisForkedNode, genesisForkedNode.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON); 1
            //idea, update global config with new liteserver
            //recreateLiteServer(node);
            //init

            //recreateLocalConfigJsonAndValidatorAccess(genesisForkedNode, genesisForkedNode.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON); 2
            //recreateLiteServer(genesisForkedNode); 3
            // error after above line with lite-server recreated
            //[adnl-ext-server.cpp:34][!manager]	failed ext query: [Error : 651 : node not synced]
            // error from lite-client
            // cannot get masterchain info from server
            //0. delete keyring directory. Execute generateValidatorKeys then

            // here lite-client can't connect, need to recreate adnl and val keys
    //        log.info("recreating new node keys and adnl address addresses");
    //        Files.deleteIfExists(Paths.get(forkedNode.getTonDbDir() + File.separator + "temporary"));
    //        log.info("Starting temporary full-node...");

            // re-elect this node
            //Process forkedValidator = createGenesisValidator(node, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON);
            // node4 valik,
            // completed sync. Validating 3 groups, reused 1 times and fails with:
            //  AdnlPeerTableImpl::subscribe - [adnl-peer-table.cpp:234][!PeerTable][&it != local_ids_.end()]
            //  smth like current validators cannot be found in dht server
            // if node is not validator - Validating 0 groups and not failing

            //start first forked full node OK (he is not validator if we compare with genesis validator)
            Process forkedValidator = startValidator(forkedNode, forkedNode.getNodeForkedGlobalConfigLocation());
            log.info("sleep 8sec");
            Thread.sleep(8 * 1000);
            // wait election id
            // replacing original global config with forked one
            FileUtils.copyFile(new File(forkedNode.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON), new File(forkedNode.getNodeGlobalConfigLocation()));
            //long electionId = new LiteClientExecutor2().executeGetActiveElectionId(genesisForkedNode, settings.getElectorSmcAddrHex());

            convertFullNodeToValidator(forkedNode, 0L, settings.getElectedFor());
            // completed sync. Validating 0 groups
            // error - "too big masterchain seqno"
            // if patch  with "if (!force) {" then error - "too small read"

            // a eto chto bi validator bil initial
    //        log.info("killing temp validator");
    //        if (forkedValidator != null) {
    //            forkedValidator.destroy();
    //        }
    //        startValidator(node, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON);

            // create neighbors

            log.info("Adding new node6");
    //  cant join other full nodes: Check `block_id_.seqno() >= opts_->get_last_fork_masterchain_seqno()` failed
    //  add static/ forked files to the node and above disappears

            // node6
            newNode1.getNodeProcess().destroy();
            FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(newNode1.getTonDbStaticDir()));
            FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(newNode1.getNodeForkedGlobalConfigLocation()));
            recreateLocalConfigJsonAndValidatorAccess(newNode1, newNode1.getNodeForkedGlobalConfigLocation());
            recreateLiteServer(newNode1);
            startValidator(newNode1, forkedNode.getNodeForkedGlobalConfigLocation());
            convertFullNodeToValidator(newNode1, 0L, settings.getElectedFor()); // completed sync. Validating 0 groups

            log.info("Adding new node7");
            // node7
            newNode2.getNodeProcess().destroy();
            FileUtils.copyDirectory(new File(forkedNode.getTonDbStaticDir()), new File(newNode2.getTonDbStaticDir()));
            FileUtils.copyFile(new File(forkedNode.getNodeForkedGlobalConfigLocation()), new File(newNode2.getNodeForkedGlobalConfigLocation()));
            recreateLocalConfigJsonAndValidatorAccess(newNode2, newNode2.getNodeForkedGlobalConfigLocation());
            recreateLiteServer(newNode2);
            startValidator(newNode2, forkedNode.getNodeForkedGlobalConfigLocation());
            convertFullNodeToValidator(newNode2, 0L, settings.getElectedFor()); // completed sync. Validating 0 groups

            new Utils().showThreads();

            while (true) {
                ResultLastBlock block = getLastBlockFromForked(forkedNode);
                log.info("last from {}          {}", settings.getNode2().getNodeName(), getLastBlockFromForked(settings.getNode2()));
                log.info("last from {}          {}", settings.getNode3().getNodeName(), getLastBlockFromForked(settings.getNode3()));
                log.info("last from {}          {}", forkedNode.getNodeName(), getLastBlockFromForked(forkedNode));
                log.info("**************** {} balance after: {}", forkedNode.getNodeName(), LiteClientParser.parseGetAccount(LiteClientExecutor.getInstance(true).executeGetAccount(forkedNode, toNode.getWalletAddress().getFullWalletAddress())).getBalance().getToncoins());

                log.info("hashes on {} {}", settings.getNode2(), LiteClientExecutor.getInstance(true).executeBySeqno(settings.getNode2(), block.getWc(), block.getShard(), block.getSeqno()));
                log.info("hashes on {} {}", settings.getNode3(), LiteClientExecutor.getInstance(true).executeBySeqno(settings.getNode3(), block.getWc(), block.getShard(), block.getSeqno()));
                log.info("hashes on {} {}", forkedNode.getNodeName(), LiteClientExecutor.getInstance(true).executeBySeqno(forkedNode, block.getWc(), block.getShard(), block.getSeqno()));

                Thread.sleep(10 * 1000L);
            }


    //        Thread.sleep(80 * 1000);
    //
    //        new LiteClientExecutor2().executeGetActiveElectionId(forkedNode, settings.getElectorSmcAddrHex());
    //
    //        String stdout = new LiteClientExecutor2().executeGetCurrentValidators(forkedNode);
    //        log.info(stdout);
    //
    //        stdout = new LiteClientExecutor2().executeGetPreviousValidators(forkedNode);
    //        log.info(stdout);
    //
    //        stdout = new LiteClientExecutor2().executeGetParticipantList(forkedNode, settings.getElectorSmcAddrHex());
    //        log.info(stdout);
    //
    //        Thread.sleep(80 * 1000);
    //
    //        new LiteClientExecutor2().executeGetActiveElectionId(forkedNode, settings.getElectorSmcAddrHex());
    //        // wait election id
    //        // send partipation request


            //startValidator(newNode, node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON);
            // fails with Check `block_id_.seqno() >= opts_->get_last_fork_masterchain_seqno()` failed
            // better to take synced node

         */
    }

    public ResultLastBlock generateNewBlock(Node node, ResultLastBlock lastBlock, String externalMsgLocation) throws Exception {

        //run create-hardfork, creates new block and put state in static directory
        Pair<Process, Future<String>> hardForkOutput = new HardforkExecutor().execute(node,
                "-D", node.getTonDbDir(),
                "-T", lastBlock.getFullBlockSeqno(),
                "-w", lastBlock.getWc() + ":" + lastBlock.getShard()
                //        ,"-m", externalMsgLocation
        );

        String newBlockOutput = hardForkOutput.getRight().get().toString();
        log.info("create-hardfork output {}", newBlockOutput);

        ResultLastBlock newBlock;
        if (newBlockOutput.contains("created block") && newBlockOutput.contains("success, block")) {
            newBlock = LiteClientParser.parseCreateHardFork(newBlockOutput);
            log.info("parsed new block {}", newBlock);

        } else {
            throw new Exception("Can't create block using create-hardfork utility.");
        }
        return newBlock;
    }

    public void addHardForkEntryIntoMyGlobalConfig(Node node, String globalConfig, ResultLastBlock newBlock) throws IOException, DecoderException {
        //add forks 1. put "hardforks": [ into global config, filehash and roothash taken from new block
        String myLocalTonConfig = FileUtils.readFileToString(new File(globalConfig), StandardCharsets.UTF_8);
        String replacedMyLocalTonConfig = StringUtils.replace(myLocalTonConfig, "\"validator\": {", "\"validator\": {\n    \"hardforks\": [\n" +
                "\t    {\n" +
                "\t\t    \"file_hash\": \"" + Base64.encodeBase64String(Hex.decodeHex(newBlock.getFileHash())) + "\",\n" +
                "\t\t    \"seqno\": " + newBlock.getSeqno() + ",\n" +
                "\t\t    \"root_hash\": \"" + Base64.encodeBase64String(Hex.decodeHex(newBlock.getRootHash())) + "\",\n" +
                "\t\t    \"workchain\": " + newBlock.getWc() + ",\n" +
                "\t\t    \"shard\": " + new BigInteger(newBlock.getShard(), 16).longValue() + "\n" +
                "\t    }\n" +
                "    ],");

        FileUtils.writeStringToFile(new File(node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON), replacedMyLocalTonConfig, StandardCharsets.UTF_8);
        log.debug("added hardforks to {}: {}", node.getTonDbDir() + MY_TON_FORKED_CONFIG_JSON, replacedMyLocalTonConfig);
    }
}