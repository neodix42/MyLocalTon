package org.ton.mylocalton.wallet;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.mylocalton.actions.MyLocalTon.tonlib;

import com.iwebpp.crypto.TweetNaclFast;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.mylocalton.enums.LiteClientEnum;
import org.ton.mylocalton.executors.fift.Fift;
import org.ton.mylocalton.executors.liteclient.LiteClient;
import org.ton.mylocalton.parameters.SendToncoinsParam;
import org.ton.mylocalton.settings.Node;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.mnemonic.Mnemonic;
import org.ton.ton4j.smartcontract.highload.HighloadWallet;
import org.ton.ton4j.smartcontract.highload.HighloadWalletV3;
import org.ton.ton4j.smartcontract.types.*;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R1;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R2;
import org.ton.ton4j.smartcontract.wallet.v1.WalletV1R3;
import org.ton.ton4j.smartcontract.wallet.v2.WalletV2R1;
import org.ton.ton4j.smartcontract.wallet.v2.WalletV2R2;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R1;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.smartcontract.wallet.v4.WalletV4R2;
import org.ton.ton4j.tonlib.types.ExtMessageInfo;
import org.ton.ton4j.tonlib.types.RawAccountState;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class MyWallet {

  public static final BigDecimal BLN1 = BigDecimal.valueOf(1000000000);
  static final double MINIMUM_TONCOINS = 0.09;
  private final LiteClient liteClient;

  public MyWallet() {
    liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);
  }

  //    boolean walletHasStateInit(AccountState accountState) {
  //        return nonNull(accountState.getStatus()); // has stateInit with some toncoins
  //    }

  public void walletHasContractInstalled(
      LiteClient liteClient, Node fromNode, Address walletAddress, String contractQueryBocFile)
      throws Exception {
    RawAccountState accountState;
    do {
      Thread.sleep(2000);
      accountState = tonlib.getRawAccountState(walletAddress);
      log.debug("waiting for smc to be installed on {}", walletAddress.toString(false));
    } while (isNull(accountState) || StringUtils.isEmpty(accountState.getCode()));

    if (StringUtils.isNotEmpty(contractQueryBocFile)) {
      FileUtils.deleteQuietly(new File(fromNode.getTonDbDir() + contractQueryBocFile));
    }
    log.debug(
        "wallet contract installed in blockchain, wallet {}",
        Address.of(walletAddress).toString(false));
  }

  public void walletHasEnoughFunds(Address walletAddress) throws Exception {
    RawAccountState accountState;
    do {
      Thread.sleep(2000);
      accountState = tonlib.getRawAccountState(walletAddress);
      log.debug("waiting for smc to be installed on {}", walletAddress.toString(false));
    } while (isNull(accountState)
        || (new BigDecimal(accountState.getBalance())
                .compareTo(BigDecimal.valueOf(MINIMUM_TONCOINS).multiply(BLN1))
            < 0));
    log.debug(
        "wallet has enough funds, wallet {}, balance {}",
        walletAddress.toString(false),
        accountState.getBalance());
  }

  public void installWalletSmartContract(Node fromNode, WalletAddress walletAddress)
      throws Exception {
    log.info("installing wallet smart-contract {}", walletAddress.getFullWalletAddress());
    // check if money arrived
    walletHasEnoughFunds(Address.of(walletAddress.getFullWalletAddress()));

    String resultSendBoc;
    // installing state-init
    if (nonNull(walletAddress.getWalletQueryFileBocLocation())) {
      resultSendBoc =
          liteClient.executeSendfile(fromNode, walletAddress.getWalletQueryFileBocLocation());
      log.debug(resultSendBoc);
    } else {
      tonlib.sendRawMessage(walletAddress.getMessage().toCell().toBase64(false));
    }

    walletHasContractInstalled(
        liteClient, fromNode, Address.of(walletAddress.getFullWalletAddress()), "");
  }

  /**
   * Used to send toncoins from one-time-wallet, where do we have prvkey, which is used in fift
   * script
   */
  public boolean sendTonCoins(SendToncoinsParam sendToncoinsParam) {
    try {
      Address fromAddress = Address.of(sendToncoinsParam.getFromWallet().getFullWalletAddress());
      Address toAddress = Address.of(sendToncoinsParam.getDestAddr());
      if (nonNull(sendToncoinsParam.getForceBounce()) && (sendToncoinsParam.getForceBounce())) {
        toAddress = Address.of(toAddress.toString(true, false, true));
      }

      long seqno = 0;
      if ((sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.highload))
          || (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.highloadV3))
          || (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R1))) {
        log.info("sendTonCoins, does not have seqno, use default seqno=0");
      } else {
        seqno = tonlib.getSeqno(fromAddress);
      }

      log.debug("seqno {}", seqno);

      //      if (seqno == -1L) {
      //        log.error("Error retrieving seqno from contract {}", fromAddress.toRaw());
      //        return false;
      //      }

      if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.highload)) {
        HighloadWallet highloadWallet =
            HighloadWallet.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getFromWallet().getWc())
                .tonlib(tonlib)
                .walletId(sendToncoinsParam.getFromWallet().getSubWalletId())
                .queryId(BigInteger.ZERO)
                .build();
        log.info("balance {}", tonlib.getAccountBalance(highloadWallet.getAddress()));
        HighloadConfig config =
            HighloadConfig.builder()
                .walletId(sendToncoinsParam.getFromWallet().getSubWalletId())
                .queryId(BigInteger.valueOf(Instant.now().getEpochSecond() + 10 * 60L << 32))
                .destinations(
                    List.of(
                        Destination.builder()
                            .mode(3)
                            .address(toAddress.toRaw())
                            .amount(sendToncoinsParam.getAmount())
                            .build()))
                .build();
        ExtMessageInfo extMessageInfo = highloadWallet.send(config);
        log.info("ExtMessageInfo {}", extMessageInfo);
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R1)) {
        WalletV1R1 walletV1R1 =
            WalletV1R1.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .tonlib(tonlib)
                .build();

        WalletV1R1Config walletV1R1Config =
            WalletV1R1Config.builder()
                .destination(toAddress)
                .amount(sendToncoinsParam.getAmount())
                .seqno(seqno)
                .comment(sendToncoinsParam.getComment())
                .build();
        walletV1R1.send(walletV1R1Config);
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R2)) {
        WalletV1R2 walletV1R2 =
            WalletV1R2.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .tonlib(tonlib)
                .build();

        WalletV1R2Config walletV1R2Config =
            WalletV1R2Config.builder()
                .destination(toAddress)
                .amount(sendToncoinsParam.getAmount())
                .seqno(seqno)
                .comment(sendToncoinsParam.getComment())
                .build();
        walletV1R2.send(walletV1R2Config);
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R3)) {
        WalletV1R3 walletV1R3 =
            WalletV1R3.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .tonlib(tonlib)
                .build();

        WalletV1R3Config walletV1R3Config =
            WalletV1R3Config.builder()
                .destination(toAddress)
                .amount(sendToncoinsParam.getAmount())
                .seqno(seqno)
                .comment(sendToncoinsParam.getComment())
                .build();
        walletV1R3.send(walletV1R3Config);
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V2R1)) {
        WalletV2R1 walletV2R1 =
            WalletV2R1.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .tonlib(tonlib)
                .build();

        WalletV2R1Config walletV2R1Config =
            WalletV2R1Config.builder()
                .destination1(toAddress)
                .amount1(sendToncoinsParam.getAmount())
                .seqno(seqno)
                .comment(sendToncoinsParam.getComment())
                .build();
        walletV2R1.send(walletV2R1Config);
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V2R2)) {
        WalletV2R2 walletV2R2 =
            WalletV2R2.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .tonlib(tonlib)
                .build();

        WalletV2R2Config walletV2R2Config =
            WalletV2R2Config.builder()
                .destination1(toAddress)
                .amount1(sendToncoinsParam.getAmount())
                .seqno(seqno)
                .comment(sendToncoinsParam.getComment())
                .build();
        walletV2R2.send(walletV2R2Config);
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V3R1)) {
        WalletV3R1 walletV3R1 =
            WalletV3R1.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .walletId(sendToncoinsParam.getFromSubWalletId())
                .tonlib(tonlib)
                .build();

        WalletV3Config walletV3Config =
            WalletV3Config.builder()
                .walletId(sendToncoinsParam.getFromSubWalletId())
                .destination(toAddress)
                .amount(sendToncoinsParam.getAmount())
                .seqno(seqno)
                .comment(sendToncoinsParam.getComment())
                .build();
        walletV3R1.send(walletV3Config);
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V3R2)) {
        WalletV3R2 walletV3R2 =
            WalletV3R2.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .walletId(sendToncoinsParam.getFromSubWalletId())
                .tonlib(tonlib)
                .build();

        if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
          WalletV3Config walletV3Config =
              WalletV3Config.builder()
                  .walletId(sendToncoinsParam.getFromSubWalletId())
                  .destination(toAddress)
                  .amount(sendToncoinsParam.getAmount())
                  .seqno(seqno)
                  .comment(sendToncoinsParam.getComment())
                  .build();
          walletV3R2.send(walletV3Config);
        } else {
          if (nonNull(sendToncoinsParam.getBocLocation())) {
            byte[] boc =
                FileUtils.readFileToByteArray(new File(sendToncoinsParam.getBocLocation()));
            Cell bodyCell = Cell.fromBoc(boc);
            WalletV3Config walletV3Config =
                WalletV3Config.builder()
                    .walletId(sendToncoinsParam.getFromSubWalletId())
                    .destination(toAddress)
                    .amount(sendToncoinsParam.getAmount())
                    .seqno(seqno)
                    .body(bodyCell)
                    .build();
            walletV3R2.send(walletV3Config);
          } else {
            WalletV3Config walletV3Config =
                WalletV3Config.builder()
                    .walletId(sendToncoinsParam.getFromSubWalletId())
                    .destination(toAddress)
                    .amount(sendToncoinsParam.getAmount())
                    .seqno(seqno)
                    .build();
            walletV3R2.send(walletV3Config);
          }
        }
      } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V4R2)) {
        WalletV4R2 walletV4R2 =
            WalletV4R2.builder()
                .keyPair(
                    Utils.generateSignatureKeyPairFromSeed(
                        Utils.hexToSignedBytes(
                            sendToncoinsParam.getFromWallet().getPrivateKeyHex())))
                .wc(sendToncoinsParam.getWorkchain())
                .walletId(sendToncoinsParam.getFromSubWalletId())
                .tonlib(tonlib)
                .build();

        WalletV4R2Config walletV4Config =
            WalletV4R2Config.builder()
                .walletId(sendToncoinsParam.getFromSubWalletId())
                .destination(toAddress)
                .amount(sendToncoinsParam.getAmount())
                .seqno(seqno)
                .comment(sendToncoinsParam.getComment())
                .build();
        walletV4R2.send(walletV4Config);
      } else if ((sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.master))
          || (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.config))) {
        // send using fift from master and config wallets using base-file
        String externalMsgLocation =
            new Fift().prepareSendTonCoinsFromNodeWallet(sendToncoinsParam, seqno);
        if (isNull(externalMsgLocation)) {
          return false;
        }
        log.debug(
            liteClient.executeSendfile(sendToncoinsParam.getExecutionNode(), externalMsgLocation));
      } else {
        log.error("{} wallet version is not supported", sendToncoinsParam.getFromWalletVersion());
      }

      log.info(
          "Sent {} nano Toncoins by {} from {} to {}",
          sendToncoinsParam.getAmount(),
          sendToncoinsParam.getExecutionNode().getNodeName(),
          fromAddress.toRaw(),
          toAddress.toRaw());

      int counter = 0;

      if ((sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.highload))
          || (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.highloadV3))
          || (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R1))) {
        return true;
      } else {
        while (true) {
          Utils.sleep(3);
          long newSeqno = tonlib.getSeqno(fromAddress);
          if (newSeqno > seqno) {
            return true;
          }
          log.info(
              "{} waiting for wallet {} to update seqno. oldSeqno {}, newSeqno {}",
              fromAddress.toString(false),
              sendToncoinsParam.getExecutionNode().getNodeName(),
              seqno,
              newSeqno);
          counter++;
          if (counter > 15) {
            log.error(
                "Error sending {} Toncoins by {} from {} to {}.",
                sendToncoinsParam.getToncoinsAmount(),
                sendToncoinsParam.getExecutionNode().getNodeName(),
                fromAddress.toString(false),
                toAddress.toString(false));
            return false;
          }
        }
      }
    } catch (Throwable te) {
      log.error(ExceptionUtils.getStackTrace(te));
      return false;
    }
  }

  private Contract createInitExternalMessageByWalletVersion(
      WalletVersion walletVersion, byte[] privateKey, long wc, long walletId) {
    switch (WalletVersion.getKeyByValue(walletVersion.getValue())) {
      case V1R1:
        return WalletV1R1.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .build();
      case V1R2:
        return WalletV1R2.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .build();
      case V1R3:
        return WalletV1R3.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .build();
      case V2R1:
        return WalletV2R1.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .build();
      case V2R2:
        return WalletV2R2.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .build();
      case V3R1:
        return WalletV3R1.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .walletId(walletId)
            .build();
      case V3R2:
        return WalletV3R2.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .walletId(walletId)
            .build();
      case V4R2:
        return WalletV4R2.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .walletId(walletId)
            .build();
      case highload:
        return HighloadWallet.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .walletId(walletId)
            .queryId(BigInteger.valueOf(Instant.now().getEpochSecond() + 5 * 60L << 32))
            .build();
      case highloadV3:
        return HighloadWalletV3.builder()
            .keyPair(Utils.generateSignatureKeyPairFromSeed(privateKey))
            .wc(wc)
            .tonlib(tonlib)
            .walletId(walletId)
            .build();
      default:
        throw new Error("unsupported wallet version");
    }
  }

  public WalletAddress createWalletByVersion(
      WalletVersion walletVersion, long workchainId, long walletId) throws Exception {

    List<String> mnemonic = Mnemonic.generate(24, "");
    org.ton.ton4j.mnemonic.Pair keyPair = Mnemonic.toKeyPair(mnemonic, "");

    TweetNaclFast.Signature.KeyPair keyPairSig =
        TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

    Contract msg =
        createInitExternalMessageByWalletVersion(
            walletVersion, keyPairSig.getSecretKey(), workchainId, walletId);
    Address address = msg.getAddress();

    String fullAddress = address.toString(false).toUpperCase();
    String nonBounceableBase64url = address.toString(true, true, false, true);
    String bounceableBase64url = address.toString(true, true, true, true);
    String nonBounceableBase64 = address.toString(true, false, false, true);
    String bounceableBase64 = address.toString(true, false, true, true);

    return WalletAddress.builder()
        .nonBounceableAddressBase64Url(nonBounceableBase64url)
        .bounceableAddressBase64url(bounceableBase64url)
        .nonBounceableAddressBase64(nonBounceableBase64)
        .bounceableAddressBase64(bounceableBase64)
        .fullWalletAddress(fullAddress)
        .wc(Long.parseLong(fullAddress.substring(0, fullAddress.indexOf(":"))))
        .subWalletId(walletId)
        .hexWalletAddress(fullAddress.substring(fullAddress.indexOf(":") + 1))
        .message(msg.prepareDeployMsg())
        .mnemonic(String.join(" ", mnemonic))
        .privateKeyHex(Hex.encodeHexString(keyPair.getSecretKey()))
        .publicKeyHex(Hex.encodeHexString(keyPair.getPublicKey()))
        .build();
  }

  public WalletAddress createFaucetWalletByVersion(
      WalletVersion walletVersion, long workchainId, long walletId, String privateKey) {

    byte[] secretKey = Utils.hexToSignedBytes(privateKey);
    TweetNaclFast.Signature.KeyPair keyPairSig =
        TweetNaclFast.Signature.keyPair_fromSeed(secretKey);

    //    List<String> mnemonicList = Arrays.asList(privateKey.split(" "));
    //    org.ton.java.mnemonic.Pair keyPair = Mnemonic.toKeyPair(mnemonicList, "");

    //    TweetNaclFast.Signature.KeyPair keyPairSig =
    //        TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

    Contract msg =
        createInitExternalMessageByWalletVersion(
            walletVersion, keyPairSig.getSecretKey(), workchainId, walletId);
    Address address = msg.getAddress();

    String fullAddress = address.toString(false).toUpperCase();
    String nonBounceableBase64url = address.toString(true, true, false, true);
    String bounceableBase64url = address.toString(true, true, true, true);
    String nonBounceableBase64 = address.toString(true, false, false, true);
    String bounceableBase64 = address.toString(true, false, true, true);

    return WalletAddress.builder()
        .nonBounceableAddressBase64Url(nonBounceableBase64url)
        .bounceableAddressBase64url(bounceableBase64url)
        .nonBounceableAddressBase64(nonBounceableBase64)
        .bounceableAddressBase64(bounceableBase64)
        .fullWalletAddress(fullAddress)
        .wc(Long.parseLong(fullAddress.substring(0, fullAddress.indexOf(":"))))
        .subWalletId(walletId)
        .hexWalletAddress(fullAddress.substring(fullAddress.indexOf(":") + 1))
        .message(msg.prepareDeployMsg())
        .mnemonic(privateKey)
        .privateKeyHex(Hex.encodeHexString(keyPairSig.getSecretKey()))
        .publicKeyHex(Hex.encodeHexString(keyPairSig.getPublicKey()))
        .build();
  }
}
