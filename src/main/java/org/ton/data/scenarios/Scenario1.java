package org.ton.data.scenarios;

import static org.ton.data.Runner.dataHighloadFaucetAddress;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.ton.data.db.DataDB;
import org.ton.java.smartcontract.types.WalletV1R1Config;
import org.ton.java.smartcontract.wallet.v1.WalletV1R1;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

/** to up V1R1 wallet, upload state-init, send back to faucet 0.08 */
@Slf4j
public class Scenario1 implements Scenario {
  Tonlib tonlib;

  public Scenario1(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() {
    log.info("STARTED SCENARIO 1");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    WalletV1R1 contract = WalletV1R1.builder().tonlib(tonlib).wc(0).keyPair(keyPair).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    tonlib.waitForBalanceChange(contract.getAddress(), 60);
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
