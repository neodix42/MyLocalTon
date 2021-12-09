package org.ton.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.executors.liteclient.api.ResultLastBlock;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@RunWith(JUnit4.class)
public class BlockIdTest {

    private static final String SHARD = "80000000";
    private static final BigInteger BLOCK_SEQNO = new BigInteger("100000000");
    private static final Long WC = -1L;
    private static ResultLastBlock resultLastBlock;

    @BeforeClass
    public static void executedBeforeEach() {
        resultLastBlock = ResultLastBlock.builder().wc(WC).shard(SHARD).seqno(BLOCK_SEQNO).build();
    }

    @Test
    public void getShortBlockSeqno() {
        assertEquals(String.format("(%d,%s,%d)", resultLastBlock.getWc(), resultLastBlock.getShard(), resultLastBlock.getSeqno())
                , String.format("(%d,%s,%d)", WC, SHARD, BLOCK_SEQNO));
    }

    @Test
    public void getPureBlockSeqno() {
        assertEquals(BLOCK_SEQNO, resultLastBlock.getSeqno());
    }

    @Test
    public void getWc() {
        assertEquals(WC, resultLastBlock.getWc());
    }

    @Test
    public void getShard() {
        assertEquals(SHARD, resultLastBlock.getShard());
    }
}