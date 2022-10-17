package org.ton.wallet;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.ton.enums.LiteClientEnum;
import org.ton.executors.fift.Fift;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.parameters.SendToncoinsParam;
import org.ton.settings.Node;

import java.io.File;
import java.math.BigDecimal;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class Wallet {

    private final LiteClient liteClient;

    public Wallet() {
        liteClient = LiteClient.getInstance(LiteClientEnum.GLOBAL);
    }

    public static final BigDecimal BLN1 = BigDecimal.valueOf(1000000000);

    static final double MINIMUM_TONCOINS = 0.09;

    boolean walletHasStateInit(AccountState accountState) {
        return nonNull(accountState.getStatus()); // has stateInit with some toncoins
    }

    public boolean walletHasContractInstalled(Node fromNode, WalletAddress walletAddress, String contractQueryBocFile) throws Exception {
        AccountState accountState;

        do {
            Thread.sleep(2000);
            accountState = LiteClientParser.parseGetAccount(liteClient.executeGetAccount(fromNode, walletAddress.getFullWalletAddress()));
        } while (isNull(accountState) || (accountState.getStatus().equals("Uninitialized")));

        FileUtils.deleteQuietly(new File(fromNode.getTonDbDir() + contractQueryBocFile));
        log.debug("wallet contract installed, wallet {}, status {}", walletAddress.getFullWalletAddress(), accountState.getStatus());
        return true;
    }

    boolean walletHasEnoughFunds(Node fromNode, WalletAddress walletAddress, BigDecimal amount) throws Exception {
        Thread.sleep(1000);
        AccountState accountState = LiteClientParser.parseGetAccount(liteClient.executeGetAccount(fromNode, walletAddress.getFullWalletAddress()));
        if (isNull(accountState.getBalance())) {
            return false;
        }
        log.debug("wallet has enough funds, wallet {}", walletAddress.getFullWalletAddress());
        return accountState.getBalance().getToncoins().compareTo(amount.multiply(BLN1)) > 0;
    }

    public void installWalletSmartContract(Node fromNode, WalletAddress walletAddress) throws Exception {
        log.debug("installing wallet smart-contract {}", walletAddress.getFullWalletAddress());
        //check if money arrived
        while (!walletHasEnoughFunds(fromNode, walletAddress, BigDecimal.valueOf(MINIMUM_TONCOINS))) ;

        // installing state-init
        String resultSendBoc = liteClient.executeSendfile(fromNode, walletAddress.getWalletQueryFileBocLocation());
        log.debug(resultSendBoc);

        while (!walletHasContractInstalled(fromNode, walletAddress, resultSendBoc)) ;

    }

    /**
     * Used to send toncoins from one-time-wallet, where do we have prvkey, which is used in fift script
     */
    public Boolean sendTonCoins(SendToncoinsParam sendToncoinsParam) throws Exception {

        long seqno = liteClient.executeGetSeqno(sendToncoinsParam.getExecutionNode(), sendToncoinsParam.getFromWallet().getFullWalletAddress());

        log.debug("getSeqNoAndSendTonCoins(), source wallet {}, version {}, seqno {}, amount {}, dest {}",
                sendToncoinsParam.getFromWallet().getFullWalletAddress(),
                sendToncoinsParam.getFromWalletVersion().getValue(),
                seqno,
                sendToncoinsParam.getAmount(),
                sendToncoinsParam.getDestAddr());

        if (seqno == -1L) {
            log.error("Error retrieving seqno from contract {}", sendToncoinsParam.getFromWallet().getFullWalletAddress());
            return false;
        }

        String externalMsgLocation = new Fift().prepareSendTonCoinsFromNodeWallet(sendToncoinsParam, seqno);

        if (isNull(externalMsgLocation)) {
            return false;
        }

        log.debug(liteClient.executeSendfile(sendToncoinsParam.getExecutionNode(), externalMsgLocation));

        //FileUtils.deleteQuietly(new File(tempBocFileAbsolutePath)); // sure ?

        log.info("Sent {} Toncoins by {} from {} to {}",
                sendToncoinsParam.getAmount(),
                sendToncoinsParam.getExecutionNode().getNodeName(),
                sendToncoinsParam.getFromWallet().getFullWalletAddress(),
                sendToncoinsParam.getDestAddr());

        int counter = 0;
        while (true) {
            Thread.sleep(3 * 1000);
            long newSeqno = liteClient.executeGetSeqno(sendToncoinsParam.getExecutionNode(), sendToncoinsParam.getFromWallet().getFullWalletAddress());
            if (newSeqno > seqno) {
                return true;
            }
            log.info("{} waiting for wallet to update seqno. Old seqno {}, new seqno {}", sendToncoinsParam.getExecutionNode().getNodeName(), seqno, newSeqno);
            counter++;
            if ((counter % 10) == 0) {
                log.info("resending external message {}", externalMsgLocation);
                log.debug(liteClient.executeSendfile(sendToncoinsParam.getExecutionNode(), externalMsgLocation));
            }
            if (counter > 30) {
                log.error("ERROR sending {} Toncoins by {} from {} to {}.",
                        sendToncoinsParam.getAmount(),
                        sendToncoinsParam.getExecutionNode().getNodeName(),
                        sendToncoinsParam.getFromWallet().getFullWalletAddress(),
                        sendToncoinsParam.getDestAddr());
                return false;
            }
        }
    }

    public WalletAddress createWallet(Node node, WalletVersion version, long workchain, long subWalletId) throws Exception {
        WalletAddress walletAddress;
        switch (version) {
            case V1:
                walletAddress = new Fift().createWalletV1QueryBoc(node, workchain);
                log.debug("wallet created {}", walletAddress);
                return walletAddress;
            case V2:
                walletAddress = new Fift().createWalletV2QueryBoc(node, workchain);
                log.debug("wallet created {}", walletAddress);
                return walletAddress;
            default:
                walletAddress = new Fift().createWalletV3QueryBoc(node, workchain, subWalletId);
                log.debug("wallet created {}", walletAddress);
                return walletAddress;
        }
    }
}
