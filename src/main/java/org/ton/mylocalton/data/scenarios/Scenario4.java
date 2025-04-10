package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import lombok.extern.slf4j.Slf4j;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.types.WalletV2R1Config;
import org.ton.java.smartcontract.wallet.v2.WalletV2R1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;
import org.ton.mylocalton.data.db.DataDB;

/** to up V2R1 wallet, upload state-init, send back to faucet 0.08 and random address 0.01 */
@Slf4j
public class Scenario4 implements Scenario {

  Tonlib tonlib;

  public Scenario4(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() {
    log.info("STARTED SCENARIO 4");

    WalletV2R1 contract = WalletV2R1.builder().tonlib(tonlib).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    tonlib.waitForBalanceChange(contract.getAddress(), 60);
    contract.deploy();
    contract.waitForDeployment();

    WalletV2R1Config config =
        WalletV2R1Config.builder()
            .bounce(false)
            .seqno(contract.getSeqno())
            .destination1(dataHighloadFaucetAddress)
            .destination2(Address.of(Utils.generateRandomAddress(-1)))
            .amount1(Utils.toNano(0.08))
            .amount2(Utils.toNano(0.01))
            .comment("mlt-scenario4")
            .build();

    contract.send(config);

    log.info("FINISHED SCENARIO 4");
  }
}
