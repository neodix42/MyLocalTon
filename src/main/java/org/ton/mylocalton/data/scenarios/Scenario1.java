package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.types.WalletV1R1Config;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R1;

import org.ton.ton4j.utils.Utils;
import org.ton.mylocalton.data.db.DataDB;

/** top up V1R1 wallet, upload state-init, send back to faucet 0.08 */
@Slf4j
public class Scenario1 implements Scenario {
  AdnlLiteClient adnlLiteClient;

  public Scenario1(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() {
    log.info("STARTED SCENARIO 1");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV1R1 contract = WalletV1R1.builder().adnlLiteClient(adnlLiteClient).wc(0).keyPair(keyPair).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    adnlLiteClient.waitForBalanceChange(contract.getAddress(), 60);
    contract.deploy();
    contract.waitForDeployment(45);

    WalletV1R1Config config =
        WalletV1R1Config.builder()
            .seqno(1)
            .bounce(false)
            .destination(dataHighloadFaucetAddress)
            .amount(Utils.toNano(0.08))
            .comment("mlt-scenario1")
            .build();

    contract.send(config);

    log.info("FINISHED SCENARIO 1");
  }
}
