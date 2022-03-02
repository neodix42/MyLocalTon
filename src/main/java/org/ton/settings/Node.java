package org.ton.settings;

import org.ton.wallet.WalletAddress;

import java.io.IOException;
import java.math.BigDecimal;

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

    void setNodeGlobalConfigLocation(String nodeGlobalConfigLocation);

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

}
