package org.ton.mylocalton.data.scenarios;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.WalletV5Config;
import org.ton.ton4j.smartcontract.wallet.v5.WalletV5;
import org.ton.ton4j.utils.Utils;

/** to up V5R1 wallet, upload state-init, transfer to 255 random recipients */
@Slf4j
public class Scenario11 implements Scenario {
  AdnlLiteClient adnlLiteClient;

  public Scenario11(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() throws NoSuchAlgorithmException {
    log.info("STARTED SCENARIO 11");
    TweetNaclFast.Signature.KeyPair keyPair = Utils.generateSignatureKeyPair();
    long walletId = Math.abs(Utils.getRandomInt());

    WalletV5 contract =
        WalletV5.builder()
            .adnlLiteClient(adnlLiteClient)
            .walletId(walletId)
            .keyPair(keyPair)
            .isSigAuthAllowed(true)
            .build();
    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    log.info("v5 address {}", nonBounceableAddress);
    DataDB.addDataRequest(nonBounceableAddress, Utils.toNano(1.5));
    adnlLiteClient.waitForBalanceChange(contract.getAddress(), 60);

    contract.deploy();

    contract.waitForDeployment();

    WalletV5Config config =
        WalletV5Config.builder()
            .seqno(1)
            .walletId(walletId)
            .body(contract.createBulkTransfer(createDummyDestinations(255)).toCell())
            .build();

    contract.send(config);
    contract.waitForBalanceChangeWithTolerance(45, Utils.toNano(0.05));

    BigInteger balance = contract.getBalance();
    if (balance.longValue() > Utils.toNano(0.07).longValue()) {
      log.error("scenario11 failed");
    }
    log.info("FINISHED SCENARIO 11");
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
