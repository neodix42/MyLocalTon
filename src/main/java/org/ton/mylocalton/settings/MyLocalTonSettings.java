package org.ton.mylocalton.settings;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.ton.ton4j.smartcontract.types.WalletVersion;
import org.ton.ton4j.utils.Utils;
import org.ton.mylocalton.parameters.ValidationParam;

@Getter
@Setter
@ToString
@Slf4j
public class MyLocalTonSettings implements Serializable {

  public static final String LOCK_FILE =
      SystemUtils.getUserHome() + File.separator + "myLocalTon.lock";
  public static final String MY_LOCAL_TON = "myLocalTon";
  protected static final String CURRENT_DIR = System.getProperty("user.dir");
  public static final String MY_APP_DIR = CURRENT_DIR + File.separator + MY_LOCAL_TON;
  public static final String GENESIS_BIN_DIR =
      CURRENT_DIR
          + File.separator
          + MY_LOCAL_TON
          + File.separator
          + "genesis"
          + File.separator
          + "bin"
          + File.separator;
  public static final String LOG_FILE =
      CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "myLocalTon.log";
  protected static final String SETTINGS_JSON = "settings.json";
  public static final String SETTINGS_FILE =
      CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + SETTINGS_JSON;
  protected static final String DB_SETTINGS_CONF = "objectsdb.conf";
  public static final String DB_SETTINGS_FILE =
      CURRENT_DIR
          + File.separator
          + MY_LOCAL_TON
          + File.separator
          + "MyLocalTonDB"
          + File.separator
          + DB_SETTINGS_CONF;
  public static String DB_DIR =
      CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "MyLocalTonDB";
  public Map<Long, ValidationParam> elections = new ConcurrentSkipListMap<>();

  /*
  constexpr int VERBOSITY_NAME(PLAIN) = -1;
  constexpr int VERBOSITY_NAME(FATAL) = 0;
  constexpr int VERBOSITY_NAME(ERROR) = 1;
  constexpr int VERBOSITY_NAME(WARNING) = 2;
  constexpr int VERBOSITY_NAME(INFO) = 3;
  constexpr int VERBOSITY_NAME(DEBUG) = 4;
  */
  // name- status (active, filled)
  Map<String, String> dbPool;

  GenesisNode genesisNode;
  Node2 node2;
  Node3 node3;
  Node4 node4;
  Node5 node5;
  Node6 node6;
  Node7 node7;
  Queue<String> activeNodes;

  WalletSettings walletSettings;
  FaucetWalletSettings faucetWalletSettings;
  FaucetHighloadWalletSettings faucetHighloadWalletSettings;
  FaucetDataWalletSettings faucetDataWalletSettings;
  UiSettings uiSettings;
  BlockchainSettings blockchainSettings;

  String mainWalletAddrBase64;
  String mainWalletAddrFull;
  String mainWalletPrvKey;
  String mainWalletFilenameBaseLocation;

  String electorSmcAddrBase64;
  String electorSmcAddrHex;

  String configSmcAddrBase64;
  String configSmcAddrHex;
  String configSmcPrvKey;

  String masterStateFileHashHex;
  String masterStateFileHashBase64;

  String masterStateRootHashHex;
  String masterStateRootHashBase64;

  String baseStateRootHashHex;
  String baseStateRootHashBase64;

  String baseStateFileHashHex;
  String baseStateFileHashBase64;

  String zeroStateRootHashHex;
  String zeroStateRootHashHuman;
  String zeroStateRootHashBase64;

  String zeroStateFileHashHex;
  String zeroStateFileHashHuman;
  String zeroStateFileHashBase64;

  Long stakeHoldRange2End;
  Long stakeHoldRange3End;

  ValidationParam lastValidationParam;
  ValidationParam lastValidationParamEvery3Cycles;
  Double timeLineScale;
  Boolean veryFirstElections = Boolean.TRUE;
  String customTonBinariesPath;
  Long currentValidatorSetSince = 0L;
  Long currentValidatorSetUntil = 0L;
  Boolean initiallyElected = false;
  String externalMsgLocation;

  public MyLocalTonSettings() {
    super();
    genesisNode = new GenesisNode();
    node2 = new Node2();
    node3 = new Node3();
    node4 = new Node4();
    node5 = new Node5();
    node6 = new Node6();
    node7 = new Node7();

    walletSettings = new WalletSettings();
    faucetWalletSettings = new FaucetWalletSettings();
    faucetHighloadWalletSettings = new FaucetHighloadWalletSettings();
    faucetDataWalletSettings = new FaucetDataWalletSettings();
    uiSettings = new UiSettings();
    blockchainSettings = new BlockchainSettings();

    dbPool = new ConcurrentHashMap<>();
    activeNodes = new ConcurrentLinkedQueue<>();
  }

  public Node getNodeByName(String nodeName) {
    switch (nodeName) {
      case "genesis":
        return genesisNode;
      case "node2":
        return node2;
      case "node3":
        return node3;
      case "node4":
        return node4;
      case "node5":
        return node5;
      case "node6":
        return node6;
      case "node7":
        return node7;
      default:
        return genesisNode;
    }
  }

  // options - account and keys
  @Getter
  @Setter
  public static class WalletSettings implements Serializable {
    BigInteger initialAmount = new BigInteger("778000000000");
    //        WalletVersion walletVersion = WalletVersion.V3R2;
    Long defaultWorkChain = 0L;
    long defaultSubWalletId = 42L;
  }

  @Getter
  @Setter
  public static class FaucetWalletSettings implements Serializable {
    BigInteger initialBalance = Utils.toNano(1_000_001);
    String privateKey = "a51e8fb6f0fae3834bf430f5012589d319e7b3b3303ceb82c816b762fccf2d05";
    String publicKey = "-";
    String walletRawAddress = "-1:22f53b7d9aba2cef44755f7078b01614cd4dde2388a1729c2c386cf8f9898afe";
    String mnemonic =
        "viable model canvas decade neck soap turtle asthma bench crouch bicycle grief history envelope valid intact invest like offer urban adjust popular draft coral";
    WalletVersion walletVersion = WalletVersion.V3R2;
    Long workChain = 0L;
    long subWalletId = 42L;
    boolean created = false;
  }

  @Getter
  @Setter
  public static class FaucetHighloadWalletSettings implements Serializable {
    BigInteger initialBalance = Utils.toNano(1_000_001);
    String privateKey = "e1480435871753a968ef08edabb24f5532dab4cd904dbdc683b8576fb45fa697";
    String publicKey = "-";
    String walletRawAddress = "-1:5ee77ced0b7ae6ef88ab3f4350d8872c64667ffbe76073455215d3cdfab3294b";
    String mnemonic =
        "twenty unfair stay entry during please water april fabric morning length lumber style tomorrow melody similar forum width ride render void rather custom coin";
    WalletVersion walletVersion = WalletVersion.highload;
    Long workChain = -1L;
    long subWalletId = 42L;
    boolean created = false;
  }

  @Getter
  @Setter
  public static class FaucetDataWalletSettings implements Serializable {
    BigInteger initialBalance = Utils.toNano(1_000_001);
    String privateKey = "f2480435871753a968ef08edabb24f5532dab4cd904dbdc683b8576fb45fa697";
    String publicKey = "-";
    String walletRawAddress = "-1:10df89757ee2bd09779d876a29b3e8ec4e706f902c9704eea5434d0a165e7ccd";
    String mnemonic = "-";
    WalletVersion walletVersion = WalletVersion.highload;
    Long workChain = -1L;
    long subWalletId = 42L;
    boolean created = false;
  }

  // options - UI
  @Getter
  @Setter
  public static class UiSettings implements Serializable {
    boolean showTickTockTransactions = false;
    boolean showMainConfigTransactions = true;
    boolean showInOutMessages = true;
    boolean showBodyInMessage = true;
    boolean showShardStateInBlockDump = false;
    boolean enableBlockchainExplorer = false;
    boolean enableDataGenerator = false;
    boolean enableTonHttpApi = false;
    boolean enableDebugMode = false;
    boolean enableNoGuiMode = false;
    int numberOfValidators = 0;
    int blockchainExplorerPort = 8001;
    int simpleHttpServerPort = 8000;
    int tonHttpApiPort = 8081;
  }

  @Getter
  @Setter
  public static class BlockchainSettings implements Serializable {
    Long minValidators = 1L;
    Long maxValidators = 1000L;
    Long maxMainValidators = 100L;
    Long electedFor = 30 * 60L; // 3 min, 60 min
    Long electionStartBefore = 25 * 60L; // 2 min, 50 min
    Long electionEndBefore = 10 * 60L; // 1 min, 10 min
    Long electionStakesFrozenFor = 5 * 60L; // 30 sec, 20 min

    //        Long electedFor = 3 * 60L; // 3 min
    //        Long electionStartBefore = 2 * 60L; // 2 min
    //        Long electionEndBefore = 60L;// 1 min
    //        Long electionStakesFrozenFor = 30L;// 30 sec
    Long originalValidatorSetValidFor = electionStartBefore;

    Long globalId = -239L;
    Long initialBalance = 4999990000L;
    Long gasPrice = 26214400L;
    Long gasPriceMc = 655360000L;
    Long cellPrice = 2621440000L;
    Long cellPriceMc = 65536000000L;

    Long minValidatorStake = 10000L;
    Long maxValidatorStake = 10000000L;
    Long minTotalValidatorStake = 10000L;
    BigDecimal maxFactor = new BigDecimal(3);
  }
}
