package org.ton.mylocalton.data.scenarios;

import static org.ton.mylocalton.data.Runner.dataHighloadFaucetAddress;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.WalletV5Config;
import org.ton.ton4j.smartcontract.wallet.v5.WalletV5;
import org.ton.ton4j.utils.Utils;

/** to up V5R1 wallet, upload state-init, send back to faucet */
@Slf4j
public class Scenario10 implements Scenario {
  AdnlLiteClient adnlLiteClient;

  public Scenario10(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() {
    log.info("STARTED SCENARIO 10");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    long walletId = Math.abs(Utils.getRandomInt());

    WalletV5 contract =
        WalletV5.builder()
            .tonProvider(adnlLiteClient)
            .walletId(walletId)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();
    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    log.info("v5 address {}", nonBounceableAddress);
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(0.1));
    Utils.sleep(20);

    contract.deploy();
    contract.waitForDeployment();

    WalletV5Config config =
        WalletV5Config.builder()
            .seqno(1)
            .walletId(walletId)
            .body(
                contract
                    .createBulkTransfer(
                        Collections.singletonList(
                            Destination.builder()
                                .bounce(false)
                                .address(dataHighloadFaucetAddress.toRaw())
                                .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
                                .amount(Utils.toNano(0.03))
                                .comment("mlt-scenario-10")
                                .build()))
                    .toCell())
            .build();

    contract.send(config);
    Utils.sleep(10);

    BigInteger balance = contract.getBalance();
    if (balance.longValue() > Utils.toNano(0.07).longValue()) {
      log.error("scenario10 failed");
    }
    log.info("FINISHED SCENARIO 10");
  }
}
