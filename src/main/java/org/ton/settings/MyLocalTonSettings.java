package org.ton.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.ton.parameters.ValidationParam;
import org.ton.wallet.WalletVersion;

import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@ToString
@Slf4j
public class MyLocalTonSettings implements Serializable {

    public static final String LOCK_FILE = SystemUtils.getUserHome() + File.separator + "myLocalTon.lock";
    protected static final String CURRENT_DIR = System.getProperty("user.dir");
    protected static final String SETTINGS_JSON = "settings.json";
    protected static final String DB_SETTINGS_CONF = "objectsdb.conf";
    public static final String MY_LOCAL_TON = "myLocalTon";
    public static final String MY_APP_DIR = CURRENT_DIR + File.separator + MY_LOCAL_TON;
    public static final String SETTINGS_FILE = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + SETTINGS_JSON;
    public static final String GENESIS_BIN_DIR = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "genesis" + File.separator + "bin" + File.separator;
    public static final String LOG_FILE = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "myLocalTon.log";
    public static String DB_DIR = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "MyLocalTonDB";
    public static final String DB_SETTINGS_FILE = CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + "MyLocalTonDB" + File.separator + DB_SETTINGS_CONF;

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
        uiSettings = new UiSettings();
        blockchainSettings = new BlockchainSettings();
        logSettings = new LogSettings();

        dbPool = new ConcurrentHashMap<>();
    }

    /*
    constexpr int VERBOSITY_NAME(PLAIN) = -1;
    constexpr int VERBOSITY_NAME(FATAL) = 0;
    constexpr int VERBOSITY_NAME(ERROR) = 1;
    constexpr int VERBOSITY_NAME(WARNING) = 2;
    constexpr int VERBOSITY_NAME(INFO) = 3;
    constexpr int VERBOSITY_NAME(DEBUG) = 4;
    */

    //name- status (active, filled)
    Map<String, String> dbPool;

    GenesisNode genesisNode;
    Node2 node2;
    Node3 node3;
    Node4 node4;
    Node5 node5;
    Node6 node6;
    Node7 node7;

    WalletSettings walletSettings;
    UiSettings uiSettings;
    BlockchainSettings blockchainSettings;
    LogSettings logSettings;

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

    Long stakeHoldRange3End;
    ValidationParam lastValidationParam;
    ValidationParam lastValidationParamEvery3Cycles;
    Double timeLineScale;
    public Map<Long, Long> electionsCounter = new HashMap<>();
    public Map<Long, Boolean> electionsCounterGlobal = new HashMap<>();
    public BigDecimal electionsRipped = BigDecimal.ZERO;
    int cycleMod = 3;
    int cycleModEquals = 1;
    BigDecimal defaultStake = new BigDecimal(10001);
    BigDecimal defaultValidatorBalance = new BigDecimal(50005);
    Boolean veryFirstElections = Boolean.TRUE;

    //options - logs
    @Getter
    @Setter
    public static class LogSettings implements Serializable {
        String myLocalTonLogLevel = "ERROR";
        String tonLogLevel = "INFO";
    }

    //options - account and keys
    @Getter
    @Setter
    public static class WalletSettings implements Serializable {
        Long numberOfPreinstalledWallets = 4L;
        BigDecimal initialAmount = new BigDecimal("778");
        WalletVersion walletVersion = WalletVersion.V3;
        Long defaultWorkChain = 0L;
        long defaultSubWalletId = 1L;
    }

    //options - UI
    @Getter
    @Setter
    public static class UiSettings implements Serializable {
        boolean showTickTockTransactions = false;
        boolean showMainConfigTransactions = true;
        boolean showInOutMessages = true;
        boolean showBodyInMessage = true;
        boolean showShardStateInBlockDump = false;
        boolean enableBlockchainExplorer = false;
        int blockchainExplorerPort = 8000;
    }

    // after you disappear from participant list, you have to wait:
    // разницу между временем окончания выборов и окончанием цикла валидации
    @Getter
    @Setter
    public static class BlockchainSettings implements Serializable {
        Long minValidators = 1L;
        Long maxValidators = 1000L;
        Long maxMainValidators = 100L;
        Long electedFor = 60 * 60L; // 3 min, 60
        Long electionStartBefore = 50 * 60L; // 2 min, 50
        Long electionEndBefore = 20 * 60L;// 1 min, 20
        Long electionStakesFrozenFor = 20 * 60L;// 30 sec, 20
        Long originalValidatorSetValidFor = 50 * 60L; // 2 min, 50

        Long validatorStateTtl = 31536000L; // 1 year
        Long validatorBlockTtl = 31536000L;
        Long validatorArchiveTtl = 31536000L;
        Long validatorKeyProofTtl = 315360000L; // 10 years
        Long validatorSyncBefore = 31536000L;

        Long globalId = -239L;
        Long initialBalance = 4999990000L;
        Long gasPrice = 1000L;
        Long gasPriceMc = 10000L;
        Long cellPrice = 100000L;
        Long cellPriceMc = 1000000L;

        Long minValidatorStake = 10000L;
        Long maxValidatorStake = 10000000L;
        Long minTotalValidatorStake = 10000L;
        BigDecimal maxFactor = new BigDecimal(3);
        Long initialStake = 10000 * 1000000000L; // 10k
        //Long initialStake = 17L; // 10k
    }

    Long currentValidatorSetSince = 0L;
    Long currentValidatorSetUntil = 0L;

    Boolean initiallyElected = false;
    String externalMsgLocation;

    public Node getNode(Node node) {
        return node;
    }
}
