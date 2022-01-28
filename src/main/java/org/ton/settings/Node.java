package org.ton.settings;

import org.ton.wallet.WalletAddress;

import java.io.IOException;

public interface Node {

    String getTonDbDir();

    String getTonBinDir();

    String getTonLogDir();

    String getTonDbKeyringDir();

    String getTonDbStaticDir();

    String getDhtServerDir();

    String getDhtServerKeyringDir();

    String getPublicIp();

    Integer getPublicPort();

    Integer getConsolePort();

    Integer getDhtPort();

    Integer getDhtForkedPort();

    Integer getDhtOutPort();

    Integer getDhtForkedOutPort();

    Integer getOutPort();

    Integer getLiteServerPort();

    Long getInitialStake();

    String getNodeName();

    Process getNodeProcess();

    Process getDhtServerProcess();

    void setNodeProcess(Process process);

    void setDhtServerProcess(Process process);

    void setBlockchainExplorerProcess(Process process);

    String getValidatorMonitoringPubKeyHex();

    String getValidatorMonitoringPubKeyInteger();

    void setValidatorMonitoringPubKeyHex(String validatorMonitoringPubKeyHex);

    void setValidatorMonitoringPubKeyInteger(String validatorMonitoringPubKeyInteger);

    String getValidatorIdHex();

    void setWalletAddress(WalletAddress walletAddress);

    WalletAddress getWalletAddress();

    String getValidatorIdBase64();

    String getValidatorIdPubKeyHex();

    void setValidatorIdHex(String validatorIdHex);

    void setValidatorIdBase64(String validatorIdBase64);

    void setValidatorIdPubKeyHex(String validatorIdPubKeyHex);

    void setValidatorAdnlAddrHex(String validatorAdnlAddrHex);

    String getValidatorAdnlAddrHex();

    String getValidatorKeyPubLocation();

    String getNodeGlobalConfigLocation();

    void setNodeGlobalConfigLocation(String nodeGlobalConfigLocation);

    String getNodeForkedGlobalConfigLocation();

    void setNodeForkedGlobalConfigLocation(String nodeForkedGlobalConfigLocation);

    void extractBinaries() throws IOException;

    String getGenesisGenZeroStateFifLocation();
}
