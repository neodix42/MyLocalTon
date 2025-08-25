package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.types.WalletV4R2Config;
import org.ton.ton4j.smartcontract.wallet.v4.WalletV4R2;
import org.ton.ton4j.utils.Utils;
import org.ton.mylocalton.data.db.DataDB;

/** to up V4R2 wallet, upload state-init with random wallet-id, send back to faucet 0.06 */
@Slf4j
public class Scenario8 implements Scenario {

  AdnlLiteClient adnlLiteClient;

  public Scenario8(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() {
    log.info("STARTED SCENARIO 8");

    long walletId = Math.abs(Utils.getRandomInt());
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();

    WalletV4R2 contract =
        WalletV4R2.builder()
            .adnlLiteClient(adnlLiteClient)
            .keyPair(keyPair)
            .walletId(walletId)
            .build();

    Address walletAddress = contract.getAddress();

    String nonBounceableAddress = walletAddress.toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    adnlLiteClient.waitForBalanceChange(contract.getAddress(), 60);
    SendResponse sendResponse = contract.deploy();
    log.info("deploy8 {}", sendResponse);
    contract.waitForDeployment();

    long walletCurrentSeqno = contract.getSeqno();
    log.info("walletV4 balance: {}", Utils.formatNanoValue(contract.getBalance()));
    log.info("seqno: {}", walletCurrentSeqno);
    log.info("walletId: {}", contract.getWalletId());
    log.info("pubKey: {}", Utils.bytesToHex(contract.getPublicKey()));
    log.info("pluginsList: {}", contract.getPluginsList());

    WalletV4R2Config config =
        WalletV4R2Config.builder()
            .operation(0)
            .walletId(contract.getWalletId())
            .seqno(contract.getSeqno())
            .destination(dataHighloadFaucetAddress)
            .mode(3)
            .amount(Utils.toNano(0.06))
            .comment("mlt-scenario8")
            .build();

    contract.send(config);
    contract.waitForBalanceChangeWithTolerance(60, Utils.toNano(0.05));

    log.info("FINISHED SCENARIO 8");
  }
}
