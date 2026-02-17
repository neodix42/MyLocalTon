package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.types.WalletV1R2Config;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R2;
import org.ton.ton4j.utils.Utils;

/** to up V1R2 wallet, upload state-init, send back to faucet 0.08 */
@Slf4j
public class Scenario2 implements Scenario {

  AdnlLiteClient adnlLiteClient;

  public Scenario2(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() {
    log.info("STARTED SCENARIO 2");

    WalletV1R2 contract = WalletV1R2.builder().tonProvider(adnlLiteClient).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    Utils.sleep(20);
    contract.deploy();
    contract.waitForDeployment();

    WalletV1R2Config config =
        WalletV1R2Config.builder()
            .seqno(contract.getSeqno())
            .destination(dataHighloadFaucetAddress)
            .amount(Utils.toNano(0.08))
            .comment("mlt-scenario2")
            .build();

    contract.send(config);

    log.info("FINISHED SCENARIO 2");
  }
}
