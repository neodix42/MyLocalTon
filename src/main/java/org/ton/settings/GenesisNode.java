package org.ton.settings;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.utils.Extractor;
import org.ton.wallet.WalletAddress;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;

import static org.ton.settings.MyLocalTonSettings.CURRENT_DIR;

@Getter
@Setter
@ToString
public class GenesisNode implements Serializable, Node {

    private static final long serialVersionUID = 1L;
    public static final String MY_LOCAL_TON = "myLocalTon";

    String nodeName = "genesis";
    String publicIp = "127.0.0.1";
    Integer consolePort = 4441;
    Integer publicPort = 4442;
    Integer liteServerPort = 4443;
    Integer outPort = 3278;
    Integer dhtPort = 6302;
    Integer dhtForkedPort = 6382;
    Integer dhtOutPort = 3278;
    Integer dhtForkedOutPort = 3288;
    transient String status = "not ready";

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
    Boolean validationPubKeyAndAdnlCreated = Boolean.FALSE;
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

    @Override
    public String getTonDbDir() {
        return CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + nodeName + File.separator + "db" + File.separator;
    }

    @Override
    public String getTonBinDir() {
        return CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + nodeName + File.separator + "bin" + File.separator;
    }

    @Override
    public String getTonLogDir() {
        return CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + nodeName + File.separator + "db" + File.separator + "log" + File.separator;
    }

    @Override
    public String getTonDbKeyringDir() {
        return CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + nodeName + File.separator + "db" + File.separator + "keyring" + File.separator;
    }

    @Override
    public String getTonDbStaticDir() {
        return CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + nodeName + File.separator + "db" + File.separator + "static" + File.separator;
    }

    @Override
    public String getDhtServerDir() {
        return CURRENT_DIR + File.separator + MY_LOCAL_TON + File.separator + nodeName + File.separator + "db" + File.separator + "dht-server" + File.separator;
    }

    @Override
    public String getDhtServerKeyringDir() {
        return getDhtServerDir() + "keyring" + File.separator;
    }

    @Override
    public String getTonCertsDir() {
        return getTonBinDir() + File.separator + "certs" + File.separator;
    }

    @Override
    public String getValidatorKeyPubLocation() {
        return getTonBinDir() + "smartcont" + File.separator + "validator-keys-1.pub";
    }

//    @Override
//    public String getNodeGlobalConfigLocation() {
//        return getTonDbDir() + "my-ton-global.config.json";
//    }
//
//    @Override
//    public void setNodeGlobalConfigLocation(String nodeGlobalConfigLocation) {
//        this.nodeGlobalConfigLocation = nodeGlobalConfigLocation;
//    }
//
//    @Override
//    public String getNodeForkedGlobalConfigLocation() {
//        return getTonDbDir() + "my-ton-forked.config.json";
//    }
//
//    @Override
//    public void setNodeForkedGlobalConfigLocation(String nodeForkedGlobalConfigLocation) {
//        this.nodeForkedGlobalConfigLocation = nodeForkedGlobalConfigLocation;
//    }

    @Override
    public void extractBinaries() throws IOException {
        new Extractor(nodeName);
    }

    @Override
    public String getGenesisGenZeroStateFifLocation() {
        return getTonBinDir() + "smartcont" + File.separator + "gen-zerostate.fif";
    }
}
