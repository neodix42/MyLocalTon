package org.ton.executors.fift;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.db.entities.WalletEntity;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.executors.validatorengineconsole.ValidatorEngineConsoleExecutor;
import org.ton.main.App;
import org.ton.parameters.SendToncoinsParam;
import org.ton.settings.Node;
import org.ton.wallet.WalletAddress;
import org.ton.wallet.WalletVersion;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.SPACE;

@Slf4j
public class FiftExecutor {

    private static final String FIFT_EXE = "fift.exe";
    private static final String FIFT = "fift";
    private static final String EOL = "\n";

    public Pair<Process, Future<String>> execute(Node node, String... command) {

        final String fiftBinaryPath = node.getTonBinDir() + (isWindows() ? FIFT_EXE : FIFT);
        String[] withBinaryCommand = {fiftBinaryPath, "-s"};
        String[] commandTrimmed = ArrayUtils.removeAllOccurences(command, "");
        withBinaryCommand = ArrayUtils.addAll(withBinaryCommand, commandTrimmed);

        try {
            log.info("execute: {}", String.join(" ", withBinaryCommand));

            ExecutorService executorService = Executors.newSingleThreadExecutor();

            final ProcessBuilder pb = new ProcessBuilder(withBinaryCommand).redirectErrorStream(true);
            Map<String, String> env = pb.environment();
            env.put("FIFTPATH", node.getTonBinDir() + "lib");

            pb.directory(new File(new File(fiftBinaryPath).getParent()));
            Process p = pb.start();

            Future<String> future = executorService.submit(() -> {
                try {
                    Thread.currentThread().setName("fift-" + node.getNodeName());

                    String resultInput = IOUtils.toString(p.getInputStream(), Charset.defaultCharset());
                    log.debug("{} stopped", "fift-" + node.getNodeName());
                    p.getInputStream().close();
                    p.getErrorStream().close();
                    p.getOutputStream().close();

                    return resultInput;

                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            });

            executorService.shutdown();

            return Pair.of(p, future);

        } catch (final IOException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    public String prepareSendTonCoinsFromNodeWallet(SendToncoinsParam sendToncoinsParam, long seqno) throws ExecutionException, InterruptedException {

        String resultBocFile = UUID.randomUUID().toString();
        String resultBocFileLocation = "wallets" + File.separator + resultBocFile;
        Pair<Process, Future<String>> result;
        String walletScript;
        if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1) || sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.MASTER)) {
            walletScript = "wallet.fif";
        } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V2)) {
            walletScript = "wallet-v2.fif";
        } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V3)) {
            walletScript = "wallet-v3.fif";
        } else {
            walletScript = "wallet.fif";
        }

        log.info("Sending using {}", walletScript);

        result = execute(sendToncoinsParam.getExecutionNode(),
                "smartcont" + File.separator + walletScript,
                sendToncoinsParam.getFromWallet().getFilenameBaseLocation(),
                sendToncoinsParam.getDestAddr(),
                (walletScript.equals("wallet-v3.fif")) ? String.valueOf(sendToncoinsParam.getFromSubWalletId()) : "",
                String.valueOf(seqno),
                sendToncoinsParam.getAmount().toPlainString(),
                (nonNull(sendToncoinsParam.getClearBounce()) && sendToncoinsParam.getClearBounce().equals(Boolean.TRUE)) ? "-n" : "",
                (nonNull(sendToncoinsParam.getForceBounce()) && sendToncoinsParam.getForceBounce().equals(Boolean.TRUE)) ? "-b" : "",
                (isNull(sendToncoinsParam.getBocLocation())) ? "" : "-B " + sendToncoinsParam.getBocLocation(),
                (isNull(sendToncoinsParam.getComment())) ? "" : "-C " + sendToncoinsParam.getComment(),
                resultBocFileLocation);

        String resultStr = result.getRight().get();
        log.debug(resultStr);

        if (Files.exists(Paths.get(sendToncoinsParam.getExecutionNode().getTonBinDir() + resultBocFileLocation + ".boc"), LinkOption.NOFOLLOW_LINKS)) {
            return sendToncoinsParam.getExecutionNode().getTonBinDir() + resultBocFileLocation + ".boc";
        } else {
            //throw new Exception("Cannot send toincoins.");
            log.error("Cannot send Toncoins");
            return null;
        }
    }

    public WalletEntity getWalletByBasename(Node node, String fileBaseName) throws ExecutionException, InterruptedException, IOException {
        Pair<Process, Future<String>> result = execute(node, "smartcont" + File.separator + "show-addr.fif", fileBaseName);

        String resultStr = result.getRight().get();
        log.debug(resultStr);

        String fullAddress = StringUtils.substringBetween(resultStr, "Source wallet address =", EOL).trim();

        String nonBounceableBase64url = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64Url (for init):", EOL).trim();
        String bounceableBase64url = StringUtils.substringBetween(resultStr, "Bounceable address, Base64Url (for later access):", EOL).trim();
        String nonBounceableBase64 = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64 (for init):", EOL).trim();
        String bounceableBase64 = StringUtils.substringBetween(resultStr, "Bounceable address, Base64 (for later access):", EOL).trim();

        String publicKey = StringUtils.substringBetween(resultStr, "Corresponding public key is", EOL).trim();
        String[] publicKeyAddrs = publicKey.split("=");
        byte[] prvKey = FileUtils.readFileToByteArray(new File(fileBaseName + ".pk"));
        String privateKeyLocation = fileBaseName + ".pk";

        WalletAddress walletAddress = WalletAddress.builder()
                .nonBounceableAddressBase64Url(nonBounceableBase64url)
                .bounceableAddressBase64url(bounceableBase64url)
                .nonBounceableAddressBase64(nonBounceableBase64)
                .bounceableAddressBase64(bounceableBase64)
                .fullWalletAddress(fullAddress)
                .wc(Long.parseLong(fullAddress.substring(0, fullAddress.indexOf(":"))))
                .subWalletId(-1L)
                .hexWalletAddress(fullAddress.substring(fullAddress.indexOf(":") + 1))
                .publicKeyHex(publicKeyAddrs[1].trim())
                .publicKeyBase64(publicKeyAddrs[0].trim())
                .privateKeyHex(Hex.encodeHexString(prvKey))
                .privateKeyLocation(privateKeyLocation)
                .filenameBase(FilenameUtils.getName(fileBaseName))
                .filenameBaseLocation(fileBaseName)
                .walletQueryFileBoc(null)
                .walletQueryFileBocLocation(null)
                .build();

        WalletVersion walletVersion;
        if (fileBaseName.contains("main-wallet")) {
            walletVersion = WalletVersion.V1;
        } else {
            walletVersion = null;
        }

        WalletEntity walletEntity = WalletEntity.builder()
                .wc(walletAddress.getWc())
                .hexAddress(walletAddress.getHexWalletAddress())
                .subWalletId(walletAddress.getSubWalletId())
                .walletVersion(walletVersion)
                .wallet(walletAddress)
                .accountState(AccountState.builder().build())
                .createdAt(Instant.now().getEpochSecond())
                .build();

        walletEntity.setPreinstalled(true);

        if (fileBaseName.contains("main-wallet")) {
            walletEntity.setMainWalletInstalled(true);
        } else if (fileBaseName.contains("config-master")) {
            walletEntity.setConfigWalletInstalled(true);
        }

        App.dbPool.insertWallet(walletEntity);
        return walletEntity;
    }

    public WalletAddress convertAddr(Node node, String wcHexAddress) {
        Pair<Process, Future<String>> result = execute(node, "smartcont" + File.separator + "convert-addr.fif", wcHexAddress);

        try {
            String resultStr = result.getRight().get();
            log.debug(resultStr);

            String nonBounceableBase64url = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64Url (for init):", EOL).trim();
            String bounceableBase64url = StringUtils.substringBetween(resultStr, "Bounceable address, Base64Url (for later access):", EOL).trim();
            String nonBounceableBase64 = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64 (for init):", EOL).trim();
            String bounceableBase64 = StringUtils.substringBetween(resultStr, "Bounceable address, Base64 (for later access):", EOL).trim();

            return WalletAddress.builder()
                    .nonBounceableAddressBase64Url(nonBounceableBase64url)
                    .bounceableAddressBase64url(bounceableBase64url)
                    .nonBounceableAddressBase64(nonBounceableBase64)
                    .bounceableAddressBase64(bounceableBase64)
                    .build();
        } catch (Exception e) {
            log.error("convertAddr error {}", e.getMessage());
            return WalletAddress.builder().build();
        }
    }

    /**
     * Generates Ton wallet address
     *
     * @return TonWalletAddress with path to boc file.
     */
    public WalletAddress createWalletV1QueryBoc(Node node, long workchainId) throws Exception {

        String fileNameBase = UUID.randomUUID().toString();
        String fileNameBaseFullPath = node.getTonBinDir() + "wallets" + File.separator + fileNameBase;
        Pair<Process, Future<String>> result = execute(node, "smartcont" + File.separator + "new-wallet.fif", String.valueOf(workchainId), fileNameBaseFullPath);

        String resultStr = result.getRight().get();
        log.debug(resultStr);

        String fullAddress = StringUtils.substringBetween(resultStr, "new wallet address =", EOL).trim().toUpperCase();
        String nonBounceableBase64url = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64Url (for init):", EOL).trim();
        String bounceableBase64url = StringUtils.substringBetween(resultStr, "Bounceable address, Base64Url (for later access):", EOL).trim();
        String nonBounceableBase64 = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64 (for init):", EOL).trim();
        String bounceableBase64 = StringUtils.substringBetween(resultStr, "Bounceable address, Base64 (for later access):", EOL).trim();
        String publicKey = StringUtils.substringBetween(resultStr, "Public key: ", EOL).trim();

        String walletQueryFileBocLocation = fileNameBaseFullPath + "-query.boc";

        if (resultStr.contains("Ed25519 signature is invalid.")) {
            throw new Exception("Ed25519 signature is invalid.");
        }

        File bocFile = new File(walletQueryFileBocLocation);

        ByteBuffer boc = ByteBuffer.wrap(FileUtils.readFileToByteArray(bocFile));
        byte[] prvKey = FileUtils.readFileToByteArray(new File(fileNameBaseFullPath + ".pk"));
        String privateKeyLocation = fileNameBaseFullPath + ".pk";
        // FileUtils.deleteQuietly(bocFile)

        return WalletAddress.builder()
                .nonBounceableAddressBase64Url(nonBounceableBase64url)
                .bounceableAddressBase64url(bounceableBase64url)
                .nonBounceableAddressBase64(nonBounceableBase64)
                .bounceableAddressBase64(bounceableBase64)
                .fullWalletAddress(fullAddress)
                .wc(Long.parseLong(fullAddress.substring(0, fullAddress.indexOf(":"))))
                .subWalletId(-1L)
                .hexWalletAddress(fullAddress.substring(fullAddress.indexOf(":") + 1))
                .publicKeyHex(publicKey)
                .privateKeyHex(Hex.encodeHexString(prvKey))
                .privateKeyLocation(privateKeyLocation)
                .filenameBase(fileNameBase)
                .filenameBaseLocation(fileNameBaseFullPath)
                .walletQueryFileBoc(boc)
                .walletQueryFileBocLocation(walletQueryFileBocLocation)
                .build();
    }

    public WalletAddress createWalletV2QueryBoc(Node node, long workchainId) throws Exception {

        String fileNameBase = UUID.randomUUID().toString();
        String fileNameBaseFullPath = node.getTonBinDir() + "wallets" + File.separator + fileNameBase;
        Pair<Process, Future<String>> result = execute(node, "smartcont" + File.separator + "new-wallet-v2.fif", String.valueOf(workchainId), fileNameBaseFullPath);

        String resultStr = result.getRight().get();
        log.debug(resultStr);

        String fullAddress = StringUtils.substringBetween(resultStr, "new wallet address =", EOL).trim().toUpperCase();
        String nonBounceableBase64url = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64Url (for init):", EOL).trim();
        String bounceableBase64url = StringUtils.substringBetween(resultStr, "Bounceable address, Base64Url (for later access):", EOL).trim();
        String nonBounceableBase64 = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64 (for init):", EOL).trim();
        String bounceableBase64 = StringUtils.substringBetween(resultStr, "Bounceable address, Base64 (for later access):", EOL).trim();
        //String publicKey = StringUtils.substringBetween(resultStr, "Public key: ", EOL).trim();

        String walletQueryFileBocLocation = fileNameBaseFullPath + "-query.boc";

        if (resultStr.contains("Ed25519 signature is invalid.")) {
            throw new Exception("Ed25519 signature is invalid.");
        }

        File bocFile = new File(walletQueryFileBocLocation);

        ByteBuffer boc = ByteBuffer.wrap(FileUtils.readFileToByteArray(bocFile));
        byte[] prvKey = FileUtils.readFileToByteArray(new File(fileNameBaseFullPath + ".pk"));
        String privateKeyLocation = fileNameBaseFullPath + ".pk";
        // FileUtils.deleteQuietly(bocFile)

        return WalletAddress.builder()
                .nonBounceableAddressBase64Url(nonBounceableBase64url)
                .bounceableAddressBase64url(bounceableBase64url)
                .nonBounceableAddressBase64(nonBounceableBase64)
                .bounceableAddressBase64(bounceableBase64)
                .fullWalletAddress(fullAddress)
                .wc(Long.parseLong(fullAddress.substring(0, fullAddress.indexOf(":"))))
                .subWalletId(-1L)
                .hexWalletAddress(fullAddress.substring(fullAddress.indexOf(":") + 1))
                // .publicKeyHex(publicKey)
                .privateKeyHex(Hex.encodeHexString(prvKey))
                .privateKeyLocation(privateKeyLocation)
                .filenameBase(fileNameBase)
                .filenameBaseLocation(fileNameBaseFullPath)
                .walletQueryFileBoc(boc)
                .walletQueryFileBocLocation(walletQueryFileBocLocation)
                .build();
    }

    public WalletAddress createWalletV3QueryBoc(Node node, long workchainId, long walletId) throws Exception {

        String fileNameBase = UUID.randomUUID().toString();
        String fileNameBaseFullPath = node.getTonBinDir() + "wallets" + File.separator + fileNameBase;
        Pair<Process, Future<String>> result = execute(node, "smartcont" + File.separator + "new-wallet-v3.fif", String.valueOf(workchainId), String.valueOf(walletId), fileNameBaseFullPath);

        String resultStr = result.getRight().get();
        log.debug(resultStr);

        String fullAddress = StringUtils.substringBetween(resultStr, "new wallet address =", EOL).trim().toUpperCase();
        String nonBounceableBase64url = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64Url (for init):", EOL).trim();
        String bounceableBase64url = StringUtils.substringBetween(resultStr, "Bounceable address, Base64Url (for later access):", EOL).trim();
        String nonBounceableBase64 = StringUtils.substringBetween(resultStr, "Non-bounceable address, Base64 (for init):", EOL).trim();
        String bounceableBase64 = StringUtils.substringBetween(resultStr, "Bounceable address, Base64 (for later access):", EOL).trim();

        String walletQueryFileBocLocation = fileNameBaseFullPath + "-query.boc";

        if (resultStr.contains("Ed25519 signature is invalid.")) {
            throw new Exception("Ed25519 signature is invalid.");
        }

        File bocFile = new File(walletQueryFileBocLocation);

        ByteBuffer boc = ByteBuffer.wrap(FileUtils.readFileToByteArray(bocFile));
        byte[] prvKey = FileUtils.readFileToByteArray(new File(fileNameBaseFullPath + ".pk"));
        String privateKeyLocation = fileNameBaseFullPath + ".pk";
        // FileUtils.deleteQuietly(bocFile)

        return WalletAddress.builder()
                .nonBounceableAddressBase64Url(nonBounceableBase64url)
                .bounceableAddressBase64url(bounceableBase64url)
                .nonBounceableAddressBase64(nonBounceableBase64)
                .bounceableAddressBase64(bounceableBase64)
                .fullWalletAddress(fullAddress)
                .wc(Long.parseLong(fullAddress.substring(0, fullAddress.indexOf(":"))))
                .subWalletId(walletId)
                .hexWalletAddress(fullAddress.substring(fullAddress.indexOf(":") + 1))
                .privateKeyHex(Hex.encodeHexString(prvKey))
                .privateKeyLocation(privateKeyLocation)
                .filenameBase(fileNameBase)
                .filenameBaseLocation(fileNameBaseFullPath)
                .walletQueryFileBoc(boc)
                .walletQueryFileBocLocation(walletQueryFileBocLocation)
                .build();
    }

    /**
     * Creating a request to participate in validator elections at time startElectionTime
     * from smart contract walletAddress.
     */
    public String createValidatorElectionRequest(Node node, long startElectionTime, BigDecimal maxFactor) throws ExecutionException, InterruptedException {
        log.info("CreateValidatorElectionRequest {}", node.getNodeName());
        String fileNameBase = UUID.randomUUID().toString();
        Pair<Process, Future<String>> result = execute(node, "smartcont" + File.separator + "validator-elect-req.fif",
                node.getWalletAddress().getBounceableAddressBase64url(),
                String.valueOf(startElectionTime),
                maxFactor.toPlainString(),
                node.getValidatorAdnlAddrHex(),
                fileNameBase);

        String resultStr = result.getRight().get();
        log.debug(resultStr);
        String[] array = resultStr.split(System.lineSeparator());
        String generatedMessageBase64 = array[array.length - 2].trim();
        String generatedMessageHex = array[array.length - 3].trim();
        //sign hex string and base64, 2 line from bottom in output
        Pair<String, Process> signed = new ValidatorEngineConsoleExecutor().execute(node, "-k", node.getTonDbDir() + "client", "-p", node.getTonDbDir() + "server.pub", "-v", "0", "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(), "-rc", "sign " + node.getValidatorIdHex() + " " + generatedMessageHex);
        log.debug(signed.getLeft());
        String signature = StringUtils.substring(signed.getLeft(), signed.getLeft().indexOf("signature") + 9).trim();

        return signature;
    }

    public Pair<String, String> signValidatorElectionRequest(Node node, long startElectionTime, BigDecimal maxFactor, String signatureFromElectionRequest) throws
            ExecutionException, InterruptedException {
        log.info("signValidatorElectionRequest {}", node.getNodeName());
        Pair<Process, Future<String>> result = execute(node, "smartcont" + File.separator + "validator-elect-signed.fif",
                node.getWalletAddress().getBounceableAddressBase64url(),
                String.valueOf(startElectionTime),
                maxFactor.toPlainString(),
                node.getValidatorAdnlAddrHex(),
                node.getValidatorIdBase64(),
                signatureFromElectionRequest);

        String resultStr = result.getRight().get();
        log.debug(resultStr);

        resultStr = resultStr.replace("\r\n", SPACE);
        resultStr = resultStr.replace("\n", SPACE);
        String validatorPublicKeyHex = StringUtils.substringBetween(resultStr, "with validator public key ", SPACE).trim();
        BigInteger bigInt = new BigInteger(validatorPublicKeyHex, 16);

        //fift -s wallet.fif my_wallet_id -1:3333333333333333333333333333333333333333333333333333333333333333 1 100001. -B validator-query.boc
        return Pair.of(validatorPublicKeyHex, bigInt.toString());
    }
}
