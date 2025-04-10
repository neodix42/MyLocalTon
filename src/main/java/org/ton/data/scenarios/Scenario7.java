package org.ton.data.scenarios;

import static org.ton.data.Runner.dataHighloadFaucetAddress;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.data.db.DataDB;
import org.ton.java.smartcontract.types.WalletV3Config;
import org.ton.java.smartcontract.wallet.v3.WalletV3R1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

/** to up V3R2 wallet, upload state-init with random wallet-id, send back to faucet 0.08 */
@Slf4j
public class Scenario7 implements Scenario {

  Tonlib tonlib;

  public Scenario7(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() {
    log.info("STARTED SCENARIO 7");

    long walletId = Math.abs(Utils.getRandomInt());
    WalletV3R1 contract = WalletV3R1.builder().tonlib(tonlib).walletId(walletId).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    tonlib.waitForBalanceChange(contract.getAddress(), 60);
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
    contract.waitForBalanceChangeWithTolerance(30, Utils.toNano(0.05));

    BigInteger balance = contract.getBalance();

    if (balance.longValue() > Utils.toNano(0.02).longValue()) {
      log.error("scenario7 failed");
    }

    log.info("FINISHED SCENARIO 7");
  }
}
