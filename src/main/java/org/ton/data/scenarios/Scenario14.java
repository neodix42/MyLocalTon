package org.ton.data.scenarios;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.ton.data.utils.MyUtils;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.smartcontract.multisig.MultiSigWalletV2;
import org.ton.java.smartcontract.types.*;
import org.ton.java.smartcontract.utils.MsgUtils;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

/**
 * deploy signer2 wallet, deploy MultiSig V2 with 2 out 3 consensus, deploy order from admin wallet,
 * sign order from signer2 wallet, execute order
 */
@Slf4j
public class Scenario14 implements Scenario {
  Tonlib tonlib;

  public Scenario14(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() throws NoSuchAlgorithmException {
    // !!! works only when workchain enabled, since order contract created at workchain=0
    log.info("STARTED SCENARIO 13");
    long walletId = Math.abs(Utils.getRandomInt());

    Address dummyRecipient1 = Address.of(Utils.generateRandomAddress(0));
    Address dummyRecipient2 = Address.of(Utils.generateRandomAddress(0));

    WalletV3R2 deployer = (WalletV3R2) new MyUtils().deploy(tonlib, Utils.toNano(0.3));
    WalletV3R2 signer2 = (WalletV3R2) new MyUtils().deploy(tonlib, Utils.toNano(0.3));

    WalletV3R2 signer3 =
        WalletV3R2.builder()
            .tonlib(tonlib)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    //    log.info("deployer {}", deployer.getAddress().toRaw());
    //    log.info("signer2 {}", signer2.getAddress().toRaw());
    //    log.info("signer3 {}", signer3.getAddress().toRaw());
    //    log.info("recipient1 {}", dummyRecipient1.toRaw());
    //    log.info("recipient2 {}", dummyRecipient2.toRaw());
    //
    //    log.info("deployer seqno {}", deployer.getSeqno());
    //    log.info("signer2 seqno {}", signer2.getSeqno());

    MultiSigWalletV2 multiSigWalletV2 =
        MultiSigWalletV2.builder()
            .tonlib(tonlib)
            .config(
                MultiSigV2Config.builder()
                    .allowArbitraryOrderSeqno(false)
                    .nextOrderSeqno(BigInteger.ZERO)
                    .threshold(2)
                    .numberOfSigners(3)
                    .signers(
                        Arrays.asList(
                            deployer.getAddress(), signer2.getAddress(), signer3.getAddress()))
                    .proposers(Collections.emptyList())
                    .build())
            .build();

    log.info("multisig address {}", multiSigWalletV2.getAddress().toBounceable());

    // deploy multisig from admin wallet on testnet
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(deployer.getWalletId())
            .seqno(1)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(multiSigWalletV2.getStateInit())
            .mode(3)
            .build();
    deployer.send(config);
    deployer.waitForDeployment(30);
    Utils.sleep(10, "pause");

    // send external msg to admin wallet that sends internal msg to multisig with body to create
    // order-contract

    Cell orderBody =
        MultiSigWalletV2.createOrder(
            Arrays.asList(
                MultiSigWalletV2.createSendMessageAction(
                    1,
                    MsgUtils.createInternalMessage(
                            dummyRecipient1, Utils.toNano(0.025), null, null, false)
                        .toCell()),
                MultiSigWalletV2.createSendMessageAction(
                    1,
                    MsgUtils.createInternalMessage(
                            dummyRecipient2, Utils.toNano(0.026), null, null, false)
                        .toCell())));
    config =
        WalletV3Config.builder()
            .walletId(deployer.getWalletId())
            .seqno(2)
            .destination(multiSigWalletV2.getAddress())
            .amount(Utils.toNano(0.1))
            .body(
                MultiSigWalletV2.newOrder(
                    0, BigInteger.ZERO, true, 0, Utils.now() + 3600, orderBody))
            .build();

    deployer.send(config);
    deployer.waitForBalanceChange();

    Utils.sleep(20);

    Address orderAddress = multiSigWalletV2.getOrderAddress(BigInteger.ZERO);
    log.info("orderAddress {} {}", orderAddress, orderAddress.toRaw());

    log.info(
        "orderData when once approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    log.info(
        "getOrderEstimate {}", multiSigWalletV2.getOrderEstimate(orderBody, Utils.now() + 3600));

    log.info("sending approve from signer2 to order address");
    config =
        WalletV3Config.builder()
            .walletId(signer2.getWalletId())
            .seqno(1)
            .destination(orderAddress)
            .amount(Utils.toNano(0.05))
            .body(MultiSigWalletV2.approve(0, 1))
            .build();
    signer2.send(config);
    signer2.waitForBalanceChange();

    Utils.sleep(20);

    log.info(
        "orderData when twice approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    BigInteger balanceRecipient1 = tonlib.getAccountBalance(dummyRecipient1);
    BigInteger balanceRecipient2 = tonlib.getAccountBalance(dummyRecipient2);

    if (balanceRecipient1.longValue() <= 0) {
      log.info("gut");
    }

    log.info("FINISHED SCENARIO 14");
  }
}
