package org.ton.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.wallet.WalletAddress;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ToString
public class Node3 implements Serializable, Node {

    private static final long serialVersionUID = 1L;

    String nodeName = "node3";
    String publicIp = "127.0.0.3";
    Integer consolePort = 4447;
    Integer publicPort = 4448;
    Integer liteServerPort = 4449;
    Integer outPort = 3274;
    Integer dhtPort = 6304;
    Integer dhtForkedPort = 6384;
    Integer dhtOutPort = 3274;
    Integer dhtForkedOutPort = 3284;
    transient String status = "not ready";
    String flag = "cloning";

    BigDecimal initialValidatorWalletAmount = new BigDecimal("50005");
    BigDecimal defaultValidatorStake = new BigDecimal("10001");
    //startup settings, individual per node
    Long validatorStateTtl = 365 * 86400L; // 1 year, state will be gc'd after this time (in seconds) default=3600, 1 hour
    Long validatorBlockTtl = 365 * 86400L; // 1 year, blocks will be gc'd after this time (in seconds) default=7*86400, 7 days
    Long validatorArchiveTtl = 365 * 86400L; //1 year, archived blocks will be deleted after this time (in seconds) default=365*86400, 1 year
    Long validatorKeyProofTtl = 10 * 365 * 86400L; // 10 years, key blocks will be deleted after this time (in seconds) default=365*86400*10, 10 years
    Long validatorSyncBefore = 300L; //5 min, initial sync download all blocks for last given seconds default=3600, 1 hour

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
    String nodeGlobalConfigLocation = getTonDbDir() + "my-ton-global.config.json";
    String nodeLocalConfigLocation = getTonDbDir() + "my-ton-local.config.json";
    String nodeForkedGlobalConfigLocation = getTonDbDir() + "my-ton-forked.config.json";
}
