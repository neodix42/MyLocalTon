package org.ton.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.settings.GenesisNode;
import org.ton.settings.Node;
import org.ton.utils.Utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(JUnit4.class)
public class UtilsTest {

    @Test
    public void parseFriendlyAddress() {

        String friendlyAddress1 = "kf8YcUS_oeDpMNiK9an7mV1R2drjtcE_a4JjQPLVM0iLBSfA"; // result -1:187144bfa1e0e930d88af5a9fb995d51d9dae3b5c13f6b826340f2d533488b05
        String friendlyAddress2a = "kQDS4GlChkZysJZzp213yNSIKYwAGecO5VVZsF3zQLHE1HcM"; // result -1:187144bfa1e0e930d88af5a9fb995d51d9dae3b5c13f6b826340f2d533488b05
        String friendlyAddress2b = "0QDS4GlChkZysJZzp213yNSIKYwAGecO5VVZsF3zQLHE1CrJ"; // result -1:187144bfa1e0e930d88af5a9fb995d51d9dae3b5c13f6b826340f2d533488b05
        String friendlyAddress3a = "kQB3LaeLjxJUZV97uB5f7e33Tom1TwDie1gEZFj222+vGCrI"; // result -1:187144bfa1e0e930d88af5a9fb995d51d9dae3b5c13f6b826340f2d533488b05
        String friendlyAddress3b = "kQB3LaeLjxJUZV97uB5f7e33Tom1TwDie1gEZFj222-vGCrI"; // result -1:187144bfa1e0e930d88af5a9fb995d51d9dae3b5c13f6b826340f2d533488b05
        String friendlyAddress3c = "0QB3LaeLjxJUZV97uB5f7e33Tom1TwDie1gEZFj222+vGHcN"; // result -1:187144bfa1e0e930d88af5a9fb995d51d9dae3b5c13f6b826340f2d533488b05
        String friendlyAddress3d = "0QB3LaeLjxJUZV97uB5f7e33Tom1TwDie1gEZFj222-vGHcN"; // result -1:187144bfa1e0e930d88af5a9fb995d51d9dae3b5c13f6b826340f2d533488b05

        assertEquals("-1:187144BFA1E0E930D88AF5A9FB995D51D9DAE3B5C13F6B826340F2D533488B05", Utils.friendlyAddrToHex(friendlyAddress1));
        assertEquals("0:D2E06942864672B09673A76D77C8D488298C0019E70EE55559B05DF340B1C4D4", Utils.friendlyAddrToHex(friendlyAddress2a));
        assertEquals("0:D2E06942864672B09673A76D77C8D488298C0019E70EE55559B05DF340B1C4D4", Utils.friendlyAddrToHex(friendlyAddress2b));
        assertEquals("0:772DA78B8F1254655F7BB81E5FEDEDF74E89B54F00E27B58046458F6DB6FAF18", Utils.friendlyAddrToHex(friendlyAddress3a));
        assertEquals("0:772DA78B8F1254655F7BB81E5FEDEDF74E89B54F00E27B58046458F6DB6FAF18", Utils.friendlyAddrToHex(friendlyAddress3b));
        assertEquals("0:772DA78B8F1254655F7BB81E5FEDEDF74E89B54F00E27B58046458F6DB6FAF18", Utils.friendlyAddrToHex(friendlyAddress3c));
        assertEquals("0:772DA78B8F1254655F7BB81E5FEDEDF74E89B54F00E27B58046458F6DB6FAF18", Utils.friendlyAddrToHex(friendlyAddress3d));
    }

    @Test
    public void dateFormatTest() {
        BigDecimal amount = new BigDecimal(2000000);
        System.out.printf("%,.8f%n", amount);

        amount = new BigDecimal(49000000000L);
        System.out.printf("%,.8f%n", amount);

        amount = new BigDecimal(4900L);
        System.out.printf("%,.8f%n", amount);

        amount = new BigDecimal("0.4998465213");
        System.out.printf("%,.8f%n", amount);
    }

    @Test
    public void TestFindClosingBracket() {
        int index = LiteClientParser.findPosOfClosingBracket("Hello ( my world ). Bye (bye).", "llo ( my");
        assertEquals(17, index);

        index = LiteClientParser.findPosOfClosingBracket("Hello ( my world ). Bye (bye).", " (bye");
        assertEquals(28, index);
    }

    @Test
    public void TestSbb() {
        String result = LiteClientParser.sbb("Hello ( my world ). Bye (bye).", " (bye");
        assertEquals("(bye)", result);

        result = LiteClientParser.sbb("Hello ( my world ). Bye (bye).", "llo ( my");
        assertEquals("( my world )", result);
    }

    @Test
    public void TestFindClosingBracketNegative() {
        int index = LiteClientParser.findPosOfClosingBracket("Hello ( my world (). Bye (bye).", "llo ( my");
        // unbalanced brackets
        assertEquals(-1, index);

        index = LiteClientParser.findPosOfClosingBracket("Hello ( my world )). Bye (bye).", "llo ( my");
        log.info("{}", index);
    }

    @Test
    public void TestStringBlocks() {
        List<String> result = LiteClientParser.findStringBlocks("Hello f1:(world=true) nevermind f1:(world=false) dich ( otomy", "f1:(");
        result.forEach(log::info);
        assertEquals(2, result.size());
    }

    @Ignore
    @Test
    public void TestHexConversion() throws IOException, DecoderException {
        Node testNode = new GenesisNode();
        testNode.extractBinaries();
        byte[] validatorPubKey = Files.readAllBytes(Paths.get(testNode.getTonDbKeyringDir() + "validator.pub"));
        byte[] removed4bytes = Arrays.copyOfRange(validatorPubKey, 4, validatorPubKey.length);
        log.info("validatorPubKey {}", validatorPubKey);
        log.info("validatorPubKey {}", Hex.encodeHexString(validatorPubKey));
        log.info("validatorPubKey {}", Hex.decodeHex(Hex.encodeHexString(validatorPubKey)));
        assertTrue(true);
    }

    @Test
    public void TestToUtcNospace() {
        long millis = System.currentTimeMillis();
        log.info("file with datetime {}", Utils.toUtcNoSpace(millis / 1000));
        log.info("file with datetime {}", Utils.toUtcNoSpace(Instant.now().getEpochSecond()));
        log.info(DateTimeFormatter.ofPattern("yyyy.MM.dd_HH:mm:ss").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())));
        log.info("file with datetime {}", Utils.toUTC(Instant.now().getEpochSecond()));
        log.info("file with datetime {}", Utils.toLocal(Instant.now().getEpochSecond()));
        log.info("{}", LocalDateTime.ofEpochSecond(millis / 1000, 0, ZoneOffset.UTC));
        log.info("{}", LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()));
        assertTrue(true);
    }
}
