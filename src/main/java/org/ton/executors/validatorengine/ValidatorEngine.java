package org.ton.executors.validatorengine;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.actions.MyLocalTon;
import org.ton.executors.createstate.CreateStateExecutor;
import org.ton.executors.generaterandomid.GenerateRandomId;
import org.ton.executors.generaterandomid.RandomIdExecutor;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.utils.Extractor;
import org.ton.utils.MyLocalTonUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.concurrent.Future;

@Slf4j
public class ValidatorEngine {
    private static final String VALIDATOR = "validator";
    private static final String ZEROSTATE = "zerostate";
    private static final String DOUBLE_SPACE = "  ";
    private static final String SPACE = " ";
    private static final String TEMPLATES = "templates";
    private static final String CURRENT_DIR = System.getProperty("user.dir");
    private static final String MY_LOCAL_TON = "myLocalTon";

    public Process startValidator(Node node, String myGlobalConfig) {
        log.info("starting validator-engine {}", node.getNodeName());

        Pair<Process, Future<String>> validator = new ValidatorEngineExecutor().execute(node,
                "-v", MyLocalTonUtils.getTonLogLevel(node.getTonLogLevel()),
                "-t", "2",
                "-C", myGlobalConfig,
                "--db", node.getTonDbDir(),
                "-l", node.getTonLogDir() + MyLocalTonUtils.toUtcNoSpace(System.currentTimeMillis()),
                "--ip", node.getPublicIp() + ":" + node.getPublicPort(),
                "-S", node.getValidatorSyncBefore().toString(), // initial sync download all blocks for last given seconds, default 3600
                "-s", node.getValidatorStateTtl().toString(), // state will be gc'd after this time (in seconds), default 3600
                "-b", node.getValidatorBlockTtl().toString(), // blocks will be gc'd after this time (in seconds), default=7*86400
                "-A", node.getValidatorArchiveTtl().toString(), // archived blocks will be deleted after this time (in seconds), default 365*86400
                "-K", node.getValidatorKeyProofTtl().toString() // 10 years key blocks will be deleted after this time (in seconds), default 365*86400*10
        );
        node.setNodeProcess(validator.getLeft());
        log.info("{} validator-engine started at {}", node.getNodeName(), node.getPublicIp() + ":" + node.getPublicPort());
        return validator.getLeft();
    }

    public Pair<Process, Future<String>> startValidatorWithoutParams(Node node, String myGlobalConfig) {
        log.debug("starting validator-engine without params {}", node.getNodeName());

        Pair<Process, Future<String>> validator = new ValidatorEngineExecutor().execute(node,
                "-v", MyLocalTonUtils.getTonLogLevel(node.getTonLogLevel()),
                "-t", "2",
                "-C", myGlobalConfig,
                "--db", node.getTonDbDir(),
                "-l", node.getTonLogDir() + MyLocalTonUtils.toUtcNoSpace(System.currentTimeMillis()),
                "--ip", node.getPublicIp() + ":" + node.getPublicPort());

        node.setNodeProcess(validator.getLeft());
        log.info("{} validator-engine started at {}", node.getNodeName(), node.getPublicIp() + ":" + node.getPublicPort());
        return validator;
    }

    /**
     * run full-node very first time
     *
     * @param node               Node
     * @param sharedGlobalConfig String
     */
    public void initFullnode(Node node, String sharedGlobalConfig) throws Exception {
        if (Files.exists(Paths.get(node.getTonDbDir() + "state"))) {
            log.info("Found non-empty state; Skip initialization!");
        } else {
            log.debug("Initializing node validator, create keyrings and config.json...");

            Files.copy(Paths.get(sharedGlobalConfig), Paths.get(node.getNodeGlobalConfigLocation()), StandardCopyOption.REPLACE_EXISTING);

            String s = startValidatorWithoutParams(node, sharedGlobalConfig).getRight().get();

            MyLocalTonUtils.replaceOutPortInConfigJson(node.getTonDbDir(), node.getOutPort());

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
            log.info("Enabling lite-server...");

            Pair<String, String> liteServerKeys = new GenerateRandomId().generateLiteServerKeys(node);
            String liteServers = "\"liteservers\" : [{\"id\":\"" + liteServerKeys.getRight() + "\",\"port\":\"" + node.getLiteServerPort() + "\"}";
            log.debug("liteservers: {} ", liteServers);

            //convert pub key to key
            String liteserverPubkeyBase64 = MyLocalTonUtils.convertPubKeyToBase64(node.getTonDbKeyringDir() + "liteserver.pub");
            int publicIpNum = MyLocalTonUtils.getIntegerIp(node.getPublicIp());

            // replace lite servers array in config.json
            String configJson = FileUtils.readFileToString(new File(node.getTonDbDir() + "config.json"), StandardCharsets.UTF_8);
            String existingLiteservers = "\"liteservers\" : " + MyLocalTonUtils.sbb(configJson, "\"liteservers\" : [");
            String configJsonNew = StringUtils.replace(configJson, existingLiteservers, liteServers + "\n]");
            FileUtils.writeStringToFile(new File(node.getTonDbDir() + "config.json"), configJsonNew, StandardCharsets.UTF_8);
            // done with config.json

            String myGlobalTonConfig = FileUtils.readFileToString(new File(myGlobalConfig), StandardCharsets.UTF_8);
            String myGlobalTonConfigNew;
            String liteServerConfigNew;
            String liteServerConfigBoth;

            if (myGlobalTonConfig.contains("liteservers")) {
                //add new lite-server to the global config
                String existingLiteserver = MyLocalTonUtils.sbb(myGlobalTonConfig, "\"liteservers\":[");
                liteServerConfigNew = "{\"id\":{\"key\":\"" + liteserverPubkeyBase64 + "\", \"@type\":\"pub.ed25519\"}, \"port\": " + node.getLiteServerPort() + ", \"ip\": " + publicIpNum + "}\n]";
                //liteServerConfigBoth = StringUtils.substring(existingLiteserver, 0, -1) + "," + liteServerConfigNew;
                //myGlobalTonConfigNew = StringUtils.replace(myGlobalTonConfig, existingLiteserver, liteServerConfigBoth);
                //FileUtils.writeStringToFile(new File(myGlobalConfig), myGlobalTonConfigNew, StandardCharsets.UTF_8);

                //replace and create new config
                myGlobalTonConfigNew = StringUtils.replace(myGlobalTonConfig, existingLiteserver, "[" + liteServerConfigNew);
                FileUtils.writeStringToFile(new File(node.getNodeLocalConfigLocation()), myGlobalTonConfigNew, StandardCharsets.UTF_8);
            } else {
                // add new lite servers around "validator":{
                liteServerConfigNew = "\"liteservers\":[{\"id\":{\"key\":\"" + liteserverPubkeyBase64 + "\", \"@type\":\"pub.ed25519\"}, \"port\": " + node.getLiteServerPort() + ", \"ip\": " + publicIpNum + "}\n],\n \"validator\": {";
                myGlobalTonConfigNew = StringUtils.replace(myGlobalTonConfig, "\"validator\": {", liteServerConfigNew);
                FileUtils.writeStringToFile(new File(myGlobalConfig), myGlobalTonConfigNew, StandardCharsets.UTF_8);
                FileUtils.writeStringToFile(new File(node.getNodeLocalConfigLocation()), myGlobalTonConfigNew, StandardCharsets.UTF_8);
            }
            log.debug("lite-server enabled");
        }
    }

    public void createZeroState(Node node) throws Exception {

        while (!generateZeroState(node)) ;

        MyLocalTonSettings settings = MyLocalTon.getInstance().getSettings();

        byte[] zerostateRootHashFile = Files.readAllBytes(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.rhash"));
        byte[] zerostateFileHashFile = Files.readAllBytes(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.fhash"));
        log.debug("ROOT_HASH {}", Base64.encodeBase64String(zerostateRootHashFile));
        log.debug("FILE HASH {}", Base64.encodeBase64String(zerostateFileHashFile));

        settings.setZeroStateFileHashBase64(Base64.encodeBase64String(zerostateFileHashFile));
        settings.setZeroStateRootHashBase64(Base64.encodeBase64String(zerostateRootHashFile));

        log.debug(settings.toString());
        MyLocalTonUtils.saveSettingsToGson(settings);

        //mv zerostate.boc ../db/static/$ZEROSTATE_FILEHASH
        Files.move(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.boc"), Paths.get(node.getTonDbStaticDir() + settings.getZeroStateFileHashHex()), StandardCopyOption.REPLACE_EXISTING);

        //mv basestate0.boc ../db/static/$BASESTATE_FILEHASH
        Files.move(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.boc"), Paths.get(node.getTonDbStaticDir() + settings.getBaseStateFileHashHex()), StandardCopyOption.REPLACE_EXISTING);

        String content = Files.readString(Paths.get(Extractor.MY_LOCAL_TON_ROOT_DIR + TEMPLATES + File.separator + "ton-private-testnet.config.json.template"), StandardCharsets.UTF_8);
        content = content.replace("ROOT_HASH", settings.getZeroStateRootHashBase64());
        content = content.replace("FILE_HASH", settings.getZeroStateFileHashBase64());
        Files.writeString(Paths.get(node.getNodeGlobalConfigLocation()), content, StandardCharsets.UTF_8);

        log.info("Zero-state created successfully.");
    }

    private static String sb(String str, String from, String to) {
        return StringUtils.substringBetween(str, from, to);
    }

    private boolean generateZeroState(Node node) throws IOException {

        MyLocalTonSettings settings = MyLocalTon.getInstance().getSettings();

        String createStateResult = new CreateStateExecutor().execute(node, node.getTonBinDir() + "smartcont" + File.separator + "gen-zerostate.fif");

        log.debug("creating zero-state output: {}", createStateResult);

        String mainWalletAddrBoth = sb(createStateResult, "wallet address = ", "(Saving address to file");
        String electorSmcAddrBoth = sb(createStateResult, "elector smart contract address = ", "(Saving address to file");
        String configSmcAddrBoth = sb(createStateResult, "config smart contract address = ", "(Saving address to file");
        String piece = StringUtils.substring(createStateResult, createStateResult.indexOf("(Initial masterchain state saved to file zerostate.boc)"));
        String masterFileHashBoth = sb(piece, "file hash= ", "root hash= ");
        String masterRootHashBoth = sb(piece, "root hash= ", "Basestate0 root hash= ");
        String basestateRootHashBoth = sb(piece, "Basestate0 root hash= ", "Basestate0 file hash= ");
        String basestateFileHashBoth = sb(piece, "Basestate0 file hash= ", "Zerostate root hash= ");
        String zerostateRootHashBoth = sb(piece, "Zerostate root hash= ", "Zerostate file hash= ");
        String zerostateFileHashBoth = StringUtils.substring(piece, piece.indexOf("Zerostate file hash= ") + "Zerostate file hash= ".length());

        String[] mainWalletAddr = mainWalletAddrBoth.split(SPACE);
        settings.setMainWalletAddrFull(mainWalletAddr[0].trim().toUpperCase());
        settings.setMainWalletAddrBase64(mainWalletAddr[1].trim());

        byte[] mainWalletPrvKey = FileUtils.readFileToByteArray(new File(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet.pk"));
        settings.setMainWalletPrvKey(Hex.encodeHexString(mainWalletPrvKey));
        settings.setMainWalletFilenameBaseLocation(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet");

//        String fullAddress = mainWalletAddr[0].trim();

//        WalletAddress genesisWalletAddress = WalletAddress.builder()
//                .fullWalletAddress(fullAddress)
//                .bounceableAddressBase64url(mainWalletAddr[1].trim())
//                .wc(Long.parseLong(fullAddress.substring(0, fullAddress.indexOf(":"))))
//                .subWalletId(-1L)
//                .hexWalletAddress(fullAddress.substring(fullAddress.indexOf(":") + 1))
//                .filenameBaseLocation(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet")
//                .privateKeyLocation(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet.pk")
//                .privateKeyHex(Hex.encodeHexString(mainWalletPrvKey))
//                .build();

        // basically genesis node uses main-wallet and sends requests to elections on its behalf
        //node.setWalletAddress(genesisWalletAddress);//  TODO

        String[] electorSmcAddr = electorSmcAddrBoth.split(SPACE);
        settings.setElectorSmcAddrHex(electorSmcAddr[0].trim().toUpperCase());
        settings.setElectorSmcAddrBase64(electorSmcAddr[1].trim());

        String[] configSmcAddr = configSmcAddrBoth.split(SPACE);
        settings.setConfigSmcAddrHex(configSmcAddr[0].trim().toUpperCase());
        settings.setConfigSmcAddrBase64(configSmcAddr[1].trim());
        byte[] configMasterPrvKey = FileUtils.readFileToByteArray(new File(node.getTonBinDir() + ZEROSTATE + File.separator + "config-master.pk"));
        settings.setConfigSmcPrvKey(Hex.encodeHexString(configMasterPrvKey));

        String[] masterStateFile = masterFileHashBoth.split(DOUBLE_SPACE);
        settings.setMasterStateFileHashHex(masterStateFile[0].trim());
        settings.setMasterStateFileHashBase64(masterStateFile[1].trim());

        String[] masterStateRoot = masterRootHashBoth.split(DOUBLE_SPACE);
        settings.setMasterStateRootHashHex(masterStateRoot[0].trim());
        settings.setMasterStateRootHashBase64(masterStateRoot[1].trim());

        String[] baseStateFile = basestateFileHashBoth.split(DOUBLE_SPACE); //ok
        settings.setBaseStateFileHashHex(baseStateFile[0].trim());
        settings.setBaseStateFileHashBase64(baseStateFile[1].trim());

        String[] baseStateRoot = basestateRootHashBoth.split(DOUBLE_SPACE);
        settings.setBaseStateRootHashHex(baseStateRoot[0].trim());
        settings.setBaseStateRootHashBase64(baseStateRoot[1].trim());

        String[] zeroStateFile = zerostateFileHashBoth.split(DOUBLE_SPACE); //ok
        settings.setZeroStateFileHashHex(zeroStateFile[0].trim());
        settings.setZeroStateFileHashHuman(zeroStateFile[1].trim());

        String[] zeroStateRoot = zerostateRootHashBoth.split(DOUBLE_SPACE);
        settings.setZeroStateRootHashHex(zeroStateRoot[0].trim());
        settings.setZeroStateRootHashHuman(zeroStateRoot[1].trim());

        if ((settings.getMasterStateFileHashHex().length() < 64) ||
                (settings.getMasterStateRootHashHex().length() < 64) ||
                (settings.getBaseStateFileHashHex().length() < 64) ||
                (settings.getBaseStateRootHashHex().length() < 64) ||
                (settings.getZeroStateRootHashHex().length() < 64) ||
                (settings.getZeroStateFileHashHex().length() < 64)
        ) {
            log.debug("gen-zerostate.fif generated wrong hashes, recreating...");
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "config-master.addr"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.fhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.rhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.fhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.rhash"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "elector.addr"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet.addr"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "config-master.pk"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "main-wallet.pk"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "basestate0.boc"));
            Files.delete(Paths.get(node.getTonBinDir() + ZEROSTATE + File.separator + "zerostate.boc"));
            return false;
        }

        return true;
    }

    public void configureGenesisZeroState() throws IOException {

        MyLocalTonSettings settings = MyLocalTon.getInstance().getSettings();

        String genZeroStateFifPath = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "genesis" + File.separator + "bin" + File.separator + "smartcont" + File.separator + "gen-zerostate.fif";
        String genZeroStateFif = FileUtils.readFileToString(new File(genZeroStateFifPath), StandardCharsets.UTF_8);
        String genZeroStateFifNew = "";

        genZeroStateFifNew = StringUtils.replace(genZeroStateFif, "GLOBAL_ID", settings.getBlockchainSettings().getGlobalId().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "INITIAL_BALANCE", settings.getBlockchainSettings().getInitialBalance().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "GAS_PRICE_MC", settings.getBlockchainSettings().getGasPriceMc().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "GAS_PRICE", settings.getBlockchainSettings().getGasPrice().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "CELL_PRICE_MC", settings.getBlockchainSettings().getCellPriceMc().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "CELL_PRICE", settings.getBlockchainSettings().getCellPrice().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_VALIDATORS", settings.getBlockchainSettings().getMaxValidators().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_MAIN_VALIDATORS", settings.getBlockchainSettings().getMaxMainValidators().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MIN_VALIDATORS", settings.getBlockchainSettings().getMinValidators().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MIN_STAKE", settings.getBlockchainSettings().getMinValidatorStake().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_STAKE", settings.getBlockchainSettings().getMaxValidatorStake().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MIN_TOTAL_STAKE", settings.getBlockchainSettings().getMinTotalValidatorStake().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "MAX_FACTOR", settings.getBlockchainSettings().getMaxFactor().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTED_FOR", settings.getBlockchainSettings().getElectedFor().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTION_START_BEFORE", settings.getBlockchainSettings().getElectionStartBefore().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTION_END_BEFORE", settings.getBlockchainSettings().getElectionEndBefore().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ELECTION_STAKE_FROZEN", settings.getBlockchainSettings().getElectionStakesFrozenFor().toString());
        genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "ORIGINAL_VSET_VALID_FOR", settings.getBlockchainSettings().getOriginalValidatorSetValidFor().toString());

        FileUtils.writeStringToFile(new File(genZeroStateFifPath), genZeroStateFifNew, StandardCharsets.UTF_8);
    }

    /**
     * creates files in db directory; prv key - validator, and pub key - validator.pub
     */
    public void generateValidatorKeys(Node node, boolean updateGenZeroStateFif) throws Exception {

        String validatorKeys = new RandomIdExecutor().execute(node, "-m", "keys", "-n", node.getTonDbKeyringDir() + VALIDATOR);

        String[] valHexBase64 = validatorKeys.split(" ");
        String validatorPrvKeyHex = valHexBase64[0].trim();
        String validatorPrvKeyBase64 = valHexBase64[1].trim();

        log.debug("{}, validatorPrvKeyHex {}, validatorPrvKeyBase64 {}", node.getNodeName(), validatorPrvKeyHex, validatorPrvKeyBase64);

        Files.copy(Paths.get(node.getTonDbKeyringDir() + VALIDATOR), Paths.get(node.getTonDbKeyringDir() + validatorPrvKeyHex), StandardCopyOption.REPLACE_EXISTING);
        //convert pub key to key
        byte[] validatorPubKey = Files.readAllBytes(Paths.get(node.getTonDbKeyringDir() + "validator.pub"));
        byte[] removed4bytes = Arrays.copyOfRange(validatorPubKey, 4, validatorPubKey.length);

        node.setValidatorPrvKeyHex(validatorPrvKeyHex);
        node.setValidatorPrvKeyBase64(validatorPrvKeyBase64);
        node.setValidatorPubKeyHex(Hex.encodeHexString(removed4bytes));
        node.setValidatorPubKeyBase64(Base64.encodeBase64String(validatorPubKey));

        MyLocalTonUtils.saveSettingsToGson(MyLocalTon.getInstance().getSettings());

        // create validator-keys-1.pub
        Files.write(Paths.get(node.getValidatorKeyPubLocation()), Hex.decodeHex(node.getValidatorPubKeyHex()), StandardOpenOption.CREATE); // "validator-keys-1.pub"

        if (updateGenZeroStateFif) {
            // replace path to validator-key-1.pub in gen-zerostate.fif
            String genZeroStateFif = FileUtils.readFileToString(new File(node.getGenesisGenZeroStateFifLocation()), StandardCharsets.UTF_8);
            String genZeroStateFifNew = StringUtils.replace(genZeroStateFif, "// \"path_to_" + node.getNodeName() + "_pub_key\"", "\"" + node.getValidatorKeyPubLocation() + "\"");
//            genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "initial_stake_" + node.getNodeName(), node.getDefaultValidatorStake().min(BigDecimal.ONE).multiply(BigDecimal.valueOf(ONE_BLN)).toString());
            genZeroStateFifNew = StringUtils.replace(genZeroStateFifNew, "initial_stake_" + node.getNodeName(), node.getDefaultValidatorStake().min(BigInteger.ONE).toString());
            FileUtils.writeStringToFile(new File(node.getGenesisGenZeroStateFifLocation()), genZeroStateFifNew, StandardCharsets.UTF_8);
        }
    }
}
