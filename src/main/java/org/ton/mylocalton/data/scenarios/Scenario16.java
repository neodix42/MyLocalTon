package org.ton.mylocalton.data.scenarios;

import com.iwebpp.crypto.TweetNaclFast;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.utils.MyUtils;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.token.nft.NftCollection;
import org.ton.ton4j.smartcontract.token.nft.NftItem;
import org.ton.ton4j.smartcontract.token.nft.NftMarketplace;
import org.ton.ton4j.smartcontract.token.nft.NftSale;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.tonlib.Tonlib;
import org.ton.ton4j.utils.Utils;

/**
 * deploy nft collection, deploy 2 nfts on it, deploy nft marketplace, deploy nft1 sale-contract,
 * deploy nft2 sale-contract, get nft1 static data (with response to admin wallet), transfer nft1 to
 * nft1 sale-contract,transfer nft2 to nft2 sale-contract, cancel nft1 sale, buy nft2 at
 * marketplace, edit nft collection content (should fail), change nft collection owner.
 */
@Slf4j
public class Scenario16 implements Scenario {
  private static final String WALLET2_ADDRESS = "EQB6-6po0yspb68p7RRetC-hONAz-JwxG9514IEOKw_llXd5";
  Tonlib tonlib;
  private Address nftItem1Address;
  private Address nftItem2Address;

  public Scenario16(Tonlib tonlib) {
    this.tonlib = tonlib;
  }

  public void run() {
    log.info("STARTED SCENARIO 16");

    WalletV3R2 adminWallet = (WalletV3R2) new MyUtils().deploy(tonlib, Utils.toNano(0.7));
    WalletV3R2 nftItemBuyer = (WalletV3R2) new MyUtils().deploy(tonlib, Utils.toNano(0.3));

    NftCollection nftCollection =
        NftCollection.builder()
            .tonlib(tonlib)
            .adminAddress(adminWallet.getAddress())
            .royalty(0.013)
            .royaltyAddress(adminWallet.getAddress())
            .collectionContentUri(
                "https://raw.githubusercontent.com/neodiX42/ton4j/main/1-media/nft-collection.json")
            .collectionContentBaseUri(
                "https://raw.githubusercontent.com/neodiX42/ton4j/main/1-media/")
            .nftItemCodeHex(WalletCodes.nftItem.getValue())
            .build();

    log.info("NFT collection address {}", nftCollection.getAddress());

    // deploy NFT Collection
    WalletV3Config adminWalletConfig =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(nftCollection.getAddress())
            .amount(Utils.toNano(0.1))
            .stateInit(nftCollection.getStateInit())
            .build();

    adminWallet.send(adminWalletConfig);
    log.info("deploying NFT collection");

    nftCollection.waitForDeployment();

    getNftCollectionInfo(nftCollection);

    Cell body =
        NftCollection.createMintBody(
            0, 0, Utils.toNano(0.06), adminWallet.getAddress(), "nft-item-1.json");

    adminWalletConfig =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(nftCollection.getAddress())
            .amount(Utils.toNano(0.1))
            .body(body)
            .build();

    adminWallet.send(adminWalletConfig);
    Utils.sleep(30, "deploying NFT item #1");

    body =
        NftCollection.createMintBody(
            0, 1, Utils.toNano(0.07), adminWallet.getAddress(), "nft-item-2.json");

    adminWalletConfig =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(nftCollection.getAddress())
            .amount(Utils.toNano(0.1))
            .body(body)
            .build();

    adminWallet.send(adminWalletConfig);
    Utils.sleep(30, "deploying NFT item #2");

    NftMarketplace marketplace =
        NftMarketplace.builder().adminAddress(adminWallet.getAddress()).build();

    log.info("nft marketplace address {}", marketplace.getAddress());

    // deploy own NFT marketplace
    adminWalletConfig =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(marketplace.getAddress())
            .amount(Utils.toNano(0.1))
            .stateInit(marketplace.getStateInit())
            .build();

    adminWallet.send(adminWalletConfig);
    Utils.sleep(30, "deploying nft marketplace");

    // deploy nft sale for item 1
    NftSale nftSale1 =
        NftSale.builder()
            .marketplaceAddress(marketplace.getAddress())
            .nftItemAddress(nftItem1Address)
            .fullPrice(Utils.toNano(0.11))
            .marketplaceFee(Utils.toNano(0.04))
            .royaltyAddress(nftCollection.getAddress())
            .royaltyAmount(Utils.toNano(0.03))
            .build();

    log.info("nft-sale-1 address {}", nftSale1.getAddress());

    adminWalletConfig =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(marketplace.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                CellBuilder.beginCell()
                    .storeUint(1, 32)
                    .storeCoins(Utils.toNano(0.06))
                    .storeRef(nftSale1.getStateInit().toCell())
                    .storeRef(CellBuilder.beginCell().endCell())
                    .endCell())
            .build();

    adminWallet.send(adminWalletConfig);

    Utils.sleep(30, "deploying NFT sale smart-contract for nft item #1");

    // get nft item 1 data
    log.info("nftSale data for nft item #1 {}", nftSale1.getData(tonlib));

    // deploy nft sale for item 2 -----------------------------------------------------------
    NftSale nftSale2 =
        NftSale.builder()
            .marketplaceAddress(marketplace.getAddress())
            .nftItemAddress(Address.of(nftItem2Address))
            .fullPrice(Utils.toNano(0.12))
            .marketplaceFee(Utils.toNano(0.03))
            .royaltyAddress(nftCollection.getAddress())
            .royaltyAmount(Utils.toNano(0.02))
            .build();

    adminWalletConfig =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(marketplace.getAddress())
            .amount(Utils.toNano(0.06))
            .body(
                CellBuilder.beginCell()
                    .storeUint(1, 32)
                    .storeCoins(Utils.toNano(0.06))
                    .storeRef(nftSale2.getStateInit().toCell())
                    .storeRef(CellBuilder.beginCell().endCell())
                    .endCell())
            .build();

    log.info("nft-sale-2 address {}", nftSale2.getAddress().toBounceable());
    adminWallet.send(adminWalletConfig);

    Utils.sleep(30, "deploying NFT sale smart-contract for nft item #2");

    // get nft item 2 data
    log.info("nftSale data for nft item #2 {}", nftSale2.getData(tonlib));

    // sends from adminWallet to nftItem request for static data, response comes to adminWallet
    // https://github.com/ton-blockchain/token-contract/blob/main/nft/nft-item.fc#L131
    getStaticData(adminWallet, Utils.toNano(0.088), nftItem1Address, BigInteger.valueOf(661));

    // transfer nft item to nft sale smart-contract (send amount > full_price+1ton)
    transferNftItem(
        adminWallet,
        Utils.toNano(0.14),
        nftItem1Address,
        BigInteger.ZERO,
        nftSale1.getAddress(),
        Utils.toNano(0.02),
        "gift1".getBytes(),
        adminWallet.getAddress());
    Utils.sleep(20, "transferring item-1 to nft-sale-1 and waiting for seqno update");

    transferNftItem(
        adminWallet,
        Utils.toNano(0.15),
        nftItem2Address,
        BigInteger.ZERO,
        nftSale2.getAddress(),
        Utils.toNano(0.02),
        "gift2".getBytes(),
        adminWallet.getAddress());
    Utils.sleep(20, "transferring item-2 to nft-sale-2 and waiting for seqno update");

    // cancels selling of item1, moves nft-item from nft-sale-1 smc back to adminWallet. nft-sale-1
    // smc becomes uninitialized

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(adminWallet.getWalletId())
            .seqno(adminWallet.getSeqno())
            .destination(nftSale1.getAddress())
            .amount(Utils.toNano(0.1))
            .body(NftSale.createCancelBody(0))
            .build();
    adminWallet.send(walletV3Config);

    Utils.sleep(20, "cancel selling of item1");

    // buy nft-item-2. send fullPrice+minimalGasAmount(1ton)
    walletV3Config =
        WalletV3Config.builder()
            .walletId(nftItemBuyer.getWalletId())
            .seqno(nftItemBuyer.getSeqno())
            .destination(nftSale2.getAddress())
            .amount(Utils.toNano(0.12 + 0.1))
            .build();
    nftItemBuyer.send(walletV3Config);

    // after changed owner this will fail with 401 error - current nft collection is not editable,
    // so nothing happens
    editNftCollectionContent(
        adminWallet,
        Utils.toNano(0.055),
        nftCollection.getAddress(),
        "ton://my-nft/collection.json",
        "ton://my-nft/",
        0.16,
        Address.of(WALLET2_ADDRESS),
        adminWallet.getKeyPair());

    changeNftCollectionOwner(
        adminWallet, Utils.toNano(0.06), nftCollection.getAddress(), Address.of(WALLET2_ADDRESS));

    getRoyaltyParams(adminWallet, Utils.toNano(0.0777), nftCollection.getAddress());

    log.info("FINISHED SCENARIO 16");
  }

  private long getNftCollectionInfo(NftCollection nftCollection) {
    CollectionData data = nftCollection.getCollectionData(tonlib);
    log.info("nft collection info {}", data);
    log.info("nft collection item count {}", data.getNextItemIndex());
    log.info("nft collection owner {}", data.getOwnerAddress());

    nftItem1Address = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ZERO);
    nftItem2Address = nftCollection.getNftItemAddressByIndex(tonlib, BigInteger.ONE);

    log.info("address at index 1 = {}", nftItem1Address);
    log.info("address at index 2 = {}", nftItem2Address);

    Royalty royalty = nftCollection.getRoyaltyParams(tonlib);
    log.info("nft collection royalty address {}", royalty.getRoyaltyAddress());

    return data.getNextItemIndex();
  }

  public void changeNftCollectionOwner(
      WalletV3R2 wallet, BigInteger msgValue, Address nftCollectionAddress, Address newOwner) {

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(wallet.getWalletId())
            .seqno(wallet.getSeqno())
            .destination(nftCollectionAddress)
            .amount(msgValue)
            .body(NftCollection.createChangeOwnerBody(0, newOwner))
            .build();
    wallet.send(walletV3Config);
  }

  public void editNftCollectionContent(
      WalletV3R2 wallet,
      BigInteger msgValue,
      Address nftCollectionAddress,
      String collectionContentUri,
      String nftItemContentBaseUri,
      double royalty,
      Address royaltyAddress,
      TweetNaclFast.Signature.KeyPair keyPair) {

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(wallet.getWalletId())
            .seqno(wallet.getSeqno())
            .destination(nftCollectionAddress)
            .amount(msgValue)
            .body(
                NftCollection.createEditContentBody(
                    0, collectionContentUri, nftItemContentBaseUri, royalty, royaltyAddress))
            .build();
    wallet.send(walletV3Config);
  }

  public void getRoyaltyParams(
      WalletV3R2 wallet, BigInteger msgValue, Address nftCollectionAddress) {

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(wallet.getWalletId())
            .seqno(wallet.getSeqno())
            .destination(nftCollectionAddress)
            .amount(msgValue)
            .body(NftCollection.createGetRoyaltyParamsBody(0))
            .build();
    wallet.send(walletV3Config);
  }

  private void transferNftItem(
      WalletV3R2 wallet,
      BigInteger msgValue,
      Address nftItemAddress,
      BigInteger queryId,
      Address nftSaleAddress,
      BigInteger forwardAmount,
      byte[] forwardPayload,
      Address responseAddress) {

    WalletV3Config walletV3Config =
        WalletV3Config.builder()
            .walletId(wallet.getWalletId())
            .seqno(wallet.getSeqno())
            .destination(nftItemAddress)
            .amount(msgValue)
            .body(
                NftItem.createTransferBody(
                    queryId, nftSaleAddress, forwardAmount, forwardPayload, responseAddress))
            .build();
    wallet.send(walletV3Config);
  }

  private void getStaticData(
      WalletV3R2 wallet, BigInteger msgValue, Address nftItemAddress, BigInteger queryId) {
    WalletV3Config config =
        WalletV3Config.builder()
            .walletId(wallet.getWalletId())
            .seqno(wallet.getSeqno())
            .destination(nftItemAddress)
            .amount(msgValue)
            .body(NftItem.createGetStaticDataBody(queryId))
            .build();
    wallet.send(config);
  }
}
