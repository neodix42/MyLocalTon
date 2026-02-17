package org.ton.mylocalton.data.scenarios;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.utils.MyUtils;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.smartcontract.multisig.MultiSigWalletV2;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.utils.Utils;

/**
 * deploy signer2 wallet, deploy MultiSig V2 with 2 out 3 consensus, deploy order from admin wallet,
 * sign order from signer2 wallet, execute order
 */
@Slf4j
public class Scenario14 implements Scenario {
  AdnlLiteClient adnlLiteClient;

  public Scenario14(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() throws NoSuchAlgorithmException {
    // !!! works only when workchain enabled, since order contract created at workchain=0
    log.info("STARTED SCENARIO 13");
    long walletId = Math.abs(Utils.getRandomInt());

    Address dummyRecipient1 = Address.of(Utils.generateRandomAddress(0));
    Address dummyRecipient2 = Address.of(Utils.generateRandomAddress(0));

    WalletV3R2 deployer = (WalletV3R2) new MyUtils().deploy(adnlLiteClient, Utils.toNano(0.3));
    WalletV3R2 signer2 = (WalletV3R2) new MyUtils().deploy(adnlLiteClient, Utils.toNano(0.3));

    WalletV3R2 signer3 =
        WalletV3R2.builder()
            .tonProvider(adnlLiteClient)
            .keyPair(Utils.generateSignatureKeyPair())
            .walletId(42)
            .build();

    MultiSigWalletV2 multiSigWalletV2 =
        MultiSigWalletV2.builder()
            .tonProvider(adnlLiteClient)
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
            .sendMode(SendMode.PAY_GAS_SEPARATELY_AND_IGNORE_ERRORS)
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
                    MsgUtils.createInternalMessageRelaxed(
                            dummyRecipient1, Utils.toNano(0.025), null, null, null, false)
                        .toCell()),
                MultiSigWalletV2.createSendMessageAction(
                    1,
                    MsgUtils.createInternalMessageRelaxed(
                            dummyRecipient2, Utils.toNano(0.026), null, null, null, false)
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

    Utils.sleep(10);

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

    Utils.sleep(10);

    log.info(
        "orderData when twice approved {}", multiSigWalletV2.getOrderData(BigInteger.valueOf(0)));

    BigInteger balanceRecipient1 = adnlLiteClient.getBalance(dummyRecipient1);
    BigInteger balanceRecipient2 = adnlLiteClient.getBalance(dummyRecipient2);

    if (balanceRecipient1.longValue() <= 0) {
      log.info("gut");
    }

    log.info("FINISHED SCENARIO 14");
  }
}
