package org.ton.executors.validatorengine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.actions.MyLocalTon;
import org.ton.executors.generaterandomid.GenerateRandomId;
import org.ton.settings.Node;
import org.ton.utils.Utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
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
                "-C", myGlobalConfig,
                "--db", node.getTonDbDir(),
                "-l", node.getTonLogDir() + Utils.toUtcNoSpace(System.currentTimeMillis()),
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
            Pair<Process, Future<String>> validatorGenesisInit = validator.execute(node,
                    "-C", node.getNodeGlobalConfigLocation(),
                    "--db", node.getTonDbDir(),
                    "--ip", node.getPublicIp() + ":" + node.getPublicPort());
            log.debug("Initialized {} validator, result {}", node.getNodeName(), validatorGenesisInit.getRight().get());

            Utils.replaceOutPortInConfigJson(node.getTonDbDir(), node.getOutPort());

            //enable access to full node from validator-engine-console - required if you want to become validator later
            GenerateRandomId generateRandomId = new GenerateRandomId();
            String serverIdBase64 = generateRandomId.generateServerCertificate(node);
            generateRandomId.generateClientCertificate(node, serverIdBase64);
        }
    }

    public void enableLiteServer(Node node, String myGlobalConfig, boolean reinstall) throws Exception {

        if (Files.exists(Paths.get(node.getTonDbKeyringDir() + "liteserver")) && (!reinstall)) {
            log.info("lite-server exists! Skipping...");
        } else {
            log.info("Installing lite-server...");

            Pair<String, String> liteServerKeys = new GenerateRandomId().generateLiteServerKeys(node);
            String liteServers = "\"liteservers\" : [{\"id\":\"" + liteServerKeys.getRight() + "\",\"port\":\"" + node.getLiteServerPort() + "\"}";
            log.info("liteservers: {} ", liteServers);

            //convert pub key to key
            String liteserverPubkeyBase64 = Utils.convertPubKeyToBase64(node.getTonDbKeyringDir() + "liteserver.pub");
            int publicIpNum = Utils.getIntegerIp(node.getPublicIp());

            // replace lite servers array in config.json
            String configJson = FileUtils.readFileToString(new File(node.getTonDbDir() + "config.json"), StandardCharsets.UTF_8);
            String existingLiteservers = "\"liteservers\" : " + Utils.sbb(configJson, "\"liteservers\" : [");
            String configJsonNew = StringUtils.replace(configJson, existingLiteservers, liteServers + "\n]");
            FileUtils.writeStringToFile(new File(node.getTonDbDir() + "config.json"), configJsonNew, StandardCharsets.UTF_8);

            String myGlobalTonConfig = FileUtils.readFileToString(new File(myGlobalConfig), StandardCharsets.UTF_8);
            String myGlobalTonConfigNew;
            String liteServerConfigNew;

            if (myGlobalTonConfig.contains("liteservers")) {
                //replace exiting lite-servers in global config
                String existingLiteserver = Utils.sbb(myGlobalTonConfig, "\"liteservers\":[");
                liteServerConfigNew = "[{\"id\":{\"key\":\"" + liteserverPubkeyBase64 + "\", \"@type\":\"pub.ed25519\"}, \"port\": " + node.getLiteServerPort() + ", \"ip\": " + publicIpNum + "}\n]";
                myGlobalTonConfigNew = StringUtils.replace(myGlobalTonConfig, existingLiteserver, liteServerConfigNew);
                FileUtils.writeStringToFile(new File(myGlobalConfig), myGlobalTonConfigNew, StandardCharsets.UTF_8);
            } else {
                // add new lite servers around "validator":{
                liteServerConfigNew = "\"liteservers\":[{\"id\":{\"key\":\"" + liteserverPubkeyBase64 + "\", \"@type\":\"pub.ed25519\"}, \"port\": " + node.getLiteServerPort() + ", \"ip\": " + publicIpNum + "}\n],\n \"validator\": {";
                myGlobalTonConfigNew = StringUtils.replace(myGlobalTonConfig, "\"validator\": {", liteServerConfigNew);
                FileUtils.writeStringToFile(new File(myGlobalConfig), myGlobalTonConfigNew, StandardCharsets.UTF_8);
            }
            log.info("lite-server installed");
        }
    }
}
