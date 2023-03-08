package org.ton.wallet;

import com.iwebpp.crypto.TweetNaclFast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.fift.Fift;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.LiteClientAccountState;
import org.ton.java.address.Address;
import org.ton.java.mnemonic.Mnemonic;
import org.ton.java.smartcontract.types.InitExternalMessage;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.smartcontract.wallet.Options;
import org.ton.java.smartcontract.wallet.Wallet;
import org.ton.java.smartcontract.wallet.WalletContract;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR2;
import org.ton.java.smartcontract.wallet.v1.WalletV1ContractR3;
import org.ton.java.smartcontract.wallet.v3.WalletV3ContractR2;
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

    public static boolean walletHasContractInstalled(LiteClient liteClient, Node fromNode, Address walletAddress, String contractQueryBocFile) throws Exception {
//        FullAccountState accountState;
//        RawAccountState accountState;
        LiteClientAccountState accountState;

        do {
            Thread.sleep(2000);
//            accountState = tonlib.getRawAccountState(org.ton.java.address.Address.of(walletAddress.getFullWalletAddress()));
//            accountState = tonlib.getAccountState(org.ton.java.address.Address.of(walletAddress.getFullWalletAddress()));
            accountState = LiteClientParser.parseGetAccount(liteClient.executeGetAccount(fromNode, walletAddress.toString(false)));
            log.debug("waiting for smc to be installed on {}", walletAddress.toString(false));
        } while (isNull(accountState) || (accountState.getStatus().equals("Uninitialized")));
//        } while (isNull(accountState) || (StringUtils.isEmpty(accountState.getCode())));
//        } while (isNull(accountState) || isNull(accountState.getAccount_state()) || (StringUtils.isEmpty(accountState.getAccount_state().getCode())));

        if (StringUtils.isNotEmpty(contractQueryBocFile)) {
            FileUtils.deleteQuietly(new File(fromNode.getTonDbDir() + contractQueryBocFile));
        }
        log.info("wallet contract installed in blockchain, wallet {}", Address.of(walletAddress).toString(false));
        return true;
    }

    boolean walletHasEnoughFunds(Node fromNode, WalletAddress walletAddress, BigDecimal amount) throws Exception {
        Thread.sleep(1000);
        LiteClientAccountState accountState = LiteClientParser.parseGetAccount(liteClient.executeGetAccount(fromNode, walletAddress.getFullWalletAddress()));
        if (isNull(accountState) || isNull(accountState.getBalance())) {
            log.warn("walletHasEnoughFunds NO {}", accountState);
            return false;
        }
        log.info("wallet has enough funds, wallet {}, balance {}", walletAddress.getFullWalletAddress(), String.format("%,.9f", accountState.getBalance().getToncoins()));
        return (accountState.getBalance().getToncoins().compareTo(amount.multiply(BLN1)) > 0);
    }

    public void installWalletSmartContract(Node fromNode, WalletAddress walletAddress) throws Exception {
        log.info("installing wallet smart-contract {}", walletAddress.getFullWalletAddress());
        //check if money arrived
        while (!walletHasEnoughFunds(fromNode, walletAddress, BigDecimal.valueOf(MINIMUM_TONCOINS))) ;

        String resultSendBoc;
        // installing state-init
        if (nonNull(walletAddress.getWalletQueryFileBocLocation())) {
            resultSendBoc = liteClient.executeSendfile(fromNode, walletAddress.getWalletQueryFileBocLocation());
            log.debug(resultSendBoc);
        } else {
            tonlib.sendRawMessage(walletAddress.getInitExternalMessage().message.toBocBase64(false));
        }

        while (!walletHasContractInstalled(liteClient, fromNode, Address.of(walletAddress.getFullWalletAddress()), ""))
            ;
    }

    /**
     * Used to send toncoins from one-time-wallet, where do we have prvkey, which is used in fift script
     */

    public Boolean sendTonCoins(SendToncoinsParam sendToncoinsParam) throws Exception {

        long seqno = liteClient.executeGetSeqno(sendToncoinsParam.getExecutionNode(), sendToncoinsParam.getFromWallet().getFullWalletAddress());
//        long seqno = MyLocalTon.getInstance().getSeqno(sendToncoinsParam.getFromWallet().getFullWalletAddress());

        log.debug("getSeqNoAndSendTonCoins(), source wallet {}, version {}, seqno {}, amount {}, dest {}",
                sendToncoinsParam.getFromWallet().getFullWalletAddress(),
                sendToncoinsParam.getFromWalletVersion().getValue(),
                seqno,
                sendToncoinsParam.getToncoinsAmount(),
                sendToncoinsParam.getDestAddr());

        if (seqno == -1L) {
            log.error("Error retrieving seqno from contract {}", sendToncoinsParam.getFromWallet().getFullWalletAddress());
            return false;
        }

        WalletContract contract = null;

        if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R2)) {
            Options options = Options.builder()
                    .publicKey(Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPublicKeyHex()))
                    .walletId(sendToncoinsParam.getFromSubWalletId())
                    .build();
            org.ton.java.smartcontract.wallet.Wallet wallet = new org.ton.java.smartcontract.wallet.Wallet(WalletVersion.V1R2, options);
            contract = wallet.create();
            ((WalletV1ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), Address.of(sendToncoinsParam.getDestAddr()), sendToncoinsParam.getAmount(), sendToncoinsParam.getComment());
        } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V1R3)) {
            Options options = Options.builder()
                    .publicKey(Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPublicKeyHex()))
                    .walletId(sendToncoinsParam.getFromSubWalletId())
                    .build();
            org.ton.java.smartcontract.wallet.Wallet wallet = new org.ton.java.smartcontract.wallet.Wallet(WalletVersion.V1R3, options);
            contract = wallet.create();
            ((WalletV1ContractR3) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), Address.of(sendToncoinsParam.getDestAddr()), sendToncoinsParam.getAmount(), sendToncoinsParam.getComment());
        } else if (sendToncoinsParam.getFromWalletVersion().equals(WalletVersion.V3R2)) {
            Options options = Options.builder()
                    .publicKey(Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPublicKeyHex()))
                    .walletId(sendToncoinsParam.getFromSubWalletId())
                    .build();
            org.ton.java.smartcontract.wallet.Wallet wallet = new org.ton.java.smartcontract.wallet.Wallet(WalletVersion.V3R2, options);
            contract = wallet.create();
            ((WalletV3ContractR2) contract).sendTonCoins(tonlib, Utils.hexToBytes(sendToncoinsParam.getFromWallet().getPrivateKeyHex()), Address.of(sendToncoinsParam.getDestAddr()), sendToncoinsParam.getAmount(), sendToncoinsParam.getComment());
        } else { // send using fift from master and config wallets using base-file
            String externalMsgLocation = new Fift().prepareSendTonCoinsFromNodeWallet(sendToncoinsParam, seqno);
            if (isNull(externalMsgLocation)) {
                return false;
            }
            log.debug(liteClient.executeSendfile(sendToncoinsParam.getExecutionNode(), externalMsgLocation));
        }

        //FileUtils.deleteQuietly(new File(tempBocFileAbsolutePath)); // sure ?

        log.info("Sent {} Toncoins by {} from {} to {}",
                sendToncoinsParam.getToncoinsAmount(),
                sendToncoinsParam.getExecutionNode().getNodeName(),
                sendToncoinsParam.getFromWallet().getFullWalletAddress(),
                Address.of(sendToncoinsParam.getDestAddr()).toString(false));

        int counter = 0;
        while (true) {
            Thread.sleep(3 * 1000);
            long newSeqno;
            if (isNull(contract)) {
                newSeqno = liteClient.executeGetSeqno(sendToncoinsParam.getExecutionNode(), sendToncoinsParam.getFromWallet().getFullWalletAddress());
            } else {
                newSeqno = contract.getSeqno(tonlib);
            }
            if (newSeqno > seqno) {
                return true;
            }
            log.info("{} waiting for wallet to update seqno. Old seqno {}, new seqno {}", sendToncoinsParam.getExecutionNode().getNodeName(), seqno, newSeqno);
            counter++;
            if ((counter % 10) == 0) {
                // todo resend
//                log.info("resending external message {}", externalMsgLocation);
//                log.debug(liteClient.executeSendfile(sendToncoinsParam.getExecutionNode(), externalMsgLocation));
            }
            if (counter > 30) {
                log.error("ERROR sending {} Toncoins by {} from {} to {}.",
                        sendToncoinsParam.getToncoinsAmount(),
                        sendToncoinsParam.getExecutionNode().getNodeName(),
                        sendToncoinsParam.getFromWallet().getFullWalletAddress(),
                        sendToncoinsParam.getDestAddr());
                return false;
            }
        }
    }

    public WalletAddress createWalletByVersion(WalletVersion walletVersion, long workchainId, long walletId) throws Exception {

        List<String> mnemonic = Mnemonic.generate(12, "");
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
