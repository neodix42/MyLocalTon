package org.ton.db;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.db.entities.BlockEntity;
import org.ton.executors.liteclient.api.block.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RunWith(JUnit4.class)
public class SerializationTest {

    @Test
    public void testPeformance() throws IOException {

        String uuid = UUID.randomUUID().toString();

        log.info("start");
        for (long i = 0; i < 1000; i++) {
            BlockEntity block = BlockEntity.builder()
                    .createdAt(i)
                    .wc(-i)
                    .seqno(UUID.randomUUID().toString())
                    .shard(UUID.randomUUID().toString())
                    .filehash(UUID.randomUUID().toString())
                    .roothash(UUID.randomUUID().toString())
                    .block(Block.builder()
                            .globalId(25L)
                            .valueFlow(ValueFlow.builder()
                                    .created(Value.builder()
                                            .toncoins(BigDecimal.TEN)
                                            .build())
                                    .recovered(Value.builder()
                                            .toncoins(BigDecimal.TEN)
                                            .build())
                                    .build())
                            .extra(Extra.builder()
                                    .accountBlock(AccountBlock.builder()
                                            .transactions(List.of(Transaction.builder()
                                                    .endStatus("endStatus")
                                                    .now(56464L)
                                                    .accountAddr("234234321341234123412334123412341234123412341234")
                                                    .newHash("2q3412341234SDFSDF#$@#$2")
                                                    .origStatus("origStatus")
                                                    .inMsg(Message.builder()
                                                            .destAddr(Address.builder()
                                                                    .addr("234123412341234124123412341234123412341234234534563456")
                                                                    .wc(1L)
                                                                    .build())
                                                            .srcAddr(Address.builder()
                                                                    .addr("234123412341234124123412341234123412341234234534563456")
                                                                    .wc(1L)
                                                                    .build())
                                                            .bounced((byte) 1)
                                                            .createdAt(1145L)
                                                            .init(Init.builder()
                                                                    .build())
                                                            .build())
                                                    .build()))
                                            .build()).build())
                            .build())
                    .build();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(block);
            out.flush();
            //log.info("size {}", bos.toByteArray().length);
        }
        log.info("finish");
        //log.info("size {}", bos.toByteArray().length);

    }
}
