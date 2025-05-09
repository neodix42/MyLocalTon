package org.ton.mylocalton.settings;

import static org.ton.mylocalton.actions.MyLocalTon.ZEROSTATE;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.mylocalton.wallet.WalletAddress;

@Getter
@Setter
@ToString
public class Node4 implements Serializable, Node {

  private static final long serialVersionUID = 1L;
  public Map<Long, Long> electionsCounter = new HashMap<>();
  String nodeName = "node4";
  String publicIp = "127.0.0.1";
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
  // startup settings, individual per node
  Long validatorStateTtl =
      365 * 86400L; // 1 year, state will be gc'd after this time (in seconds) default=3600, 1 hour
  Long validatorBlockTtl =
      365 * 86400L; // 1 year, blocks will be gc'd after this time (in seconds) default=7*86400, 7
  // days
  Long validatorArchiveTtl =
      365 * 86400L; // 1 year, archived blocks will be deleted after this time (in seconds)
  // default=365*86400, 1 year
  Long validatorKeyProofTtl =
      10 * 365 * 86400L; // 10 years, key blocks will be deleted after this time (in seconds)
  // default=365*86400*10, 10 years
  Long validatorSyncBefore =
      3600L; // 1h, initial sync download all blocks for last given seconds default=3600, 1 hour
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
  BigInteger electionsRipped = BigInteger.ZERO;
  BigInteger totalRewardsCollected = BigInteger.ZERO;
  BigInteger lastRewardCollected = BigInteger.ZERO;
  BigInteger totalPureRewardsCollected = BigInteger.ZERO;
  BigInteger lastPureRewardCollected = BigInteger.ZERO;
  BigInteger avgPureRewardCollected = BigInteger.ZERO;

  WalletAddress walletAddress;
  transient Process nodeProcess;
  transient Process dhtServerProcess;
  transient Process blockchainExplorerProcess;
  transient Process tonHttpApiProcess;
  String nodeGlobalConfigLocation = getTonDbDir() + "my-ton-global.config.json";
  String nodeLocalConfigLocation = getTonDbDir() + "my-ton-local.config.json";
  String nodeForkedGlobalConfigLocation = getTonDbDir() + "my-ton-forked.config.json";

  @Override
  public String getValidatorBaseFile() {
    return this.getTonBinDir() + ZEROSTATE + File.separator + "validator-3";
  }
}
