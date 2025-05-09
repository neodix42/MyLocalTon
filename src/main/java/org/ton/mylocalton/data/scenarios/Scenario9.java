package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import com.iwebpp.crypto.TweetNaclFast;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.types.DeployedPlugin;
import org.ton.java.smartcontract.types.NewPlugin;
import org.ton.java.smartcontract.types.WalletV4R2Config;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.smartcontract.wallet.v4.SubscriptionInfo;
import org.ton.java.smartcontract.wallet.v4.WalletV4R2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.mylocalton.data.utils.MyUtils;

/**
 * creates beneficiary wallet used in plugin, tops up V4R2 wallet, upload state-init with random
 * wallet-id, installs plugin, collects fee, pauses and collects fee again, uninstalls plugin.
 */
@Slf4j
public class Scenario9 implements Scenario {

  Tonlib tonlib;

  public Scenario9(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() {
    log.info("STARTED SCENARIO 9");

    WalletV3R2 beneficiary = (WalletV3R2) new MyUtils().deploy(tonlib, Utils.toNano(0.3));

    long walletId = Math.abs(Utils.getRandomInt());
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 contract =
        WalletV4R2.builder().tonlib(tonlib).keyPair(keyPair).walletId(walletId).build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();

    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(1));
    tonlib.waitForBalanceChange(contract.getAddress(), 60);

    contract.deploy();

    contract.waitForDeployment();

    long walletCurrentSeqno = contract.getSeqno();

    // create and deploy plugin
    SubscriptionInfo subscriptionInfo =
        SubscriptionInfo.builder()
            .beneficiary(beneficiary.getAddress())
            .subscriptionFee(Utils.toNano(0.2))
            .period(60)
            .startTime(0)
            .timeOut(30)
            .lastPaymentTime(0)
            .lastRequestTime(0)
            .failedAttempts(0)
            .subscriptionId(12345)
            .build();

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .seqno(contract.getSeqno())
            .operation(1) // deploy and install plugin
            .walletId(walletId)
            .newPlugin(
                NewPlugin.builder()
                    .secretKey(keyPair.getSecretKey())
                    .seqno(walletCurrentSeqno)
                    .pluginWc(contract.getWc()) // reuse wc of the wallet
                    .amount(
                        Utils.toNano(0.1)) // initial plugin balance, will be taken from wallet-v4
                    .stateInit(contract.createPluginStateInit(subscriptionInfo))
                    .body(contract.createPluginBody())
                    .build())
            .build();

    contract.send(config);

    contract.waitForBalanceChangeWithTolerance(60, Utils.toNano(0.1));

    Utils.sleep(10);

    // create and deploy plugin -- end

    // get plugin list
    List<String> plugins = contract.getPluginsList();
    //    log.info("pluginsList: {}", plugins);

    if (plugins.isEmpty()) {
      log.info("should be one plugin, exit");
      return;
    }

    Address pluginAddress = Address.of(plugins.get(0));

    Cell extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
                contract.getKeyPair(), pluginAddress, null, null)
            .toCell();

    tonlib.sendRawMessage(extMessage.toBase64());

    Utils.sleep(60, "collect fee - first time");

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);

    if (subscriptionInfo.getLastPaymentTime() == 0) {
      log.info("plugin lastPaymentTime should be updated, exit");
      return;
    }

    Utils.sleep(180, "collect fee - second time");

    subscriptionInfo = contract.getSubscriptionData(pluginAddress);
    //    log.info("subscriptionInfo1 {}", subscriptionInfo);

    extMessage =
        MsgUtils.createExternalMessageWithSignedBody(
                contract.getKeyPair(), pluginAddress, null, null)
            .toCell();
    tonlib.sendRawMessage(extMessage.toBase64());

    tonlib.waitForBalanceChangeWithTolerance(
        subscriptionInfo.getBeneficiary(), 60, Utils.toNano(0.1));

    // uninstall/remove plugin from the wallet -- start

    walletCurrentSeqno = contract.getSeqno();

    config =
        WalletV4R2Config.builder()
            .seqno(contract.getSeqno())
            .walletId(config.getWalletId())
            .operation(3) // uninstall plugin
            .deployedPlugin(
                DeployedPlugin.builder()
                    .seqno(walletCurrentSeqno)
                    .amount(Utils.toNano(0.1))
                    .pluginAddress(Address.of(contract.getPluginsList().get(0)))
                    .secretKey(keyPair.getSecretKey())
                    .queryId(0)
                    .build())
            .build();

    contract.uninstallPlugin(config);
    contract.waitForBalanceChangeWithTolerance(60, Utils.toNano(0.05));

    // uninstall plugin -- end

    Utils.sleep(60);
    List<String> list = contract.getPluginsList();
    if (!list.isEmpty()) {
      log.info("plugin list should be empty, exit");
      return;
    }

    config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(contract.getWalletId())
            .seqno(contract.getSeqno())
            .destination(dataHighloadFaucetAddress)
            .amount(Utils.toNano(0.331))
            .build();

    contract.send(config);

    log.info("FINISHED SCENARIO 9");
  }
}
