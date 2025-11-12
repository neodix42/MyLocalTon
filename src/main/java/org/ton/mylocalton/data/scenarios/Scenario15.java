package org.ton.mylocalton.data.scenarios;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.utils.MyUtils;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.token.ft.JettonMinter;
import org.ton.ton4j.smartcontract.token.ft.JettonWallet;
import org.ton.ton4j.smartcontract.token.nft.NftUtils;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.utils.MsgUtils;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.utils.Utils;

/**
 * deploy jetton minter, mint jettons, edit minter's jetton content, change minter admin, transfer
 * jettons, burn jettons
 */
@Slf4j
public class Scenario15 implements Scenario {
  AdnlLiteClient adnlLiteClient;

  public Scenario15(AdnlLiteClient adnlLiteClient) {
    this.adnlLiteClient = adnlLiteClient;
  }

  public void run() throws NoSuchAlgorithmException {
    log.info("STARTED SCENARIO 15");

    String NEW_ADMIN2 = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";

    WalletV3R2 adminWallet = (WalletV3R2) new MyUtils().deploy(adnlLiteClient, Utils.toNano(2));
    WalletV3R2 wallet2 = (WalletV3R2) new MyUtils().deploy(adnlLiteClient, Utils.toNano(1));
    log.info("admin wallet address {}", adminWallet.getAddress());
    log.info("second wallet address {}", wallet2.getAddress());

    JettonMinter minter =
        JettonMinter.builder()
            .adnlLiteClient(adnlLiteClient)
            .adminAddress(adminWallet.getAddress())
            .content(
                NftUtils.createOffChainUriCell(
                    "https://raw.githubusercontent.com/neodix42/ton4j/main/1-media/neo-jetton.json"))
            .build();

    log.info("jetton minter address {}", minter.getAddress());

    // DEPLOY MINTER

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.2))
            .stateInit(minter.getStateInit())
            .comment("deploy minter")
            .build();

    adminWallet.send(walletV3Config);
    log.info("deploying minter");
    minter.waitForDeployment(60);

    //    getMinterInfo(minter); // nothing minted, so zero returned

    // MINT JETTONS

    walletV3Config =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.07))
            .body(
                JettonMinter.createMintBody(
                    0,
                    adminWallet.getAddress(),
                    Utils.toNano(0.07),
                    Utils.toNano(100500),
                    null,
                    null,
                    BigInteger.ONE,
                    MsgUtils.createTextMessageBody("minting")))
            .build();

    adminWallet.send(walletV3Config);

    log.info("minting...");
    adminWallet.waitForBalanceChange();

    getMinterInfo(minter);

    // EDIT MINTER'S JETTON CONTENT

    walletV3Config =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.055))
            .body(
                minter.createEditContentBody(
                    "http://localhost/nft-marketplace/my_collection_1.json", 0))
            .build();
    adminWallet.send(walletV3Config);

    log.info("edit minter content, OP 4");
    adminWallet.waitForBalanceChange();

    //    getMinterInfo(minter);

    // CHANGE MINTER ADMIN
    log.info("newAdmin {}", Address.of(NEW_ADMIN2));

    walletV3Config =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(minter.getAddress())
            .amount(Utils.toNano(0.056))
            .body(minter.createChangeAdminBody(0, Address.of(NEW_ADMIN2)))
            .build();
    adminWallet.send(walletV3Config);

    log.info("change minter admin, OP 3");
    adminWallet.waitForBalanceChange();
    getMinterInfo(minter);
    Utils.sleep(10);

    //    log.info("adminWallet balance: {}", Utils.formatNanoValue(adminWallet.getBalance()));
    //    log.info("    wallet2 balance: {}", Utils.formatNanoValue(wallet2.getBalance()));

    JettonWallet adminJettonWallet = minter.getJettonWallet(adminWallet.getAddress());

    // transfer from admin to WALLET2_ADDRESS by sending transfer request to admin's jetton wallet

    walletV3Config =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.057))
            .body(
                JettonWallet.createTransferBody(
                    0,
                    Utils.toNano(444),
                    wallet2.getAddress(), // recipient
                    adminWallet.getAddress(), // response address
                    null, // custom payload
                    BigInteger.ONE, // forward amount
                    MsgUtils.createTextMessageBody("gift") // forward payload
                    ))
            .build();
    adminWallet.send(walletV3Config);

    log.info("transferring 444 jettons...");
    adminWallet.waitForBalanceChange();
    //    log.info("admin balance {}", Utils.formatNanoValue(adminJettonWallet.getBalance()));
    Utils.sleep(10);

    // wallet 2, after received jettons, can use JettonWallet
    JettonWallet jettonWallet2 = minter.getJettonWallet(wallet2.getAddress());
    //    log.info("wallet2 balance {}", Utils.formatNanoValue(jettonWallet2.getBalance()));

    // BURN JETTONS in ADMIN WALLET

    walletV3Config =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(adminJettonWallet.getAddress())
            .amount(Utils.toNano(0.05))
            .body(JettonWallet.createBurnBody(0, Utils.toNano(111), adminWallet.getAddress()))
            .build();
    adminWallet.send(walletV3Config);

    log.info("burning 111 jettons in admin wallet");
    adminWallet.waitForBalanceChange();

    log.info("FINISHED SCENARIO 15");
  }

  private void getMinterInfo(JettonMinter minter) {
    JettonMinterData data = minter.getJettonData();
    log.info("minter adminAddress {}", data.getAdminAddress());
    log.info("minter totalSupply {}", data.getTotalSupply());
    log.info("minter jetton uri {}", data.getJettonContentUri());
  }
}
