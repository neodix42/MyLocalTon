package org.ton.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.db.entities.BlockEntity;
import org.ton.db.entities.TxEntity;
import org.ton.db.entities.WalletEntity;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.executors.liteclient.api.StorageInfo;
import org.ton.executors.liteclient.api.block.*;
import org.ton.wallet.WalletAddress;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(JUnit4.class)
public class OrientDbTest {

    @Test
    public void testBlocks() {
        log.info("running test testBlocks");

        //ODatabaseObject db = OrientDB.getDB();

        String randomBlockSeqno = UUID.randomUUID().toString();
        String randomBlockSeqno2 = UUID.randomUUID().toString();
/*
        BlockEntity block = BlockEntity.builder()
                .createdAt(11111111L)
                .wc(-1L)
                .seqno(randomBlockSeqno)
                .shard("80000000000000000")
                .filehash(UUID.randomUUID().toString())
                .block(Block.builder().globalId(25L).build())
                .build();

        OrientDB.insertBlock(block);

        BlockEntity block2 = BlockEntity.builder()
                .createdAt(222222222L)
                .wc(0L)
                .seqno(randomBlockSeqno2)
                .shard("80000000000000000")
                .filehash(UUID.randomUUID().toString())
                .block(Block.builder().globalId(25L).build())
                .build();
        OrientDB.insertBlock(block2);

        log.info("inserted block");
*/
        List<BlockEntity> blocks = OrientDB.getAllBlocks();
        blocks.forEach(s -> log.info("block {}", s.getSeqno()));
        log.info("count {}", blocks.size());

        BlockEntity blockToFind = BlockEntity.builder()
                .createdAt(11111111L)
                .wc(-1L)
                .seqno(randomBlockSeqno)
                .shard("80000000000000000")
                .filehash(UUID.randomUUID().toString())
                .build();

        BlockEntity blockFound = OrientDB.findBlock(blockToFind.getPrimaryKey());
        log.info("found block {}", blockFound);

        OrientDB.updateBlockDump(blockFound.getPrimaryKey(), Block.builder().globalId(27L).build());

        blockFound = OrientDB.findBlock(blockToFind.getPrimaryKey());
        log.info("after update found block {}", blockFound);
        assertThat(blockFound.getBlock().getGlobalId()).isEqualTo(27L);

        OrientDB.deleteBlock(blockFound.getPrimaryKey());

        List<BlockEntity> blocksBefore = OrientDB.loadBlocksBefore(Instant.now().getEpochSecond() + 3000);
        log.info("count blocksBefore {}", blocksBefore.size());

        List<BlockEntity> foundBlocks = OrientDB.searchBlocks("80000000000000000");
        log.info("found blocks {}", foundBlocks.size());
    }

    @Test
    public void testWallets() {
        log.info("running test testBlocks");

        String randomHexAddress = UUID.randomUUID().toString();

        WalletEntity wallet = WalletEntity.builder()
                .walletVersion("V3")
                .createdAt(11111111L)
                .wc(0L)
                .hexAddress(randomHexAddress)
                .wallet(WalletAddress.builder()
                        .subWalletId(25L)
                        .bounceableAddressBase64url("bounceable")
                        .nonBounceableAddressBase64("nonbounceable")
                        .hexWalletAddress("hex")
                        .wc(0L)
                        .build())
                .mainWalletInstalled(false)
                .configWalletInstalled(true)
                .accountState(AccountState.builder()
                        .address("myaddress")
                        .wc(0L)
                        .balance(Value.builder().toncoins(BigDecimal.TEN).build())
                        .lastTxLt("11")
                        .storageInfo(StorageInfo.builder().usedBits(33L).build())
                        .status("FROZEN")
                        .stateCode(List.of("Code1", "Code2"))
                        .build())
                .subWalletId(25L)
                .preinstalled(true)
                .build();

        OrientDB.insertWallet(wallet);

        List<WalletEntity> wallets = OrientDB.getAllWallets();
        wallets.forEach(s -> log.info("wallet {}, version {}", s.getHexAddress(), s.getWalletVersion()));
        log.info("count {}", wallets.size());

        WalletEntity foundWallet = OrientDB.findWallet(wallet.getPrimaryKey());
        log.info("found wallet {}", foundWallet);

        AccountState newAccountState = AccountState.builder()
                .address("myaddress")
                .wc(0L)
                .status("NEW")
                .stateCode(List.of("New Code 1", "New Code 2"))
                .stateData(List.of("New Data 1", "New Data 2"))
                .balance(Value.builder().toncoins(BigDecimal.ONE).build())
                .build();

        OrientDB.updateWalletState(wallet, newAccountState);

        foundWallet = OrientDB.findWallet(wallet.getPrimaryKey());
        log.info("found updated wallet {}", foundWallet);
        assertThat(foundWallet.getAccountState().getStatus()).isEqualTo("NEW");

        log.info("config wallet {}", OrientDB.existsConfigWallet());
        log.info("main wallet {}", OrientDB.existsMainWallet());
        OrientDB.deleteWallet(wallet.getPrimaryKey());

        wallets = OrientDB.getAllWallets();
        wallets.forEach(s -> log.info("wallet {}", s.getHexAddress()));
        log.info("count {}", wallets.size());
    }

    @Test
    public void testTx() {

        TxEntity txEntity = TxEntity.builder()
                .seqno("12")
                .wc(0L)
                .shard("8000000000000")
                .txHash(UUID.randomUUID().toString())
                .typeTx("Ordinary")
                .typeMsg("Tick")
                .txLt("123")
                .to(Address.builder().addr("toAddr").wc(0L).build())
                .from(Address.builder().addr("fromAddr").wc(0L).build())
                .accountAddress("accAddr")
                .amount(BigDecimal.TEN)
                .createdAt(11111111L)
                .message(Message.builder().body(Body.builder()
                                .cells(List.of("cell1")).build())
                        .bounce((byte) 1)
                        .destAddr(Address.builder().addr("toAddr")
                                .build()).build())
                .tx(Transaction.builder().accountAddr("accAddr").endStatus("endStatus").inMsg(Message.builder().build()).build())
                .build();

        OrientDB.insertTx(txEntity);

        List<TxEntity> txs = OrientDB.getAllTxs();
        txs.forEach(t -> log.info("wallet {}", (t.getFrom())));
        log.info("count {}", txs.size());

        TxEntity foundTx = OrientDB.findTx(txEntity.getPrimaryKey());
        log.info("found tx {}", foundTx);

        List<TxEntity> txsBefore = OrientDB.loadTxsBefore(Instant.now().getEpochSecond() + 3000);
        log.info("count txsBefore {}", txsBefore.size());
    }
}
