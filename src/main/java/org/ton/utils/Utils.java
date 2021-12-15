package org.ton.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.jutils.jprocesses.JProcesses;
import org.reactfx.collection.ListModification;
import org.slf4j.LoggerFactory;
import org.ton.actions.MyLocalTon;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.executors.liteclient.api.ResultLastBlock;
import org.ton.main.Main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.sun.javafx.PlatformUtil.isWindows;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.ton.executors.liteclient.LiteClientParser.*;

@Slf4j
public class Utils {
    public static final String WALLET_V1_CODE = "FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54";
    public static final String WALLET_V2_CODE = "FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54";
    public static final String WALLET_V3_CODE = "FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54";

    public static final String WALLET_MASTER = "FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F31D307D4D101FB00A4C8CB1FCBFFC9ED54";
    public static final String WALLET_CONFIG = "FF00F4A413F4BCF2C80B";

    public static final String[] KEYWORDS = new String[]{
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws",
            "transient", "try", "void", "volatile", "while"
    };

    public static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    public static final String PAREN_PATTERN = "\\(|\\)";
    public static final String BRACE_PATTERN = "\\{|\\}";
    public static final String BRACKET_PATTERN = "\\[|\\]";
    public static final String SEMICOLON_PATTERN = "\\;";
    public static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    public static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/"   // for whole text processing (text blocks)
            + "|" + "/\\*[^\\v]*" + "|" + "^\\h*\\*([^\\v]*|/)";  // for visible paragraph processing (line by line)

    public static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );

    public static String toUTC(long timestamp) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss").format(LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC));
    }

    public static String toLocal(long timestamp) {
        return DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp * 1000), ZoneId.systemDefault()));
    }

    public static String toUtcNoSpace(long timestamp) {
        return DateTimeFormatter.ofPattern("yyyy.MM.dd_HH-mm-ss").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()));
    }

    public static void setMyLocalTonLogLevel(String myLocalTonLogLevel) {
        Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        switch (myLocalTonLogLevel) {
            case "INFO":
                root.setLevel(Level.INFO);
                break;
            case "DEBUG":
                root.setLevel(Level.DEBUG);
                break;
            case "ERROR":
                root.setLevel(Level.ERROR);
                break;
            default:
                root.setLevel(Level.INFO);
        }
    }

    public static String getTonLogLevel(String tonLogLevel) {

        switch (tonLogLevel) {
            case "INFO":
                return "3";
            case "WARNING":
                return "2";
            case "DEBUG":
                return "4";
            case "ERROR":
                return "1";
            case "FATAL":
                return "0";
            default:
                return "0";
        }
    }

    public static Pair<String, Long> detectWalledVersionAndId(AccountState accountState) {
        List<String> accountData = accountState.getStateData();
        List<String> accountCode = accountState.getStateCode();
        String walletVersion = null;
        long walletId;

        if (nonNull(accountCode)) {
            for (String codeLine : accountCode) {
                if (codeLine.contains(WALLET_V3_CODE)) {
                    walletVersion = "V3";
                    break;
                }
                if (codeLine.contains(WALLET_V2_CODE)) {
                    walletVersion = "V2";
                    break;
                }
                if (codeLine.contains(WALLET_V1_CODE)) {
                    walletVersion = "V1";
                    break;
                }
                if (codeLine.contains(WALLET_MASTER)) {
                    walletVersion = "MASTER";
                    break;
                }
                if (codeLine.contains(WALLET_CONFIG)) {
                    walletVersion = "CONFIG";
                    break;
                }
            }
        }

        if (nonNull(walletVersion) && walletVersion.equals("V3")) { // TODO might be more wallets with walletId, e.g. highload-wallet
            try {
                walletId = Long.parseLong(accountData.get(0).substring(8, 16));
            } catch (Exception e) {
                walletId = -1L;
            }
        } else {
            walletId = -1L;
        }

        return Pair.of(walletVersion, walletId);
    }

    //4 - debug, 3- info, 2 - warning, 1 - error, 0 - fatal

    public void showThreads() {

        int noThreads = Thread.activeCount();
        if (noThreads == 0) {
            log.info("Threads 0");
            return;
        }
        Thread[] lstThreads = new Thread[noThreads];
        Thread.enumerate(lstThreads);
        for (int i = 0; i < noThreads; i++) {
            log.info("Thread No: {}, {}", i, Objects.isNull(lstThreads[i]) ? "null" : lstThreads[i].getName());
        }
    }

    public static long datetimeToTimestamp(String datetime) {

        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = formatter.parse(datetime);
            return date.getTime() / 1000;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static class VisibleParagraphStyler<PS, SEG, S> implements Consumer<ListModification<? extends Paragraph<PS, SEG, S>>> {
        private final GenericStyledArea<PS, SEG, S> area;
        private final Function<String, StyleSpans<S>> computeStyles;
        private int prevParagraph, prevTextLength;

        public VisibleParagraphStyler(GenericStyledArea<PS, SEG, S> area, Function<String, StyleSpans<S>> computeStyles) {
            this.computeStyles = computeStyles;
            this.area = area;
        }

        @Override
        public void accept(ListModification<? extends Paragraph<PS, SEG, S>> lm) {
            if (lm.getAddedSize() > 0) {
                int paragraph = Math.min(area.firstVisibleParToAllParIndex() + lm.getFrom(), area.getParagraphs().size() - 1);
                String text = area.getText(paragraph, 0, paragraph, area.getParagraphLength(paragraph));

                if (paragraph != prevParagraph || text.length() != prevTextLength) {
                    int startPos = area.getAbsolutePosition(paragraph, 0);
                    Platform.runLater(() -> area.setStyleSpans(startPos, computeStyles.apply(text)));
                    prevTextLength = text.length();
                    prevParagraph = paragraph;
                }
            }
        }
    }

    public static Long parseLong(String l) {
        try {
            return Long.parseLong(l);
        } catch (Exception e) {
            return -2L;
        }
    }

    public static String friendlyAddrToHex(String base64Addr) {
        byte[] data = Base64.decodeBase64(base64Addr);

        if (data.length != 36) return "";

        byte[] addr = Arrays.copyOfRange(data, 0, 34);

        int workchain;
        if (addr[1] == (byte) 0xff) {
            workchain = -1;
        } else {
            workchain = addr[1];
        }

        byte[] hashPart = Arrays.copyOfRange(addr, 2, 34);

        return workchain + ":" + Hex.encodeHexString(hashPart).toUpperCase();
    }

    public static String getMyPath() {
        String result = "";
        try {
            CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            result = jarFile.getParentFile().getPath() + File.separator + "MyLocalTon.jar";
        } catch (Exception e) {
            return "";
        }
        return result;
    }

    public static boolean doShutdown() {
        try {
            if (Main.appActive.get()) {
                log.debug("do shutdown");
                Main.appActive.set(false);
                Main.fileLock.release();
                Main.randomAccessFile.close();
                Main.file.delete();
                log.info("Destroying external processes...");

                MyLocalTon.getInstance().getDhtServerProcess().destroy();

                Thread.sleep(1000);

                Runtime rt = Runtime.getRuntime();
                if (isWindows()) {
                    rt.exec("taskkill /F /IM " + "lite-client.exe");
                    log.debug("SendSignalCtrlC64.exe {}", MyLocalTon.getInstance().getGenesisValidatorProcess().pid());
                    rt.exec("myLocalTon/genesis/bin/SendSignalCtrlC64.exe " + MyLocalTon.getInstance().getGenesisValidatorProcess().pid());
                    Thread.sleep(4000);
                    //double knockout
                    log.debug("SendSignalCtrlC64.exe {}", MyLocalTon.getInstance().getGenesisValidatorProcess().pid());
                    rt.exec("myLocalTon/genesis/bin/SendSignalCtrlC64.exe " + MyLocalTon.getInstance().getGenesisValidatorProcess().pid());

                    // triple knockout
                    String resultInput = "";
                    do {
                        Process proc = Runtime.getRuntime().exec("tasklist.exe /FI \"IMAGENAME eq validator-engine.exe\"");
                        InputStream procOutput = proc.getInputStream();

                        if (proc.waitFor() == 0) {
                            resultInput = IOUtils.toString(procOutput, Charset.defaultCharset());
                            log.debug(resultInput);
                            if (resultInput.contains("validator-engine")) {
                                int pid = Integer.parseInt(StringUtils.substringBetween(resultInput, "validator-engine.exe", "Console").trim());
                                boolean success = JProcesses.killProcess(pid).isSuccess();
                                log.debug("validator-engine with pid " + pid + " killed " + success);

                            }
                        }
                    } while (resultInput.contains("validator-engine"));
                } else {
                    rt.exec("killall -9 " + "lite-client");
                    rt.exec("killall -2 " + "validator-engine");
                }

                log.debug("Waiting for processes to be killed...");
                Thread.sleep(3000);
                return true;
            } else {
                return true;
            }
        } catch (Exception e) {
            log.error("Unable to shutdown gracefully, error: {}", e.getMessage());
            return false;
        }
    }

    public static ResultLastBlock fullBlockSeqno2Result(String fullBlockSeqno) {
        String shortBlockSeqno = OPEN + sb(fullBlockSeqno, OPEN, CLOSE) + CLOSE;
        String rootHashId = sb(fullBlockSeqno, ":", ":");
        String fileHashId = fullBlockSeqno.substring(fullBlockSeqno.lastIndexOf(':') + 1);
        String shard = sb(shortBlockSeqno, ",", ",");
        String pureBlockSeqno = sb(shortBlockSeqno, shard + ",", CLOSE);
        Long wc = Long.parseLong(sb(shortBlockSeqno, OPEN, ","));

        return ResultLastBlock.builder()
                .seqno(pureBlockSeqno)
                .wc(wc)
                .shard(shard)
                .fileHash(fileHashId)
                .rootHash(rootHashId)
                .build();
    }

    public static boolean isMacOsArm() {
        try {
            Process proc = Runtime.getRuntime().exec("arch");
            InputStream procOutput = proc.getInputStream();

            if (proc.waitFor() == 0) {
                String resultInput = IOUtils.toString(procOutput, Charset.defaultCharset());
                log.debug("isMacOsArm: {}", resultInput);
                return resultInput.contains("arm");
            }
        } catch (Exception e) {
            log.error("isMacOsArm error: {}", e.getMessage());
            return false;
        }
        return false;
    }

    public static String getUbuntuVersion() {
        try {
            Process proc = Runtime.getRuntime().exec("lsb_release -a");
            InputStream procOutput = proc.getInputStream();

            if (proc.waitFor() == 0) {
                String resultInput = IOUtils.toString(procOutput, Charset.defaultCharset());
                log.debug("getUbuntuVersion: {}", resultInput);
                if (resultInput.contains("20.04")) {
                    return "20.04";
                } else if (resultInput.contains("18.04")) {
                    return "18.04";
                } else {
                    return "";
                }
            }
        } catch (Exception e) {
            log.error("getUbuntuVersion error: {}", e.getMessage());
            return "";
        }
        return "";
    }

    public static String constructFullBlockSeq(Long wc, String shard, String seqno, String rootHash, String fileHash) {
        return String.format("(%d,%s,%s):%s:%s", wc, shard, seqno, rootHash, fileHash);
    }

    /**
     * Finds matching closing bracket in string starting with pattern
     */
    private static int findPosOfClosingBracket(String str, String pattern) {
        int i;
        int index = str.indexOf(pattern) + pattern.indexOf("[");

        ArrayDeque<Integer> st = new ArrayDeque<>();

        for (i = index; i < str.length(); i++) {
            if (str.charAt(i) == '[') {
                st.push((int) str.charAt(i));
            } else if (str.charAt(i) == ']') {
                st.pop();
                if (st.isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static String sbb(String str, String pattern) {
        if (isNull(str) || !str.contains(pattern))
            return null;

        int openindex = str.indexOf(pattern) + pattern.indexOf("[");
        int closeIndex = findPosOfClosingBracket(str, pattern);
        return str.substring(openindex, closeIndex + 1).trim();
    }

    public static String convertPubKeyToBase64(String pathToPubkey) throws IOException {
        byte[] liteserverPubkey = Files.readAllBytes(Paths.get(pathToPubkey));
        byte[] removed4bytes = Arrays.copyOfRange(liteserverPubkey, 4, liteserverPubkey.length);
        return Base64.encodeBase64String(removed4bytes);
    }

}
