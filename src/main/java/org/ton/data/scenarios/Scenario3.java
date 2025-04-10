package org.ton.data.scenarios;

import static org.ton.data.Runner.dataHighloadFaucetAddress;

import lombok.extern.slf4j.Slf4j;
import org.ton.data.db.DataDB;
import org.ton.java.smartcontract.types.WalletV1R3Config;
import org.ton.java.smartcontract.wallet.v1.WalletV1R3;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

/** to up V1R3 wallet, upload state-init, send back to faucet 0.08 */
@Slf4j
public class Scenario3 implements Scenario {

  Tonlib tonlib;

  public Scenario3(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() {
    log.info("STARTED SCENARIO 3");

    WalletV1R3 contract = WalletV1R3.builder().tonlib(tonlib).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    tonlib.waitForBalanceChange(contract.getAddress(), 60);
    contract.deploy();
    contract.waitForDeployment();

    WalletV1R3Config config =
        WalletV1R3Config.builder()
            .seqno(contract.getSeqno())
            .destination(dataHighloadFaucetAddress)
            .amount(Utils.toNano(0.08))
            .comment("mlt-scenario3")
            .build();

    contract.send(config);

    log.info("FINISHED SCENARIO 3");
  }
}
