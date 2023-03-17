package org.ton.wallet;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.fift.Fift;
import org.ton.executors.liteclient.LiteClient;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR1;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR2;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR3;
import org.ton.java.smartcontract.wallet.v2.WalletV2ContractR1;
import org.ton.java.smartcontract.wallet.v2.WalletV2ContractR2;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR1;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR2;
import org.ton.java.smartcontract.wallet.v4.WalletV4ContractR2;
import org.ton.java.tonlib.types.RawAccountState;
import org.ton.java.utils.Utils;
import org.ton.parameters.SendToncoinsParam;
import org.ton.settings.Node;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.actions.MyLocalTon.tonlib;

@Slf4j
public class MyWallet {

    private final LiteClient liteClient;

    public MyWallet() {
        liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);
    }

    public static final BigDecimal BLN1 = BigDecimal.valueOf(1000000000);

    static final double MINIMUM_TONCOINS = 0.09;

//    boolean walletHasStateInit(AccountState accountState) {
//        return nonNull(accountState.getStatus()); // has stateInit with some toncoins
//    }

    public void walletHasContractInstalled(LiteClient liteClient, Node fromNode, Address walletAddress, String contractQueryBocFile) throws Exception {
        RawAccountState accountState;
        do {
            Thread.sleep(2000);
            accountState = tonlib.getRawAccountState(walletAddress);
            log.debug("waiting for smc to be installed on {}", walletAddress.toString(false));
        } while (isNull(accountState) || StringUtils.isEmpty(accountState.getCode()));

        if (StringUtils.isNotEmpty(contractQueryBocFile)) {
            FileUtils.deleteQuietly(new File(fromNode.getTonDbDir() + contractQueryBocFile));
        }
        log.debug("wallet contract installed in blockchain, wallet {}", Address.of(walletAddress).toString(false));
    }

    public void walletHasEnoughFunds(Address walletAddress) throws Exception {
        RawAccountState accountState;
        do {
            Thread.sleep(2000);
            accountState = tonlib.getRawAccountState(walletAddress);
            log.debug("waiting for smc to be installed on {}", walletAddress.toString(false));
        } while (isNull(accountState) || (new BigDecimal(accountState.getBalance()).compareTo(BigDecimal.valueOf(MINIMUM_TONCOINS).multiply(BLN1)) < 0));
        log.debug("wallet has enough funds, wallet {}, balance {}", walletAddress.toString(false), accountState.getBalance());
    }

    public void installWalletSmartContract(Node fromNode, WalletAddress walletAddress) throws Exception {
        log.info("installing wallet smart-contract {}", walletAddress.getFullWalletAddress());
        //check if money arrived
        walletHasEnoughFunds(Address.of(walletAddress.getFullWalletAddress()));

        String resultSendBoc;
        // installing state-init
        if (nonNull(walletAddress.getWalletQueryFileBocLocation())) {
            resultSendBoc = liteClient.executeSendfile(fromNode, walletAddress.getWalletQueryFileBocLocation());
            log.debug(resultSendBoc);
        } else {
            tonlib.sendRawMessage(walletAddress.getInitExternalMessage().message.toBocBase64(false));
        }

        walletHasContractInstalled(liteClient, fromNode, Address.of(walletAddress.getFullWalletAddress()), "");
    }

    /**
     * Used to send toncoins from one-time-wallet, where do we have prvkey, which is used in fift script
     */

    public boolean sendTonCoins(SendToncoinsParam sendToncoinsParam) {
        try {
            Address fromAddress = Address.of(sendToncoinsParam.getFromWallet().getFullWalletAddress());
            Address toAddress = Address.of(sendToncoinsParam.getDestAddr());
            if (nonNull(sendToncoinsParam.getForceBounce()) && (sendToncoinsParam.getForceBounce())) {
                toAddress = Address.of(toAddress.toString(true, false, true));
            }

            long seqno = -1;
            if (!sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R1)) {
                seqno = tonlib.getSeqno(fromAddress);
            }
            log.debug("seqno {}", seqno);

            if (seqno == -1L) {
                log.error("Error retrieving seqno from contract {}", fromAddress.toString(false));
                return false;
            }

            Options options = Options.builder()
                    .publicKey(nonNull(sendToncoinsParam.getFromWallet().getPublicKeyHex()) ? Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPublicKeyHex()) : null)
                    .walletId(sendToncoinsParam.getFromSubWalletId())
                    .wc(sendToncoinsParam.getFromWallet().getWc())
                    .build();

            log.debug("sending using wallet version {}", sendToncoinsParam.getFromWalletVersion());

            WalletContract contract;

            if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R1)) {
                contract = new Wallet(WalletVersion.V1R1, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV1ContractR1) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    ((WalletV1ContractR1) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                }
            } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R2)) {
                contract = new Wallet(WalletVersion.V1R2, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV1ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    ((WalletV1ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                }
            } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R3)) {
                contract = new Wallet(WalletVersion.V1R3, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV1ContractR3) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    ((WalletV1ContractR3) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                }
            } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V2R1)) {
                contract = new Wallet(WalletVersion.V2R1, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV2ContractR1) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    ((WalletV2ContractR1) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                }
            } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V2R2)) {
                contract = new Wallet(WalletVersion.V2R2, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV2ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    ((WalletV2ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                }
            } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V3R1)) {
                contract = new Wallet(WalletVersion.V3R1, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV3ContractR1) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    ((WalletV3ContractR1) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                }
            } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V3R2)) {
                contract = new Wallet(WalletVersion.V3R2, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV3ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    if (nonNull(sendToncoinsParam.getBocLocation())) {
                        byte[] boc = FileUtils.readFileToByteArray(new File(sendToncoinsParam.getBocLocation()));
                        Cell bodyCell = Cell.fromBoc(boc);
                        ((WalletV3ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, bodyCell);
                    } else {
                        ((WalletV3ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                    }
                }
            } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V4R2)) {
                contract = new Wallet(WalletVersion.V4R2, options).create();
                if (StringUtils.isNoneEmpty(sendToncoinsParam.getComment())) {
                    ((WalletV4ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno, sendToncoinsParam.getComment());
                } else {
                    ((WalletV4ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), toAddress, sendToncoinsParam.getAmount(), seqno);
                }
            } else if ((sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.master)) || (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.config))) {
                // send using fift from master and config wallets using base-file
                String externalMsgLocation = new Fift().prepareSendTonCoinsFromNodeWallet(sendToncoinsParam, seqno);
                if (isNull(externalMsgLocation)) {
                    return false;
                }
                log.debug(liteClient.executeSendfile(sendToncoinsParam.getExecutionNode(), externalMsgLocation));
            } else {
                log.error("{} wallet version is not supported", sendToncoinsParam.getFromWalletVersion());
            }

            log.info("Sent {} nano Toncoins by {} from {} to {}",
                    sendToncoinsParam.getAmount(),
                    sendToncoinsParam.getExecutionNode().getNodeName(),
                    fromAddress.toString(false),
                    toAddress.toString(false));

            int counter = 0;

            if (!sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R1)) { // better check if seqno method exist
                while (true) {
                    Thread.sleep(3 * 1000);
                    long newSeqno = liteClient.executeGetSeqno(sendToncoinsParam.getExecutionNode(), fromAddress.toString(false));
                    if (newSeqno > seqno) {
                        return true;
                    }
                    log.info("{} waiting for wallet {} to update seqno. oldSeqno {}, newSeqno {}", fromAddress.toString(false), sendToncoinsParam.getExecutionNode().getNodeName(), seqno, newSeqno);
                    counter++;
                    if (counter > 15) {
                        log.error("Error sending {} Toncoins by {} from {} to {}.",
                                sendToncoinsParam.getToncoinsAmount(),
                                sendToncoinsParam.getExecutionNode().getNodeName(),
                                fromAddress.toString(false),
                                toAddress.toString(false));
                        return false;
                    }
                }
            } else {
                return true;
            }
        } catch (Throwable te) {
            log.error(ExceptionUtils.getStackTrace(te));
            return false;
        }
    }

    public WalletAddress createWalletByVersion(WalletVersion walletVersion, long workchainId, long walletId) throws Exception {

        List<String> mnemonic = Mnemonic.generate(24, "");
        org.ton.java.mnemonic.Pair keyPair = Mnemonic.toKeyPair(mnemonic, "");

        TweetNaclFast.Signature.KeyPair keyPairSig = TweetNaclFast.Signature.keyPair_fromSeed(keyPair.getSecretKey());

        Options options = Options.builder()
                .publicKey(keyPairSig.getPublicKey())
                .wc(workchainId)
                .walletId(walletId)
                .build();

        WalletContract contract = new Wallet(walletVersion, options).create();

        InitExternalMessage msg = contract.createInitExternalMessage(keyPairSig.getSecretKey());
        Address address = msg.address;

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
                .initExternalMessage(msg)
                .mnemonic(String.join(" ", mnemonic))
                .privateKeyHex(Hex.encodeHexString(keyPair.getSecretKey()))
                .publicKeyHex(Hex.encodeHexString(keyPair.getPublicKey()))
                .build();
    }
}
