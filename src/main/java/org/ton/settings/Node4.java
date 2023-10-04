package org.ton.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.wallet.WalletAddress;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class Node4 implements Serializable, Node {

    private static final long serialVersionUID = 1L;

    String nodeName = "node4";
    String publicIp = "127.0.0.4";
    Integer consolePort = 4450;
    Integer publicPort = 4451;
    Integer liteServerPort = 4452;
    Integer outPort = 3275;
    Integer dhtPort = 3305;
    Integer dhtForkedPort = 3385;
    Integer dhtOutPort = 3275;
    Integer dhtForkedOutPort = 3285;
    transient String status = "not ready";
    String flag = "cloning";

    BigInteger initialValidatorWalletAmount = new BigInteger("50005000000000");
    BigInteger defaultValidatorStake = new BigInteger("10001000000000");
    //startup settings, individual per node
    Long validatorStateTtl = 365 * 86400L; // 1 year, state will be gc'd after this time (in seconds) default=3600, 1 hour
    Long validatorBlockTtl = 365 * 86400L; // 1 year, blocks will be gc'd after this time (in seconds) default=7*86400, 7 days
    Long validatorArchiveTtl = 365 * 86400L; //1 year, archived blocks will be deleted after this time (in seconds) default=365*86400, 1 year
    Long validatorKeyProofTtl = 10 * 365 * 86400L; // 10 years, key blocks will be deleted after this time (in seconds) default=365*86400*10, 10 years
    Long validatorSyncBefore = 3600L; // 1h, initial sync download all blocks for last given seconds default=3600, 1 hour

    String tonLogLevel = "ERROR";

    String validatorMonitoringPubKeyHex;
    String validatorMonitoringPubKeyInteger;
    String validatorPrvKeyHex;
    String validatorPrvKeyBase64;
    String validatorPubKeyHex;
    String validatorPubKeyBase64;
    String validatorAdnlAddrHex;

    String validationSigningKey;
    String validationSigningPubKey;

    String validationAndlKey;
    String validationPubKeyHex;
    String validationPubKeyInteger;
    String prevValidationAndlKey;
    String prevValidationPubKeyHex;
    String prevValidationPubKeyInteger;

    Boolean validationPubKeyAndAdnlCreated = Boolean.FALSE;
    public Map<Long, Long> electionsCounter = new HashMap<>();
    BigDecimal electionsRipped = BigDecimal.ZERO;
    BigDecimal totalRewardsCollected = BigDecimal.ZERO;
    BigDecimal lastRewardCollected = BigDecimal.ZERO;
    BigDecimal totalPureRewardsCollected = BigDecimal.ZERO;
    BigDecimal lastPureRewardCollected = BigDecimal.ZERO;
    BigDecimal avgPureRewardCollected = BigDecimal.ZERO;

    WalletAddress walletAddress;
    transient Process nodeProcess;
    transient Process dhtServerProcess;
    transient Process blockchainExplorerProcess;
    transient Process tonHttpApiProcess;
    String nodeGlobalConfigLocation = getTonDbDir() + "my-ton-global.config.json";
    String nodeLocalConfigLocation = getTonDbDir() + "my-ton-local.config.json";
    String nodeForkedGlobalConfigLocation = getTonDbDir() + "my-ton-forked.config.json";
}
