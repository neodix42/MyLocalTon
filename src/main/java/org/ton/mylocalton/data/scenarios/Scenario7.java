package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.types.WalletV3Config;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.utils.Utils;

/** to up V3R2 wallet, upload state-init with random wallet-id, send back to faucet 0.08 */
@Slf4j
public class Scenario7 implements Scenario {

  AdnlLiteClient adnlLiteClient;

  public Scenario7(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() {
    log.info("STARTED SCENARIO 7");

    long walletId = Math.abs(Utils.getRandomInt());
    WalletV3R1 contract = WalletV3R1.builder().tonProvider(adnlLiteClient).walletId(walletId).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    Utils.sleep(20);
    contract.deploy();
    contract.waitForDeployment();

    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(walletId)
            .seqno(1)
            .destination(dataHighloadFaucetAddress)
            .amount(Utils.toNano(0.08))
            .comment("mlt-scenario7")
            .build();

    contract.send(config);
    Utils.sleep(6);

    BigInteger balance = contract.getBalance();

    if (balance.longValue() > Utils.toNano(0.02).longValue()) {
      log.error("scenario7 failed");
    }

    log.info("FINISHED SCENARIO 7");
  }
}
