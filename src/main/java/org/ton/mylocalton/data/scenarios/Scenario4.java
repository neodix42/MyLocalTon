package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.types.WalletV2R1Config;
import org.ton.ton4j.smartcontract.wallet.v2.WalletV2R1;
import org.ton.ton4j.utils.Utils;

/** to up V2R1 wallet, upload state-init, send back to faucet 0.08 and random address 0.01 */
@Slf4j
public class Scenario4 implements Scenario {

  AdnlLiteClient adnlLiteClient;

  public Scenario4(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() {
    log.info("STARTED SCENARIO 4");

    WalletV2R1 contract = WalletV2R1.builder().tonProvider(adnlLiteClient).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    Utils.sleep(20);
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
