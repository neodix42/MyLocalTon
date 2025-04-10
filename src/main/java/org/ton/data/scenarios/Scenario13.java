package org.ton.data.scenarios;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.ton.data.db.DataDB;
import org.ton.java.smartcontract.highload.HighloadWalletV3;
import org.ton.java.smartcontract.types.*;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

/** deploy HighLoadWallet V3 and transfer to 1000 random recipients */
@Slf4j
public class Scenario13 implements Scenario {
  Tonlib tonlib;

  public Scenario13(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() throws NoSuchAlgorithmException {
    log.info("STARTED SCENARIO 13");
    long walletId = Math.abs(Utils.getRandomInt());

    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    HighloadWalletV3 contract =
        HighloadWalletV3.builder().tonlib(tonlib).keyPair(keyPair).walletId(walletId).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    log.info("non-bounceable address {}", nonBounceableAddress);
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(1));
    tonlib.waitForBalanceChange(contract.getAddress(), 60);

    HighloadV3Config config =
        HighloadV3Config.builder()
            .walletId(walletId)
            .queryId(HighloadQueryId.fromSeqno(0).getQueryId())
            .build();
    contract.deploy(config);

    contract.waitForDeployment();

    config =
        HighloadV3Config.builder()
            .walletId(walletId)
            .queryId(HighloadQueryId.fromSeqno(1).getQueryId())
            .body(
                contract.createBulkTransfer(
                    createDummyDestinations(1000),
                    BigInteger.valueOf(HighloadQueryId.fromSeqno(1).getQueryId())))
            .build();

    contract.send(config);
    log.info("sent 1000 messages");

    log.info("FINISHED SCENARIO 13");
  }

  List<Destination> createDummyDestinations(int count) {
    List<Destination> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      String dstDummyAddress = Utils.generateRandomAddress(0);

      result.add(
          Destination.builder()
              .bounce(false)
              .address(dstDummyAddress)
              .amount(Utils.toNano(0.0001))
              .build());
    }
    return result;
  }
}
