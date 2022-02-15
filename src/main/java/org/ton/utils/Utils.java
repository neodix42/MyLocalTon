package org.ton.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.jutils.jprocesses.JProcesses;
import org.reactfx.collection.ListModification;
import org.slf4j.LoggerFactory;
import org.ton.actions.MyLocalTon;
import org.ton.db.entities.WalletEntity;
import org.ton.executors.fift.Fift;
import org.ton.executors.liteclient.LiteClient;
import org.ton.executors.liteclient.LiteClientParser;
import org.ton.executors.liteclient.api.*;
import org.ton.main.App;
import org.ton.main.Main;
import org.ton.parameters.SendToncoinsParam;
import org.ton.parameters.ValidationParam;
import org.ton.settings.MyLocalTonSettings;
import org.ton.settings.Node;
import org.ton.ui.controllers.MainController;
import org.ton.wallet.Wallet;
import org.ton.wallet.WalletVersion;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import static org.ton.main.App.fxmlLoader;

@Slf4j
public class Utils {
    public static final String WALLET_V1_CODE = "FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F3120D74A96D307D402FB00DED1A4C8CB1FCBFFC9ED54";
    public static final String WALLET_V2_CODE = "FF0020DD2082014C97BA218201339CBAB19C71B0ED44D0D31FD70BFFE304E0A4F2608308D71820D31FD31F01F823BBF263ED44D0D31FD3FFD15131BAF2A103F901541042F910F2A2F800029320D74A96D307D402FB00E8D1A4C8CB1FCBFFC9ED54";
    public static final String WALLET_V3_CODE = "FF0020DD2082014C97BA218201339CBAB19F71B0ED44D0D31FD31F31D70BFFE304E0A4F2608308D71820D31FD31FD31FF82313BBF263ED44D0D31FD31FD3FFD15132BAF2A15144BAF2A204F901541055F910F2A3F8009320D74A96D307D402FB00E8D101A4C8CB1FCB1FCBFFC9ED54";

    public static final String WALLET_MASTER = "FF0020DD2082014C97BA9730ED44D0D70B1FE0A4F260810200D71820D70B1FED44D0D31FD3FFD15112BAF2A122F901541044F910F2A2F80001D31F31D307D4D101FB00A4C8CB1FCBFFC9ED54";
    public static final String WALLET_CONFIG = "FF00F4A413F4BCF2C80B";

    public static final int YEAR_1971 = 34131600;

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

    public static Pair<WalletVersion, Long> detectWalledVersionAndId(AccountState accountState) {
        List<String> accountData = accountState.getStateData();
        List<String> accountCode = accountState.getStateCode();
        WalletVersion walletVersion = null;
        long walletId;

        if (nonNull(accountCode)) {
            for (String codeLine : accountCode) {
                if (codeLine.contains(WALLET_V3_CODE)) {
                    walletVersion = WalletVersion.V3;
                    break;
                }
                if (codeLine.contains(WALLET_V2_CODE)) {
                    walletVersion = WalletVersion.V2;
                    break;
                }
                if (codeLine.contains(WALLET_V1_CODE)) {
                    walletVersion = WalletVersion.V1;
                    break;
                }
                if (codeLine.contains(WALLET_MASTER)) {
                    walletVersion = WalletVersion.MASTER;
                    break;
                }
                if (codeLine.contains(WALLET_CONFIG)) {
                    walletVersion = WalletVersion.CONFIG;
                    break;
                }
            }
        }

        if (nonNull(walletVersion) && walletVersion.equals(WalletVersion.V3)) { // TODO might be more wallets with walletId, e.g. highload-wallet
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
                log.debug("Do shutdown");
                Main.appActive.set(false);
                App.dbPool.closeDbs();
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
                    rt.exec("killall -2 " + "validator-engine"); // TODO look up for the shutdown order
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
        BigInteger pureBlockSeqno = new BigInteger(sb(shortBlockSeqno, shard + ",", CLOSE));
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
            Process proc = Runtime.getRuntime().exec("sysctl -n machdep.cpu.brand_string");
            InputStream procOutput = proc.getInputStream();

            if (proc.waitFor() == 0) {
                String resultInput = IOUtils.toString(procOutput, Charset.defaultCharset());
                log.info("isMacOsArm: {}", resultInput);
                return resultInput.contains("M1");
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

    public static String constructFullBlockSeq(Long wc, String shard, BigInteger seqno, String rootHash, String fileHash) {
        return String.format("(%d,%s,%d):%s:%s", wc, shard, seqno, rootHash, fileHash);
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

    public static String getLightAddress(String addr) {
        return StringUtils.substring(addr, 0, 6) + ".." + StringUtils.substring(addr, -4);
    }

    public static void replaceOutPortInConfigJson(String path, Integer port) throws IOException {
        String contentConfigJson = Files.readString(Paths.get(path + "config.json"), StandardCharsets.UTF_8);
        String replacedConfigJson = StringUtils.replace(contentConfigJson, "3278", String.valueOf(port));
        Files.writeString(Paths.get(path + "config.json"), replacedConfigJson, StandardOpenOption.CREATE);
    }

    public static int getIntegerIp(String publicIP) throws UnknownHostException {
        InetAddress addr = InetAddress.getByName(publicIP);
        return ByteBuffer.wrap(addr.getAddress()).getInt();
    }

    public static void waitForBlockchainReady(Node node) throws Exception {
        ResultLastBlock lastBlock;
        do {
            Thread.sleep(5000);
            lastBlock = LiteClientParser.parseLast(new LiteClient().executeLast(node));
        } while (isNull(lastBlock) || (lastBlock.getSeqno().compareTo(BigInteger.ONE) < 0));
    }

    public static void waitForNodeSynchronized(Node node) throws Exception {
        ResultLastBlock lastBlock;
        do {
            Thread.sleep(5000);
            lastBlock = LiteClientParser.parseLast(new LiteClient().executeLast(node));
            log.info("{} is out of sync by {} seconds", node.getNodeName(), lastBlock.getSyncedSecondsAgo());
        } while (lastBlock.getSeqno().compareTo(BigInteger.ONE) > 0 && lastBlock.getSyncedSecondsAgo() > 10);
    }

    public static long getCurrentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    public static ValidationParam getConfig(org.ton.settings.Node node) throws Exception {

        ResultConfig12 config12 = LiteClientParser.parseConfig12(new LiteClient().executeBlockchainInfo(node));
        log.info("blockchain was launched at {}", Utils.toLocal(config12.getEnabledSince()));

        long activeElectionId = new LiteClient().executeGetActiveElectionId(node, MyLocalTon.getInstance().getSettings().getElectorSmcAddrHex());
        log.info("active election id {}, {}", activeElectionId, Utils.toLocal(activeElectionId));

        ResultConfig15 config15 = LiteClientParser.parseConfig15(new LiteClient().executeGetElections(node));
        log.info("active elections {}", config15);

        ResultConfig17 config17 = LiteClientParser.parseConfig17(new LiteClient().executeGetMinMaxStake(node));
        log.info("min/max stake {}", config17);

        ResultConfig34 config34 = LiteClientParser.parseConfig34(new LiteClient().executeGetCurrentValidators(node));
        log.info("current validators {}", config34);

        log.info("start work time since {}, until {}", Utils.toLocal(config34.getValidators().getSince()), Utils.toLocal(config34.getValidators().getUntil()));

        ResultConfig32 config32 = LiteClientParser.parseConfig32(new LiteClient().executeGetPreviousValidators(node));
        log.info("previous validators {}", config32);

        ResultConfig36 config36 = LiteClientParser.parseConfig36(new LiteClient().executeGetNextValidators(node));
        log.info("next validators {}", config36);

        ResultConfig0 config0 = LiteClientParser.parseConfig0(new LiteClient().executeGetConfigSmcAddress(node));
        log.info("config address {}", config0.getConfigSmcAddr());

        ResultConfig1 config1 = LiteClientParser.parseConfig1(new LiteClient().executeGetElectorSmcAddress(node));
        log.info("elector address {}", config1.getElectorSmcAddress());

        ResultConfig2 config2 = LiteClientParser.parseConfig2(new LiteClient().executeGetMinterSmcAddress(node));
        log.info("minter address {}", config2.getMinterSmcAddress());

        List<ResultListParticipants> participants = LiteClientParser.parseRunMethodParticipantList(new LiteClient().executeGetParticipantList(node, config1.getElectorSmcAddress()));
        log.info("participants {}", participants);

        return ValidationParam.builder()
                .totalNodes(1L)
                .validatorNodes(config34.getValidators().getTotal())
                .blockchainLaunchTime(config12.getEnabledSince())
                .startValidationCycle(activeElectionId) // same as config34.getValidators().getSince()
                .endValidationCycle(activeElectionId + config15.getValidatorsElectedFor())
                .startElections(activeElectionId - config15.getElectionsStartBefore())
                .endElections(activeElectionId - config15.getElectionsEndBefore())
                .startElectionsBefore(config15.getElectionsStartBefore())
                .endElectionsBefore(config15.getElectionsEndBefore())
                .nextElections(activeElectionId - config15.getElectionsStartBefore() + config15.getValidatorsElectedFor())
                .electionDuration(config15.getElectionsStartBefore() - config15.getElectionsEndBefore())
                .validationDuration(config15.getValidatorsElectedFor())
                .holdPeriod(config15.getStakeHeldFor())
                .minStake(config17.getMinStake())
                .maxStake(config17.getMaxStake())
                .configAddr(config0.getConfigSmcAddr())
                .electorAddr(config1.getElectorSmcAddress())
                .minterAddr(config2.getMinterSmcAddress())
                .participants(participants)
                .previousValidators(config32.getValidators().getValidators())
                .build();
    }

    public static void participate(Node node, ValidationParam v) {

        try {
            long electionId = v.getStartValidationCycle();
            MyLocalTonSettings settings = MyLocalTon.getInstance().getSettings();

            if (node.getValidationParticipated()) {
                log.info("{} has already sent request for elections", node.getNodeName());
                return;
            }

            // if it's a genesis node it has a wallet already - main-wallet.pk
            if (isNull(node.getWalletAddress())) {
                log.info("creating validator controlling smart-contract (wallet) for node {}", node.getNodeName());
                WalletEntity walletEntity = MyLocalTon.getInstance().createWalletEntity(node, null, -1L, settings.getWalletSettings().getDefaultSubWalletId(), 30005L);
                node.setWalletAddress(walletEntity.getWallet());
                Thread.sleep(5 * 1000); //10 sec
            } else {
                log.info("{} no need to create controlling smart-contract (wallet)", node.getNodeName());
            }

            //can be done only once
            MyLocalTon.getInstance().createValidatorPubKeyAndAdnlAddress(node, electionId);

            log.info("{} with wallet {} wants to participate in elections {} ({})", node.getNodeName(), node.getWalletAddress().getBounceableAddressBase64url(), electionId, Utils.toLocal(electionId));

            Fift fift = new Fift();
            String signature = fift.createValidatorElectionRequest(node, electionId, settings.getBlockchainSettings().getMaxFactor());
            fift.signValidatorElectionRequest(node, electionId, settings.getBlockchainSettings().getMaxFactor(), signature);

            MyLocalTon.getInstance().saveSettingsToGson();

            // send stake and validator-query.boc to elector
            SendToncoinsParam sendToncoinsParam = SendToncoinsParam.builder()
                    .executionNode(node)
                    .fromWallet(node.getWalletAddress())
                    .fromWalletVersion(WalletVersion.V3) // todo wallet version
                    .fromSubWalletId(settings.getWalletSettings().getDefaultSubWalletId())
                    .destAddr(settings.getElectorSmcAddrHex())
                    .amount(BigDecimal.valueOf(10004L))
                    // .comment("validator-request")
                    .bocLocation(node.getTonBinDir() + "validator-query.boc")
                    .build();

            new Wallet().sendTonCoins(sendToncoinsParam);

            node.setValidationParticipated(true);

            settings.saveSettingsToGson(settings);
        } catch (Exception e) {
            log.error("Error participating in elections! Error {}", e.getMessage());
            log.error(ExceptionUtils.getStackTrace(e));
        }
    }

    private static void colorValidationTiming(ValidationParam v, MainController c) {

        long currentTime = System.currentTimeMillis() / 1000;

        if (v.getStartValidationCycle() > currentTime) {
            c.startCycle.setTextFill(Color.GREEN);
        } else {
            c.startCycle.setTextFill(Color.BLACK);
        }

        if (v.getEndValidationCycle() > currentTime) {
            c.endCycle.setTextFill(Color.GREEN);
        } else {
            c.endCycle.setTextFill(Color.BLACK);
        }

        if (v.getStartElections() > currentTime) {
            c.startElections.setTextFill(Color.GREEN);
        } else {
            c.startElections.setTextFill(Color.BLACK);
        }

        if (v.getEndElections() > currentTime) {
            c.endElections.setTextFill(Color.GREEN);
        } else {
            c.endElections.setTextFill(Color.BLACK);
        }

        if (v.getNextElections() > currentTime) {
            c.nextElections.setTextFill(Color.GREEN);
        } else {
            c.nextElections.setTextFill(Color.BLACK);
        }
    }

    public static void updateValidationTabGUI(ValidationParam v) {

        Platform.runLater(() -> {
            try {

                MainController c = fxmlLoader.getController();

                c.totalValidators.setText(v.getValidatorNodes().toString());
                c.blockchainLaunched.setText(Utils.toLocal(v.getBlockchainLaunchTime()));
                c.blockchainLaunched.setText(Utils.toLocal(v.getBlockchainLaunchTime()));

                Long startValidationCycle = v.getStartValidationCycle();

                if (startValidationCycle < YEAR_1971) {
                    return;
                }

                colorValidationTiming(v, c);

                long validationStartInAgoSeconds = Math.abs(Utils.getCurrentTimeSeconds() - startValidationCycle);
                String startsValidationDuration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(validationStartInAgoSeconds).toMillis(), "HH:mm:ss", true);
                if ((Utils.getCurrentTimeSeconds() - startValidationCycle) > 0) {
                    c.startCycle.setText(Utils.toLocal(startValidationCycle) + "  Started ago (" + startsValidationDuration + ")");
                } else {
                    c.startCycle.setText(Utils.toLocal(startValidationCycle) + "  Starts in (" + startsValidationDuration + ")");
                }
                long validationDurationInSeconds = v.getEndValidationCycle() - startValidationCycle;
                String validation1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(validationDurationInSeconds).toMillis(), "HH:mm:ss", true);
                c.endCycle.setText(Utils.toLocal(v.getEndValidationCycle()) + "  Duration (" + validation1Duration + ")");

                long electionsStartsInAgoSeconds = Math.abs(Utils.getCurrentTimeSeconds() - v.getStartElections());
                String startsElectionDuration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(electionsStartsInAgoSeconds).toMillis(), "HH:mm:ss", true);
                if ((Utils.getCurrentTimeSeconds() - v.getStartElections()) > 0) {
                    c.startElections.setText(Utils.toLocal(v.getStartElections()) + "  Started ago (" + startsElectionDuration + ")");
                } else {
                    c.startElections.setText(Utils.toLocal(v.getStartElections()) + "  Starts in (" + startsElectionDuration + ")");
                }
                long electionDurationInSeconds = v.getEndElections() - v.getStartElections();
                String elections1Duration = DurationFormatUtils.formatDuration(java.time.Duration.ofSeconds(electionDurationInSeconds).toMillis(), "HH:mm:ss", true);

                c.endElections.setText(Utils.toLocal(v.getEndElections()) + "  Duration (" + elections1Duration + ")");
                c.nextElections.setText(Utils.toLocal(v.getNextElections()));

                c.minterAddr.setText(v.getMinterAddr());
                c.configAddr.setText(v.getConfigAddr());
                c.electorAddr.setText(v.getElectorAddr());
                c.validationPeriod.setText(v.getValidationDuration().toString());
                c.electionPeriod.setText(v.getElectionDuration().toString());
                c.holdPeriod.setText(v.getHoldPeriod().toString());
                c.minimumStake.setText(v.getMinStake().divide(BigInteger.valueOf(1000000000L)).toString());
                c.maximumStake.setText(v.getMaxStake().divide(BigInteger.valueOf(1000000000L)).toString());

                c.nodePublicPort1.setText(MyLocalTon.getInstance().getSettings().getGenesisNode().getPublicPort().toString());
                c.nodeConsolePort1.setText(MyLocalTon.getInstance().getSettings().getGenesisNode().getConsolePort().toString());
                c.liteServerPort1.setText(MyLocalTon.getInstance().getSettings().getGenesisNode().getLiteServerPort().toString());

                c.validator1AdnlAddress.setText(MyLocalTon.getInstance().getSettings().getGenesisNode().getValidatorAdnlAddrHex());
                c.validator1PubKeyHex.setText(MyLocalTon.getInstance().getSettings().getGenesisNode().getValidatorPubKeyHex());

            } catch (Exception e) {
                log.error("Error updating validation tab GUI! Error {}", e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
