package org.ton.mylocalton.settings;

import static java.util.Objects.nonNull;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jutils.jprocesses.JProcesses;
import org.ton.mylocalton.utils.Extractor;
import org.ton.mylocalton.wallet.WalletAddress;

public interface Node {

  String CURRENT_DIR = System.getProperty("user.dir");
  String MY_LOCAL_TON = "myLocalTon";

  default String getTonDbDir() {
    return CURRENT_DIR
        + File.separator
        + MY_LOCAL_TON
        + File.separator
        + this.getNodeName()
        + File.separator
        + "db"
        + File.separator;
  }

  default String getTonBinDir() {
    return CURRENT_DIR
        + File.separator
        + MY_LOCAL_TON
        + File.separator
        + this.getNodeName()
        + File.separator
        + "bin"
        + File.separator;
  }

  default String getTonLogDir() {
    return getTonDbDir() + "log" + File.separator;
  }

  default String getTonDbArchiveDir() {
    return getTonDbDir() + "archive" + File.separator;
  }

  default String getTonDbCatchainsDir() {
    return getTonDbDir() + "catchains" + File.separator;
  }

  default String getTonDbCellDbDir() {
    return getTonDbDir() + "celldb" + File.separator;
  }

  default String getTonDbStateDir() {
    return getTonDbDir() + "state" + File.separator;
  }

  default String getTonDbFilesDir() {
    return getTonDbDir() + "files" + File.separator;
  }

  default String getTonDbKeyringDir() {
    return getTonDbDir() + "keyring" + File.separator;
  }

  default String getTonDbStaticDir() {
    return getTonDbDir() + "static" + File.separator;
  }

  default String getDhtServerDir() {
    return getTonDbDir() + "dht-server" + File.separator;
  }

  default String getDhtServerKeyringDir() {
    return getDhtServerDir() + "keyring" + File.separator;
  }

  default String getTonCertsDir() {
    return getTonBinDir() + "certs" + File.separator;
  }

  default String getTonlibKeystore() {
    return getTonBinDir() + "tonlib-keystore" + File.separator;
  }

  default String getValidatorKeyPubLocation() {
    return getTonBinDir() + "smartcont" + File.separator + "validator-keys-1.pub";
  }

  String getValidatorBaseFile();

  default void extractBinaries() throws IOException {
    new Extractor(this.getNodeName());
  }

  default String getGenesisGenZeroStateFifLocation() {
    return getTonBinDir() + "smartcont" + File.separator + "gen-zerostate.fif";
  }

  default boolean nodeShutdownAndDelete() {

    String nodeName = this.getNodeName();
    int nodePid = 0;
    if (nonNull(this.getNodeProcess())) {
      nodePid = (int) this.getNodeProcess().pid();
    }

    System.out.println("nodeShutdown " + nodeName + ", process " + nodePid);

    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        // kill all lite-client processes of the particular node
        Runtime.getRuntime()
            .exec(
                "WMIC PROCESS WHERE \"COMMANDLINE LIKE '%"
                    + nodeName
                    + "%lite-client%'\" CALL TERMINATE");
        Thread.sleep(1000);
        Runtime.getRuntime()
            .exec(
                "WMIC PROCESS WHERE \"COMMANDLINE LIKE '%"
                    + nodeName
                    + "%lite-client%'\" CALL TERMINATE");

        Runtime.getRuntime().exec("myLocalTon/utils/SendSignalCtrlC64.exe " + nodePid);
        Thread.sleep(2000);
        System.out.println(
            "validator-engine with pid "
                + nodePid
                + " killed "
                + JProcesses.killProcess(nodePid).isSuccess());
        Thread.sleep(2000);
      } else {
        // kill all lite-client processes of the particular node
        Runtime.getRuntime()
            .exec(
                "ps ax | grep "
                    + nodeName
                    + " | grep lite-client | awk '{print $1}' | xargs kill -9");
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

  default boolean nodeShutdown() {

    String nodeName = this.getNodeName();
    int nodePid = 0;
    if (nonNull(this.getNodeProcess())) {
      nodePid = (int) this.getNodeProcess().pid();
    }

    System.out.println("nodeShutdown " + nodeName + ", process " + nodePid);

    try {
      if (SystemUtils.IS_OS_WINDOWS) {
        // kill all lite-client processes of the particular node
        Runtime.getRuntime()
            .exec(
                "WMIC PROCESS WHERE \"COMMANDLINE LIKE '%"
                    + nodeName
                    + "%lite-client%'\" CALL TERMINATE");
        Thread.sleep(1000);
        Runtime.getRuntime()
            .exec(
                "WMIC PROCESS WHERE \"COMMANDLINE LIKE '%"
                    + nodeName
                    + "%lite-client%'\" CALL TERMINATE");

        Runtime.getRuntime().exec("myLocalTon/utils/SendSignalCtrlC64.exe " + nodePid);
        Thread.sleep(2000);
        System.out.println(
            "validator-engine with pid "
                + nodePid
                + " killed "
                + JProcesses.killProcess(nodePid).isSuccess());
        Thread.sleep(2000);
      } else {
        // kill all lite-client processes of the particular node
        Runtime.getRuntime()
            .exec(
                "ps ax | grep "
                    + nodeName
                    + " | grep lite-client | awk '{print $1}' | xargs kill -9");
        Runtime.getRuntime().exec("kill -2 " + nodePid);
      }
      System.out.println("validator-engine with pid " + nodePid + " killed");

      return true;

    } catch (Exception e) {
      System.out.println("cannot shutdown node " + nodeName + ". Error " + e.getMessage());
      return false;
    }
  }

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

  String getFlag();

  void setFlag(String status);

  String getNodeName();

  Process getNodeProcess();

  void setNodeProcess(Process process);

  Process getDhtServerProcess();

  void setDhtServerProcess(Process process);

  void setBlockchainExplorerProcess(Process process);

  void setTonHttpApiProcess(Process process);

  String getValidationPubKeyHex();

  void setValidationPubKeyHex(String validationPubKeyHex);

  String getValidationPubKeyInteger();

  void setValidationPubKeyInteger(String validatorMonitoringPubKeyInteger);

  String getPrevValidationPubKeyHex();

  void setPrevValidationPubKeyHex(String prevValidationPubKeyHex);

  String getPrevValidationPubKeyInteger();

  void setPrevValidationPubKeyInteger(String prevValidatorMonitoringPubKeyInteger);

  void setValidationPubKeyAndAdnlCreated(Boolean pubKeyAndAdnlCreated);

  // outdated?
  String getValidatorPrvKeyHex();

  void setValidatorPrvKeyHex(String validatorPrvKeyHex);

  String getValidatorPrvKeyBase64();

  void setValidatorPrvKeyBase64(String validatorPrvKeyBase64);

  // ---

  String getValidatorPubKeyHex();

  void setValidatorPubKeyHex(String validatorPubKeyHex);

  String getValidatorPubKeyBase64();

  void setValidatorPubKeyBase64(String validatorPubKeyHex);

  WalletAddress getWalletAddress();

  void setWalletAddress(WalletAddress walletAddress);

  BigInteger getTotalRewardsCollected();

  void setTotalRewardsCollected(BigInteger totalRewardsCollected);

  BigInteger getLastRewardCollected();

  void setLastRewardCollected(BigInteger lastRewardCollected);

  BigInteger getTotalPureRewardsCollected();

  void setTotalPureRewardsCollected(BigInteger totalPureRewardsCollected);

  BigInteger getLastPureRewardCollected();

  void setLastPureRewardCollected(BigInteger lastPureRewardCollected);

  BigInteger getAvgPureRewardCollected();

  void setAvgPureRewardCollected(BigInteger avgPureRewardCollected);

  String getValidatorAdnlAddrHex();

  void setValidatorAdnlAddrHex(String validatorAdnlAddrHex);

  String getNodeGlobalConfigLocation();

  void setNodeGlobalConfigLocation(String nodeLocalConfigLocation);

  String getNodeLocalConfigLocation();

  void setNodeLocalConfigLocation(String nodeLocalConfigLocation);

  String getNodeForkedGlobalConfigLocation();

  void setNodeForkedGlobalConfigLocation(String nodeForkedGlobalConfigLocation);

  String getValidationAndlKey();

  void setValidationAndlKey(String prevValidationAndlKey);

  String getPrevValidationAndlKey();

  void setPrevValidationAndlKey(String prevValidationAndlKey);

  String getValidationSigningKey();

  void setValidationSigningKey(String validationSigningKey);

  String getValidationSigningPubKey();

  void setValidationSigningPubKey(String validationSigningPubKey);

  BigInteger getElectionsRipped();

  void setElectionsRipped(BigInteger number);

  Map<Long, Long> getElectionsCounter();

  void setElectionsCounter(Map<Long, Long> elections);

  String getTonLogLevel();

  void setTonLogLevel(String logLevel);

  Long getValidatorSyncBefore();

  Long getValidatorStateTtl();

  Long getValidatorBlockTtl();

  Long getValidatorArchiveTtl();

  Long getValidatorKeyProofTtl();

  BigInteger getInitialValidatorWalletAmount();

  void setInitialValidatorWalletAmount(BigInteger amount);

  BigInteger getDefaultValidatorStake();

  void setDefaultValidatorStake(BigInteger amount);
}
