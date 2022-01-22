package org.ton.executors.validatorengine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.actions.MyLocalTon;
import org.ton.executors.generaterandomid.GenerateRandomId;
import org.ton.settings.Node;
import org.ton.utils.Utils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Future;

@Slf4j
public class ValidatorEngine {

    public Process startValidator(Node node, String myGlobalConfig) {
        log.info("starting validator-engine {}", node.getNodeName());

        Pair<Process, Future<String>> validator = new ValidatorEngineExecutor().execute(node,
                "-v", Utils.getTonLogLevel(MyLocalTon.getInstance().getSettings().getLogSettings().getTonLogLevel()),
                "-t", "1",
                "-C", myGlobalConfig,
                "--db", node.getTonDbDir(),
                "-l", node.getTonLogDir() + Utils.toUtcNoSpace(System.currentTimeMillis()),
                "--ip", node.getPublicIp() + ":" + node.getPublicPort(),
                "-S", MyLocalTon.getInstance().getSettings().getBlockchainSettings().getValidatorSyncBefore().toString(), // 1 year, in initial sync download all blocks for last given seconds
                "-s", MyLocalTon.getInstance().getSettings().getBlockchainSettings().getValidatorStateTtl().toString(), // state will be gc'd after this time (in seconds), default 3600
                "-b", MyLocalTon.getInstance().getSettings().getBlockchainSettings().getValidatorBlockTtl().toString(), // blocks will be gc'd after this time (in seconds), default=7*86400
                "-A", MyLocalTon.getInstance().getSettings().getBlockchainSettings().getValidatorArchiveTtl().toString(), // archived blocks will be deleted after this time (in seconds), default 365*86400
                "-K", MyLocalTon.getInstance().getSettings().getBlockchainSettings().getValidatorKeyProofTtl().toString() // 10 years key blocks will be deleted after this time (in seconds), default 365*86400*10
        );
        node.setNodeProcess(validator.getLeft());
        return validator.getLeft();
    }

    public Pair<Process, Future<String>> startValidatorWithoutParams(Node node, String myGlobalConfig) {
        ValidatorEngineExecutor validatorGenesis = new ValidatorEngineExecutor();
        return validatorGenesis.execute(node,
                "-v", Utils.getTonLogLevel(MyLocalTon.getInstance().getSettings().getLogSettings().getTonLogLevel()),
                "-t", "1",
                "-l", node.getTonDbDir() + "validatorGenesis",
                "-C", myGlobalConfig,
                "--db", node.getTonDbDir(),
                "--ip", node.getPublicIp() + ":" + node.getPublicPort());
    }

    public void initFullnode(Node node, String sharedGlobalConfig) throws Exception {
        //run full node very first time
        if (Files.exists(Paths.get(node.getTonDbDir() + "state"))) {
            log.info("Found non-empty state; Skip initialization!");
        } else {
            log.info("Initializing node validator, create keyrings and config.json...");
            Files.copy(Paths.get(sharedGlobalConfig), Paths.get(node.getNodeGlobalConfigLocation()), StandardCopyOption.REPLACE_EXISTING);
            ValidatorEngineExecutor validator = new ValidatorEngineExecutor();
            Pair<Process, Future<String>> validatorGenesisInit = validator.execute(node, "-C", node.getNodeGlobalConfigLocation(), "--db", node.getTonDbDir(), "--ip", node.getPublicIp() + ":" + node.getPublicPort());
            log.debug("Initialized {} validator, result {}", node.getNodeName(), validatorGenesisInit.getRight().get());

            Utils.replaceOutPortInConfigJson(node.getTonDbDir(), node.getOutPort());

            //enable access to full node from validator-engine-console - required if you want to become validator later
            GenerateRandomId generateRandomId = new GenerateRandomId();
            String serverIdBase64 = generateRandomId.generateServerCertificate(node);
            generateRandomId.generateClientCertificate(node, serverIdBase64);
        }
    }
}
