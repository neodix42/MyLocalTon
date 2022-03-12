package org.ton.settings;

import org.apache.commons.io.FileUtils;
import org.ton.wallet.WalletAddress;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.nonNull;

public interface Node {

    String getTonDbDir();

    String getTonBinDir();

    String getTonLogDir();

    String getTonDbKeyringDir();

    String getTonDbStaticDir();

    String getDhtServerDir();

    String getDhtServerKeyringDir();

    String getTonCertsDir();

    String getPublicIp();

    Integer getPublicPort();

    Integer getConsolePort();

    Integer getDhtPort();

    Integer getDhtForkedPort();

    Integer getDhtOutPort();

    Integer getDhtForkedOutPort();

    Integer getOutPort();

    Integer getLiteServerPort();

    String getStatus();

    void setStatus(String status);

    String getNodeName();

    Process getNodeProcess();

    Process getDhtServerProcess();

    void setNodeProcess(Process process);

    void setDhtServerProcess(Process process);

    void setBlockchainExplorerProcess(Process process);

    String getValidationPubKeyHex();

    String getValidationPubKeyInteger();

    void setValidationPubKeyHex(String validationPubKeyHex);

    void setValidationPubKeyInteger(String validatorMonitoringPubKeyInteger);

    void setValidationPubKeyAndAdnlCreated(Boolean pubKeyAndAdnlCreated);

    Boolean getValidationPubKeyAndAdnlCreated();

    String getValidatorPrvKeyHex();

    String getValidatorPrvKeyBase64();

    String getValidatorPubKeyHex();

    String getValidatorPubKeyBase64();

    void setWalletAddress(WalletAddress walletAddress);

    WalletAddress getWalletAddress();

    BigDecimal getTotalRewardsCollected();

    void setTotalRewardsCollected(BigDecimal totalRewardsCollected);

    BigDecimal getLastRewardCollected();

    void setLastRewardCollected(BigDecimal lastRewardCollected);

    BigDecimal getTotalPureRewardsCollected();

    void setTotalPureRewardsCollected(BigDecimal totalPureRewardsCollected);

    BigDecimal getLastPureRewardCollected();

    void setLastPureRewardCollected(BigDecimal lastPureRewardCollected);

    BigDecimal getAvgPureRewardCollected();

    void setAvgPureRewardCollected(BigDecimal avgPureRewardCollected);

    void setValidatorPrvKeyHex(String validatorPrvKeyHex);

    void setValidatorPrvKeyBase64(String validatorPrvKeyBase64);

    void setValidatorPubKeyHex(String validatorPubKeyHex);

    void setValidatorPubKeyBase64(String validatorPubKeyHex);

    void setValidatorAdnlAddrHex(String validatorAdnlAddrHex);

    String getValidatorAdnlAddrHex();

    String getValidatorKeyPubLocation();

    String getNodeGlobalConfigLocation();

    void setNodeGlobalConfigLocation(String nodeLocalConfigLocation);

    String getNodeLocalConfigLocation();

    void setNodeLocalConfigLocation(String nodeLocalConfigLocation);

    String getNodeForkedGlobalConfigLocation();

    void setNodeForkedGlobalConfigLocation(String nodeForkedGlobalConfigLocation);

    void extractBinaries() throws IOException;

    String getGenesisGenZeroStateFifLocation();

    void setValidationSigningKey(String validationSigningKey);

    void setValidationSigningPubKey(String validationSigningPubKey);

    void setValidationAndlKey(String validationAndlKey);

    String getValidationSigningKey();

    String getValidationSigningPubKey();

    String getValidationAndlKey();

    default boolean nodeShutdownAndDelete() {

        String nodeName = this.getNodeName();
        long nodePid = 0;
        if (nonNull(this.getNodeProcess())) {
            nodePid = this.getNodeProcess().pid();
        }

        System.out.println("nodeShutdown " + nodeName + ", process " + nodePid);

        try {
            if (isWindows()) {
                Runtime.getRuntime().exec("myLocalTon/genesis/bin/SendSignalCtrlC64.exe " + nodePid);
            } else {
                Runtime.getRuntime().exec("kill -2 " + nodePid);
            }
            System.out.println("validator-engine with pid " + nodePid + " killed");

            FileUtils.deleteQuietly(new File(MyLocalTonSettings.MY_APP_DIR + File.separator + nodeName));

            return true;

        } catch (Exception e) {
            System.out.println("cannot shutdown node " + nodeName + ". Error " + e.getMessage());
            return false;
        }
    }
}
