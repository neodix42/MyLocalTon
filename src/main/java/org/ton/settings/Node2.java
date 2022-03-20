package org.ton.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.wallet.WalletAddress;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class Node2 implements Serializable, Node {

    private static final long serialVersionUID = 1L;

    String nodeName = "node2";
    String publicIp = "127.0.0.2";
    Integer consolePort = 4444;
    Integer publicPort = 4445;
    Integer liteServerPort = 4446;
    Integer outPort = 3273;
    Integer dhtPort = 6303;
    Integer dhtForkedPort = 6383;
    Integer dhtOutPort = 3273;
    Integer dhtForkedOutPort = 3283;
    transient String status = "not ready";

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
    Boolean electionRequestSent = Boolean.FALSE;
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
    String nodeLocalConfigLocation = getTonDbDir() + "my-ton-local.config.json"; // used when one wants to connect directly to the local lite-server
    String nodeForkedGlobalConfigLocation = getTonDbDir() + "my-ton-forked.config.json";
}
