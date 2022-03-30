package org.ton.executors.fift;

import com.sun.javafx.PlatformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.SPACE;

@Slf4j
public class Fift {

    private static final String EOL = "\n";

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

        log.info("{} sending using {}", sendToncoinsParam.getExecutionNode().getNodeName(), walletScript);
        String attachedBoc;

        if (PlatformUtil.isWindows()) {
            attachedBoc = (isNull(sendToncoinsParam.getBocLocation())) ? "" : "-B\"" + sendToncoinsParam.getBocLocation().trim() + "\"";
        } else {
            attachedBoc = (isNull(sendToncoinsParam.getBocLocation())) ? "" : "-B" + sendToncoinsParam.getBocLocation().trim();
        }

        result = new FiftExecutor().execute(sendToncoinsParam.getExecutionNode(),
                "smartcont" + File.separator + walletScript,
                sendToncoinsParam.getFromWallet().getFilenameBaseLocation(),
                sendToncoinsParam.getDestAddr(),
                (walletScript.equals("wallet-v3.fif")) ? String.valueOf(sendToncoinsParam.getFromSubWalletId()) : "",
                String.valueOf(seqno),
                sendToncoinsParam.getAmount().toPlainString(),
                (nonNull(sendToncoinsParam.getForceBounce()) && sendToncoinsParam.getForceBounce().equals(Boolean.TRUE)) ? "-b" : "",
                (isNull(sendToncoinsParam.getComment())) ? "" : "-C " + sendToncoinsParam.getComment().trim(),
                attachedBoc,
                resultBocFileLocation);

        String resultStr = result.getRight().get();
        log.debug(resultStr);

        if (Files.exists(Paths.get(sendToncoinsParam.getExecutionNode().getTonBinDir() + resultBocFileLocation + ".boc"), LinkOption.NOFOLLOW_LINKS)) {
            log.debug("prepared boc file {}", sendToncoinsParam.getExecutionNode().getTonBinDir() + resultBocFileLocation + ".boc");
            return sendToncoinsParam.getExecutionNode().getTonBinDir() + resultBocFileLocation + ".boc";
        } else {
            log.error("Cannot send Toncoins. Stdout: {}", resultStr);
            return null;
        }
    }

    public WalletEntity getWalletByBasename(Node node, String fileBaseName) throws ExecutionException, InterruptedException, IOException {
        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "show-addr.fif", fileBaseName);

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
        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "convert-addr.fif", wcHexAddress);

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
        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "new-wallet.fif", String.valueOf(workchainId), fileNameBaseFullPath);

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
        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "new-wallet-v2.fif", String.valueOf(workchainId), fileNameBaseFullPath);

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
        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "new-wallet-v3.fif", String.valueOf(workchainId), String.valueOf(walletId), fileNameBaseFullPath);

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
        log.debug("CreateValidatorElectionRequest {}", node.getNodeName());

        String fileNameBase = UUID.randomUUID().toString();

        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "validator-elect-req.fif",
                node.getWalletAddress().getBounceableAddressBase64url(),
                String.valueOf(startElectionTime),
                maxFactor.toPlainString(),
                node.getValidationAndlKey(),
                fileNameBase);

        String resultStr = result.getRight().get();

        log.debug(resultStr);

        String[] array = resultStr.split(System.lineSeparator());
        String generatedMessageBase64 = array[array.length - 2].trim();
        String generatedMessageHex = array[array.length - 3].trim();

        log.info("signing request by {}", node.getNodeName());

        //sign hex string and base64, 2nd line from bottom in output
        Pair<Process, Future<String>> signed = new ValidatorEngineConsoleExecutor().execute(node,
                "-k", node.getTonCertsDir() + "client",
                "-p", node.getTonCertsDir() + "server.pub",
                "-v", "0",
                "-a", node.getPublicIp() + ":" + node.getConsolePort().toString(),
                "-rc",
                "sign " + node.getValidationSigningKey() + " " + generatedMessageHex);

        log.debug(signed.getRight().get()); // make debug

        String signature = StringUtils.substring(signed.getRight().get(), signed.getRight().get().indexOf("signature") + 9).trim();
        log.info("signature {}", signature);
        FileUtils.deleteQuietly(new File(node.getTonBinDir() + fileNameBase));

        return signature;
    }

    public void signValidatorElectionRequest(Node node, long startElectionTime, BigDecimal maxFactor, String signatureFromElectionRequest) throws
            ExecutionException, InterruptedException {
        log.debug("signValidatorElectionRequest {}", node.getNodeName());

        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "validator-elect-signed.fif",
                node.getWalletAddress().getBounceableAddressBase64url(),
                String.valueOf(startElectionTime),
                maxFactor.toPlainString(),
                node.getValidationAndlKey(), // getValidatorAdnlAddrHex
                node.getValidationSigningPubKey(), // getValidatorIdBase64
                signatureFromElectionRequest);

        String resultStr = result.getRight().get();
        log.debug(resultStr); // make debug

        resultStr = resultStr.replace("\r\n", SPACE).replace("\n", SPACE);

        String validatorPublicKeyHex = StringUtils.substringBetween(resultStr, "with validator public key ", SPACE).trim();
        BigInteger bigInt = new BigInteger(validatorPublicKeyHex, 16);
        log.info("{} signed adnl {} with validator pubkey (hex){}, (integer){}", node.getNodeName(), node.getValidationAndlKey(), validatorPublicKeyHex, bigInt);

        // used only for monitoring
        node.setPrevValidationPubKeyHex(node.getPrevValidationPubKeyHex());
        node.setValidationPubKeyHex(validatorPublicKeyHex);

        node.setPrevValidationPubKeyInteger(node.getPrevValidationPubKeyInteger());
        node.setValidationPubKeyInteger(bigInt.toString());
    }

    public void createRecoverStake(Node node) throws ExecutionException, InterruptedException {
        log.info("createRecoverStake {}", node.getNodeName());

        Pair<Process, Future<String>> result = new FiftExecutor().execute(node, "smartcont" + File.separator + "recover-stake.fif");

        String resultStr = result.getRight().get();
        log.debug(resultStr);
    }
}
