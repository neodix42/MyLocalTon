package org.ton.executors.liteclient;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.ton.executors.liteclient.api.*;
import org.ton.executors.liteclient.api.block.Currency;
import org.ton.executors.liteclient.api.block.*;
import org.ton.executors.liteclient.api.config.Validator;
import org.ton.executors.liteclient.api.config.Validators;
import org.ton.executors.liteclient.exception.IncompleteDump;
import org.ton.executors.liteclient.exception.ParsingError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
public class LiteClientParser {

    public static final String EOL = "\n";
    private static final String EOLWIN = "\r\n";
    private static final byte NOT_EXISTS_42 = -42;
    private static final String VALUE_COLON = "value:";
    public static final String SPACE = " ";
    public static final String OPEN = "(";
    public static final String CLOSE = ")";
    private static final String OPEN_CURLY = "{";
    private static final String TRANSACTION_TAG = "transaction #";
    private static final String WORKCHAIN_ID_COLON = "workchain_id:";
    private static final String ADDRESS_COLON = "address:";
    private static final String BOUNCE_COLON = "bounce:";
    private static final String SUCCESS_COLON = "success:";
    private static final String SEQ_NO_COLON = "seq_no:";
    private static final String END_LT_COLON = "end_lt:";
    private static final String START_LT_COLON = "start_lt:";

    private LiteClientParser() {
    }

    public static ResultLastBlock parseLast(String stdout) {

        if (StringUtils.isEmpty(stdout)) {
            log.debug("parseLast, stdout is empty: {}", stdout);
            return null;
        }

        if (StringUtils.contains(stdout, "adnl query timeout")) {
            log.debug("Blockchain node is not ready");
            return null;
        }

        if (StringUtils.contains(stdout, "server appears to be out of sync")) {
            log.debug("Blockchain node is out of sync");
        }

        try {

            String last = stdout.replace(EOL, SPACE);
            Long craetedAt = Long.parseLong(sb(last, "created at ", OPEN).trim());
            String fullBlockSeqno = sb(last, "last masterchain block is ", SPACE).trim();
            String shortBlockSeqno = OPEN + sb(fullBlockSeqno, OPEN, CLOSE) + CLOSE;
            String rootHashId = sb(fullBlockSeqno, ":", ":");
            String fileHashId = fullBlockSeqno.substring(fullBlockSeqno.lastIndexOf(':') + 1);
            String shard = sb(shortBlockSeqno, ",", ",");
            BigInteger pureBlockSeqno = new BigInteger(sb(shortBlockSeqno, shard + ",", CLOSE));
            Long wc = Long.parseLong(sb(shortBlockSeqno, OPEN, ","));
            Long secondsAgo = -1L;
            if (last.contains("seconds ago")) {
                String craetedAtStr = sb(last, "created at ", "seconds ago");
                secondsAgo = Long.parseLong(sb(craetedAtStr, OPEN, SPACE).trim());
            }

            return ResultLastBlock.builder()
                    .createdAt(craetedAt)
                    .seqno(pureBlockSeqno)
                    .rootHash(rootHashId)
                    .fileHash(fileHashId)
                    .wc(wc)
                    .shard(shard)
                    .syncedSecondsAgo(secondsAgo)
                    .build();

        } catch (Exception e) {
            log.debug("Error parsing lite-client's last command! Output: {}", stdout);
            return null;
        }
    }

    public static ResultLastBlock parseCreateHardFork(String stdout) {

        if (StringUtils.isEmpty(stdout)) {
            log.error("parseCreateHardfork, stdout is empty: {}", stdout);
            return null;
        }

        try {

            String last = stdout.replace(EOL, SPACE);

            String fullBlockSeqno = last.substring(last.indexOf("saved to disk") + 13).trim();
            String shortBlockSeqno = OPEN + sb(fullBlockSeqno, OPEN, CLOSE) + CLOSE;
            String rootHashId = sb(fullBlockSeqno, ":", ":");
            String fileHashId = fullBlockSeqno.substring(fullBlockSeqno.lastIndexOf(':') + 1);
            String shard = sb(shortBlockSeqno, ",", ",");
            BigInteger pureBlockSeqno = new BigInteger(sb(shortBlockSeqno, shard + ",", CLOSE));
            Long wc = Long.parseLong(sb(shortBlockSeqno, OPEN, ","));

            return ResultLastBlock.builder()
                    .seqno(pureBlockSeqno)
                    .rootHash(rootHashId)
                    .fileHash(fileHashId)
                    .wc(wc)
                    .shard(shard)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing create-hardfork's command! Output: {}, error: {}", stdout, e.getMessage());
            return null;
        }
    }

    public static ResultLastBlock parseBySeqno(String stdout) throws IncompleteDump, ParsingError {

        if (StringUtils.isEmpty(stdout) || stdout.contains("seqno not in db") || stdout.contains("block not found"))
            throw new IncompleteDump("parseBySeqno: block is missing");

        if (!stdout.contains("global_id"))
            throw new IncompleteDump("parseBySeqno: incomplete dump or block missing");
        try {

            String last = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

            String fullBlockSeqno = sb(last, "obtained block header for ", " from server");
            String shortBlockSeqno = OPEN + sb(fullBlockSeqno, OPEN, CLOSE) + CLOSE;
            String rootHashId = sb(fullBlockSeqno, ":", ":");
            String fileHashId = fullBlockSeqno.substring(fullBlockSeqno.lastIndexOf(':') + 1);
            String createdAt = sb(last, "@", "lt").trim();

            String shard = sb(shortBlockSeqno, ",", ",");
            BigInteger pureBlockSeqno = new BigInteger(sb(shortBlockSeqno, shard + ",", CLOSE));
            Long wc = Long.parseLong(sb(shortBlockSeqno, OPEN, ","));

            return ResultLastBlock.builder()
                    .seqno(pureBlockSeqno)
                    .rootHash(rootHashId)
                    .fileHash(fileHashId)
                    .wc(wc)
                    .shard(shard)
                    .createdAt(Long.valueOf(createdAt))
                    .build();

        } catch (Exception e) {
            throw new ParsingError("parseBySeqno: parsing error", e);
        }
    }

    public static List<ResultListBlockTransactions> parseListBlockTrans(String stdout) {

        if (StringUtils.isEmpty(stdout) || !stdout.contains(TRANSACTION_TAG))
            return Collections.emptyList();

        String onlyTransactions = stdout.substring(stdout.indexOf(TRANSACTION_TAG));

        String[] lines = onlyTransactions.split("\\r?\\n");

        List<ResultListBlockTransactions> txs = new ArrayList<>();

        for (String line : lines) {
            if (line.contains(TRANSACTION_TAG)) {
                BigInteger txSeqno = new BigInteger(sb(line, TRANSACTION_TAG, ":"));
                String txAccountAddress = sb(line, "account ", " lt").toUpperCase();
                BigInteger txLogicalTime = new BigInteger(sb(line, "lt ", " hash"));
                String txHash = line.substring(line.indexOf("hash ") + 5);
                txs.add(ResultListBlockTransactions.builder()
                        .txSeqno(txSeqno)
                        .accountAddress(txAccountAddress)
                        .lt(txLogicalTime)
                        .hash(txHash)
                        .build());
            }
        }
        return txs;
    }

    public static Transaction parseDumpTrans(String stdout, boolean includeMessageBody) {
        if (StringUtils.isEmpty(stdout)) {
            return null;
        }
        String blockdump = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);
        return parseTransaction(blockdump, includeMessageBody);
    }

    // config address
    public static ResultConfig0 parseConfig0(String stdout) {
        return ResultConfig0.builder()
                .configSmcAddr("-1:" + sb(stdout, "config_addr:x", CLOSE))
                .build();
    }

    // elector address
    public static ResultConfig1 parseConfig1(String stdout) {
        return ResultConfig1.builder()
                .electorSmcAddress("-1:" + sb(stdout, "elector_addr:x", CLOSE))
                .build();
    }

    // minter address
    public static ResultConfig2 parseConfig2(String stdout) {
        return ResultConfig2.builder()
                .minterSmcAddress("-1:" + sb(stdout, "minter_addr:x", CLOSE))
                .build();
    }

    public static ResultConfig12 parseConfig12(String stdout) {

        stdout = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

        return ResultConfig12.builder()
                .enabledSince(Long.parseLong(sb(stdout, "workchain enabled_since:", SPACE)))
                .actualMinSplit(Long.parseLong(sb(stdout, "actual_min_split:", SPACE)))
                .minSplit(Long.parseLong(sb(stdout, "min_split:", SPACE)))
                .maxSplit(Long.parseLong(sb(stdout, "max_split:", SPACE)))
                .basic(Long.parseLong(sb(stdout, "basic:", SPACE)))
                .active(Long.parseLong(sb(stdout, "active:", SPACE)))
                .acceptMsg(Long.parseLong(sb(stdout, "accept_msgs:", SPACE)))
                .flags(Long.parseLong(sb(stdout, "flags:", SPACE)))
                .rootHash(sb(stdout, "zerostate_root_hash:x", SPACE))
                .fileHash(sb(stdout, "zerostate_file_hash:x", SPACE))
                .version(Long.parseLong(sb(stdout, "version:", SPACE)))
                .build();
    }

    public static ResultConfig15 parseConfig15(String stdout) {

        stdout = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

        //    validators_elected_for:4000 elections_start_before:2000 elections_end_before:500 stake_held_for:1000
        return ResultConfig15.builder()
                .validatorsElectedFor(Long.parseLong(sb(stdout, "validators_elected_for:", SPACE)))
                .electionsStartBefore(Long.parseLong(sb(stdout, "elections_start_before:", SPACE)))
                .electionsEndBefore(Long.parseLong(sb(stdout, "elections_end_before:", SPACE)))
                .stakeHeldFor(Long.parseLong(sb(stdout, "stake_held_for:", ")")))
                .build();
    }

    public static ResultConfig17 parseConfig17(String stdout) {

        stdout = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

        String minStake = sbb(stdout, "min_stake:");
        String maxStake = sbb(stdout, "max_stake:");
        String minTotalStake = sbb(stdout, "min_total_stake:");

        return ResultConfig17.builder()
                .minStake(parseBigIntegerBracket(minStake, "value:"))
                .maxStake(parseBigIntegerBracket(maxStake, "value:"))
                .minTotalStake(parseBigIntegerBracket(minTotalStake, "value:"))
                .maxStakeFactor(parseBigIntegerBracket(stdout, "max_stake_factor:"))
                .build();
    }

    private static List<Validator> parseConfigValidators(String stdout) {
        List<Validator> validators = new ArrayList<>();

        List<String> unparsedLeafs = findStringBlocks(stdout, "node:(hmn_leaf");

        for (String leaf : unparsedLeafs) {

            String pubKey = sb(leaf, "pubkey:x", CLOSE);
            BigInteger weight;
            String adnlAddress;

            if (leaf.contains("adnl_addr:x")) {
                weight = new BigInteger(sb(leaf, "weight:", SPACE));
                adnlAddress = sb(leaf, "adnl_addr:x", CLOSE);
            } else {
                weight = new BigInteger(sb(leaf, "weight:", CLOSE));
                adnlAddress = null;
            }
            validators.add(Validator.builder()
                    .publicKey(pubKey)
                    .adnlAddress(adnlAddress)
                    .weight(weight)
                    .build());
        }

        return validators;
    }

    /**
     * current validators
     */
    public static ResultConfig34 parseConfig34(String stdout) {

        stdout = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

        List<Validator> validators = new ArrayList<>();

        if (stdout.contains("ConfigParam(34) = (null)")) {
            return ResultConfig34.builder()
                    .validators(Validators.builder()
                            .since(0L)
                            .until(0L)
                            .total(0L)
                            .main(0L)
                            .totalWeight(BigInteger.ZERO)
                            .validators(validators)
                            .build())
                    .build();
        }

        validators = parseConfigValidators(stdout);

        return ResultConfig34.builder()
                .validators(Validators.builder()
                        .since(Long.parseLong(sb(stdout, "utime_since:", SPACE)))
                        .until(Long.parseLong(sb(stdout, "utime_until:", SPACE)))
                        .total(Long.parseLong(sb(stdout, "total:", SPACE)))
                        .main(Long.parseLong(sb(stdout, "main:", SPACE)))
                        .totalWeight(new BigInteger(sb(stdout, "total_weight:", SPACE)))
                        .validators(validators)
                        .build())
                .build();
    }

    /**
     * next validators
     */
    public static ResultConfig36 parseConfig36(String stdout) {

        stdout = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

        List<Validator> validators = new ArrayList<>();

        if (stdout.contains("ConfigParam(36) = (null)")) {
            return ResultConfig36.builder()
                    .validators(Validators.builder()
                            .since(0L)
                            .until(0L)
                            .total(0L)
                            .main(0L)
                            .totalWeight(BigInteger.ZERO)
                            .validators(validators)
                            .build())
                    .build();
        }

        validators = parseConfigValidators(stdout);

        return ResultConfig36.builder()
                .validators(Validators.builder()
                        .since(Long.parseLong(sb(stdout, "utime_since:", SPACE)))
                        .until(Long.parseLong(sb(stdout, "utime_until:", SPACE)))
                        .total(Long.parseLong(sb(stdout, "total:", SPACE)))
                        .main(Long.parseLong(sb(stdout, "main:", SPACE)))
                        .totalWeight(new BigInteger(sb(stdout, "total_weight:", SPACE)))
                        .validators(validators)
                        .build())
                .build();
    }

    /**
     * previous validators
     */
    public static ResultConfig32 parseConfig32(String stdout) {

        stdout = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

        List<Validator> validators = new ArrayList<>();

        if (stdout.contains("ConfigParam(32) = (null)")) {
            return ResultConfig32.builder()
                    .validators(Validators.builder()
                            .since(0L)
                            .until(0L)
                            .total(0L)
                            .main(0L)
                            .totalWeight(BigInteger.ZERO)
                            .validators(validators)
                            .build())
                    .build();
        }

        validators = parseConfigValidators(stdout);

        return ResultConfig32.builder()
                .validators(Validators.builder()
                        .since(Long.parseLong(sb(stdout, "utime_since:", SPACE)))
                        .until(Long.parseLong(sb(stdout, "utime_until:", SPACE)))
                        .total(Long.parseLong(sb(stdout, "total:", SPACE)))
                        .main(Long.parseLong(sb(stdout, "main:", SPACE)))
                        .totalWeight(new BigInteger(sb(stdout, "total_weight:", SPACE)))
                        .validators(validators)
                        .build())
                .build();
    }

    public static List<ResultLastBlock> parseAllShards(String stdout) throws IncompleteDump, ParsingError {

        if (StringUtils.isEmpty(stdout) || stdout.contains("cannot load state for") || stdout.contains("state already gc'd"))
            throw new IncompleteDump("parseAllShards: incomplete dump");

        try {
            String onlyShards = stdout.substring(stdout.indexOf("shard #"));
            String[] lines = onlyShards.split("\\r?\\n");

            List<ResultLastBlock> shards = new ArrayList<>();

            for (String line : lines) {
                if (line.startsWith("shard")) {
                    //BigInteger seqno = new BigInteger(sb(line, "shard #", ":").trim());
                    String fullBlockSeqno = sb(line, ":", "@").trim();
                    String shortBlockSeqno = OPEN + sb(fullBlockSeqno, OPEN, CLOSE) + CLOSE;
                    String rootHash = sb(fullBlockSeqno, ":", ":");
                    String fileHash = fullBlockSeqno.substring(fullBlockSeqno.lastIndexOf(':') + 1);
                    String shard = sb(shortBlockSeqno, ",", ",");
                    BigInteger pureBlockSeqno = new BigInteger(sb(shortBlockSeqno, shard + ",", CLOSE));
                    Long wc = Long.parseLong(sb(shortBlockSeqno, OPEN, ","));

                    Long timestamp = Long.parseLong(sb(line, "@", "lt").trim());
                    //BigInteger startLt = new BigInteger(sb(line, "lt", "..").trim());
                    //BigInteger endLt = new BigInteger(line.substring(line.indexOf(".. ") + 3).trim());

                    ResultLastBlock resultLastBlock = ResultLastBlock.builder()
                            .wc(wc)
                            .shard(shard)
                            .seqno(pureBlockSeqno)
                            .rootHash(rootHash)
                            .fileHash(fileHash)
                            .createdAt(timestamp)
                            .build();

                    shards.add(resultLastBlock);
                }
            }

            return shards;
        } catch (Exception e) {
            throw new ParsingError("parseAllShards: parsing error", e);
        }
    }

    public static Block parseDumpblock(String stdout, boolean includeShardState, boolean includeMessageBody) throws IncompleteDump, ParsingError {

        if (StringUtils.isEmpty(stdout) || stdout.length() < 400)
            throw new IncompleteDump("parseDumpblock: incomplete dump");

        try {
            String blockdump = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

            Long globalBlockId = Long.parseLong(sb(blockdump, "block global_id:", SPACE));

            String blockInf = sbb(blockdump, "info:(block_info");
            String valueFlw = sbb(blockdump, "value_flow:(value_flow");

            String shardState = null;
            if (includeShardState) {
                shardState = sbb(blockdump, "state_update:(raw@(MERKLE_UPDATE");
            }

            String blockExtra = sbb(blockdump, "extra:(block_extra");
            Info info = parseBlockInfo(blockInf);
            ValueFlow valueFlow = parseValueFlow(valueFlw);
            Extra extra = parseExtra(blockExtra, includeMessageBody);

            return Block.builder()
                    .globalId(globalBlockId)
                    .info(info)
                    .valueFlow(valueFlow)
                    .shardState(shardState)
                    .extra(extra)
                    .build();

        } catch (Exception e) {
            throw new ParsingError("parseDumpblock: parsing error", e);
        }
    }

    private static Extra parseExtra(String blockExtra, Boolean includeMessageBody) {

        String inMsgDesc = sbb(blockExtra, "in_msg_descr:(");
        String outMsgDesc = sbb(blockExtra, "out_msg_descr:(");
        String accountBlocks = sbb(blockExtra, "account_blocks:(");
        String masterchainCustomBlock = sbb(blockExtra, "custom:(just");

        InMsgDescr inMsgDescr = parseInMsgDescr(inMsgDesc, includeMessageBody);

        OutMsgDescr outMsgDescr = parseOutMsgDescr(outMsgDesc, includeMessageBody);

        AccountBlock accountBlock = parseAccountBlock(accountBlocks, includeMessageBody);
        String randSeed = sb(blockExtra, "rand_seed:", SPACE);
        if (Strings.isNotEmpty(randSeed)) {
            randSeed = randSeed.substring(1);
        }
        String createdBy = sb(blockExtra, "created_by:", SPACE);
        if (Strings.isNotEmpty(createdBy)) {
            createdBy = createdBy.substring(1);
        }

        MasterchainBlock masterchainBlock = parseMasterchainBlock(masterchainCustomBlock, includeMessageBody);

        return Extra.builder()
                .inMsgDescrs(inMsgDescr)
                .outMsgsDescrs(outMsgDescr)
                .accountBlock(accountBlock)
                .masterchainBlock(masterchainBlock)
                .randSeed(randSeed)
                .createdBy(createdBy)
                .build();
    }

    private static MasterchainBlock parseMasterchainBlock(String masterchainBlock, boolean includeMessageBody) {

        String hashesBlock = sbb(masterchainBlock, "shard_hashes:(");
        Long wc = parseLongSpace(sbb(hashesBlock, "label:(hml_same"), "v:");
        List<String> shards = LiteClientParser.findStringBlocks(hashesBlock, "leaf:(shard_descr");

        List<ShardHash> shardHashes = parseShardHashes(shards, wc);
        List<ShardFee> shardFees = parseShardFees(shards, wc); // TODO need example dump

        //recover create msg
        String recoverCreateMsg = sbb(masterchainBlock, "recover_create_msg:(");
        List<Transaction> txsRecoverCreateMsg = parseTxs(recoverCreateMsg, includeMessageBody);
        Message inMsgRecoverCreateMsg = parseInMessage(recoverCreateMsg, includeMessageBody);
        RecoverCreateMessage recoverCreateMessage = RecoverCreateMessage.builder()
                .transactions(txsRecoverCreateMsg)
                .inMsg(inMsgRecoverCreateMsg)
                .build();

        //mint msg
        String mintMsg = sbb(masterchainBlock, "mint_msg:(");
        List<Transaction> txsMintMsg = parseTxs(mintMsg, includeMessageBody);
        Message inMsgMintMsg = parseInMessage(mintMsg, includeMessageBody);
        MintMessage mintMessage = MintMessage.builder()
                .transactions(txsMintMsg)
                .inMsg(inMsgMintMsg).build();

        return MasterchainBlock.builder()
                .wc(wc)
                .shardHashes(shardHashes)
                .shardFees(shardFees)
                .recoverCreateMsg(recoverCreateMessage)
                .mintMsg(mintMessage)
                .build();
    }

    private static List<ShardFee> parseShardFees(List<String> shards, Long workchain) {
        List<ShardFee> shardFees = new ArrayList<>();
        return shardFees;
    }

    private static List<ShardHash> parseShardHashes(List<String> shards, Long workchain) {

        List<ShardHash> shardHashes = new ArrayList<>();

        for (String shard : shards) {

            BigInteger seqno = parseBigIntegerSpace(shard, SEQ_NO_COLON);
            BigInteger regMcSeqno = parseBigIntegerSpace(shard, "reg_mc_seqno:");
            BigInteger startLt = parseBigIntegerSpace(shard, START_LT_COLON);
            BigInteger endLt = parseBigIntegerSpace(shard, END_LT_COLON);
            String rootHash = sb(shard, "root_hash:", SPACE);
            String fileHash = sb(shard, "file_hash:", SPACE);
            Byte beforeSplit = parseByteSpace(shard, "before_split:");
            Byte beforeMerge = parseByteSpace(shard, "before_merge:");
            Byte wantSplit = parseByteSpace(shard, "want_split:");
            Byte wantMerge = parseByteSpace(shard, "want_merge:");
            Byte nxCcUpdated = parseByteSpace(shard, "nx_cc_updated:");
            Byte flags = parseByteSpace(shard, "flags:");
            BigInteger nextCatchainSeqno = parseBigIntegerSpace(shard, "next_catchain_seqno:");
            String nextValidatorShard = sb(shard, "next_validator_shard:", SPACE);
            BigInteger minRefMcSeqno = parseBigIntegerSpace(shard, "min_ref_mc_seqno:");
            Long getUtime = parseLongSpace(shard, "gen_utime:");
            Value feesCollected = readValue(sbb(shard, "fees_collected:(currencies"));
            Value fundsCreated = readValue(sbb(shard, "funds_created:(currencies"));

            ShardHash shardHash = ShardHash.builder()
                    .wc(workchain)
                    .seqno(seqno)
                    .regMcSeqno(regMcSeqno)
                    .startLt(startLt)
                    .endLt(endLt)
                    .rootHash(rootHash)
                    .fileHash(fileHash)
                    .beforeSplit(beforeSplit)
                    .beforeMerge(beforeMerge)
                    .wantSplit(wantSplit)
                    .wantMerge(wantMerge)
                    .nxCcUpdate(nxCcUpdated)
                    .flags(flags)
                    .nextCatchainSeqno(nextCatchainSeqno)
                    .nextValidatorShard(nextValidatorShard)
                    .minRefMcSeqno(minRefMcSeqno)
                    .genUtime(getUtime)
                    .feesCollected(feesCollected)
                    .fundsCreated(fundsCreated)
                    .build();

            shardHashes.add(shardHash);
        }
        return shardHashes;
    }

    private static AccountBlock parseAccountBlock(String accountBlocks, Boolean includeMessageBody) {
        List<String> txsList = LiteClientParser.findStringBlocks(accountBlocks, "value:^(transaction");
        if (!txsList.isEmpty()) {
            List<Transaction> txs = txsList.stream().map(x -> LiteClientParser.parseTransaction(x, includeMessageBody)).collect(Collectors.toList());
            return AccountBlock.builder().transactions(txs).build();
        } else {
            return AccountBlock.builder().transactions(Collections.emptyList()).build();
        }
    }

    private static List<Transaction> parseTxs(String content, Boolean includeMessageBody) {
        List<Transaction> txs = new ArrayList<>();
        List<String> txsList = LiteClientParser.findStringBlocks(content, "transaction:(");
        if (!txsList.isEmpty()) {
            txs = txsList.stream().map(x -> LiteClientParser.parseTransaction(x, includeMessageBody)).collect(Collectors.toList());
            return txs;
        } else {
            return txs;
        }
    }

    private static OutMsgDescr parseOutMsgDescr(String outMsgDescr, Boolean includeMessageBody) {
        List<String> unparsedLeafs = findLeafsWithLabel(outMsgDescr, "node:(ahmn_leaf");

        List<Leaf> parsedLeafs = parseLeafs(unparsedLeafs, includeMessageBody);

        return OutMsgDescr.builder()
                .leaf(parsedLeafs)
                .build();
    }

    private static InMsgDescr parseInMsgDescr(String inMsgDesc, boolean includeMessageBody) {

        List<String> unparsedLeafs = findLeafsWithLabel(inMsgDesc, "node:(ahmn_leaf");

        List<Leaf> parsedLeafs = parseLeafs(unparsedLeafs, includeMessageBody);

        return InMsgDescr.builder()
                .leaf(parsedLeafs)
                .build();
    }

    private static List<Leaf> parseLeafs(List<String> unparsedLeafs, boolean includeMessageBody) {
        List<Leaf> result = new ArrayList<>();
        for (String unparsedLeaf : unparsedLeafs) {
            String label = readLabel(unparsedLeaf);
            Message inMsg = parseInMessage(unparsedLeaf, includeMessageBody);
            List<Transaction> txs = parseTxs(unparsedLeaf, includeMessageBody);
            BigDecimal feesCollected = parseBigDecimalBracket(sbb(unparsedLeaf, "fees_collected:("), VALUE_COLON);
            Value valueImported = readValue(sbb(unparsedLeaf, "value_imported:(currencies"));

            Leaf leaf = Leaf.builder()
                    .label(label)
                    .message(inMsg)
                    .transactions(txs)
                    .feesCollected(feesCollected)
                    .valueImported(valueImported)
                    .build();
            result.add(leaf);
        }
        return result;
    }

    private static String readLabel(String unparsedLeaf) {
        String result = null;
        try {
            String label = sbb(unparsedLeaf, "label:(");
            result = label.substring(label.indexOf("s:x") + 3, label.length() - 2);
        } catch (Exception e) {
            log.error("cannot parse label in extra block");
        }
        return result;
    }

    private static Message parseInMessage(String inMsgDescr, boolean includeMessageBody) {

        String message = sbb(inMsgDescr, "(message");

        if (Strings.isNotEmpty(message)) {
            // message exist
            String msgType = convertMsgType(sb(message, "info:(", SPACE));
            Byte ihrDisabled = parseByteSpace(message, "ihr_disabled:");
            Byte bounce = parseByteSpace(message, BOUNCE_COLON);
            Byte bounced = parseByteSpace(message, "bounced:");
            BigInteger createdLt = parseBigIntegerSpace(message, "created_lt:");
            BigInteger createdAt = parseBigIntegerBracket(message, "created_at:");

            String src = sbb(message, "src:(");
            Long srcWc = parseLongSpace(src, WORKCHAIN_ID_COLON);
            String srcAddr = sb(src, ADDRESS_COLON, CLOSE);
            if (Strings.isNotEmpty(srcAddr)) {
                srcAddr = srcAddr.substring(1);
            }
            LiteClientAddress sourceAddr = LiteClientAddress.builder().wc(srcWc).addr(srcAddr).build();

            String dest = sbb(message, "dest:(");
            Long destWc = parseLongSpace(dest, WORKCHAIN_ID_COLON);
            String destAddr = sb(dest, ADDRESS_COLON, CLOSE);
            if (Strings.isNotEmpty(destAddr)) {
                destAddr = destAddr.substring(1);
            }
            LiteClientAddress destinationAddr = LiteClientAddress.builder().wc(destWc).addr(destAddr).build();

            Value grams = readValue(sbb(inMsgDescr, "value:(currencies"));
//add import_fee handling
            BigDecimal ihrFee = parseBigDecimalBracket(sbb(message, "ihr_fee:("), VALUE_COLON);
            BigDecimal fwdFee = parseBigDecimalBracket(sbb(message, "fwd_fee:("), VALUE_COLON);

            BigDecimal importFee = parseBigDecimalBracket(sbb(message, "import_fee:("), VALUE_COLON);

            String initContent = sbb(message, "init:(");
            Init init = parseInit(initContent);

            Body body = null;
            if (includeMessageBody) {
                String bodyContent = sbb(message, "body:(");
                body = parseBody(bodyContent);
            }

            return Message.builder()
                    .srcAddr(sourceAddr)
                    .destAddr(destinationAddr)
                    .type(msgType)
                    .ihrDisabled(ihrDisabled)
                    .bounce(bounce)
                    .bounced(bounced)
                    .value(grams)
                    .ihrFee(ihrFee)
                    .fwdFee(fwdFee)
                    .importFee(importFee)
                    .createdLt(createdLt)
                    .createdAt(createdAt)
                    .init(init)
                    .body(body)
                    .build();

        } else {
            return null;
        }
    }

    private static Body parseBody(String bodyContent) {
        if (StringUtils.isEmpty(bodyContent)) {
            return Body.builder()
                    .cells(new ArrayList<>())
                    .build();
        }
        String[] bodyCells = bodyContent.split("x\\{");
        ArrayList<String> cells = new ArrayList<>(Arrays.asList(bodyCells));
        ArrayList<String> cleanedCells = cleanCells(cells);

        return Body.builder()
                .cells(cleanedCells)
                .build();
    }

    private static ArrayList<String> cleanCells(ArrayList<String> cells) {
        ArrayList<String> cleanCells = new ArrayList<>();
        if (!isNull(cells) && cells.size() > 0) {
            cells.remove(0);
            cells.stream().filter(s -> s.replace(")", "").replace(" ", "").replace("}", "").length() > 0).forEach(s -> {
                cleanCells.add(s.replace(")", "").replace(" ", "").replace("}", ""));
            });
        }
        return cleanCells;
    }

    private static Init parseInit(String initContent) {
        if (StringUtils.isEmpty(initContent)) {
            return Init.builder()
                    .code(new ArrayList<>())
                    .data(new ArrayList<>())
                    .library(new ArrayList<>())
                    .build();
        }
        String code = sbb(initContent, "code:(");
        String[] codeCells = isNull(code) ? new String[0] : code.split("x\\{");
        ArrayList<String> cleanedCodeCells = cleanCells(new ArrayList<>(Arrays.asList(codeCells)));

        String data = sbb(initContent, "data:(");
        String[] dataCells = isNull(data) ? new String[0] : data.split("x\\{");
        ArrayList<String> cleanedDataCells = cleanCells(new ArrayList<>(Arrays.asList(dataCells)));

        String library = sbb(initContent, "library:(");
        String[] libraryCells = isNull(library) ? new String[0] : library.split("x\\{");
        ArrayList<String> cleanedLibraryCells = cleanCells(new ArrayList<>(Arrays.asList(libraryCells)));

        return Init.builder()
                .code(cleanedCodeCells)
                .data(cleanedDataCells)
                .library(cleanedLibraryCells)
                .build();
    }

    private static List<Message> parseOutMessages(String outMsgsField, boolean includeMessageBody) {

        List<Message> outMsgs = new ArrayList<>();
        if (StringUtils.isEmpty(outMsgsField)) {
            return outMsgs;
        }

        while (outMsgsField.contains("(message")) {

            String message = sbb(outMsgsField, "(message");

            if (Strings.isNotEmpty(message)) {
                // message exist
                String msgType = convertMsgType(sb(message, "info:(", SPACE));
                Byte ihrDisabled = parseByteSpace(message, "ihr_disabled:");
                Byte bounce = parseByteSpace(message, BOUNCE_COLON);
                Byte bounced = parseByteSpace(message, "bounced:");
                BigInteger createdLt = parseBigIntegerSpace(message, "created_lt:");
                BigInteger createdAt = parseBigIntegerBracket(message, "created_at:");

                String src = sbb(message, "src:(");
                Long srcWc = parseLongSpace(src, WORKCHAIN_ID_COLON);
                String srcAddr = sb(src, ADDRESS_COLON, CLOSE);
                if (Strings.isNotEmpty(srcAddr)) {
                    srcAddr = srcAddr.substring(1);
                }
                LiteClientAddress sourceAddr = LiteClientAddress.builder().wc(srcWc).addr(srcAddr).build();

                String dest = sbb(message, "dest:(");
                Long destWc = parseLongSpace(dest, WORKCHAIN_ID_COLON);
                String destAddr = sb(dest, ADDRESS_COLON, CLOSE);
                if (Strings.isNotEmpty(destAddr)) {
                    destAddr = destAddr.substring(1);
                }
                LiteClientAddress destinationAddr = LiteClientAddress.builder().wc(destWc).addr(destAddr).build();

                Value toncoins = readValue(sbb(message, "value:(currencies"));

                BigDecimal ihrFee = parseBigDecimalBracket(sbb(message, "ihr_fee:("), VALUE_COLON);
                BigDecimal fwdFee = parseBigDecimalBracket(sbb(message, "fwd_fee:("), VALUE_COLON);
                BigDecimal importFee = parseBigDecimalBracket(sbb(message, "import_fee:("), VALUE_COLON);

                String initContent = sbb(message, "init:(");
                Init init = parseInit(initContent);

                Body body = null;
                if (includeMessageBody) {
                    String bodyContent = sbb(message, "body:(");
                    body = parseBody(bodyContent);
                }

                outMsgs.add(
                        Message.builder()
                                .srcAddr(sourceAddr)
                                .destAddr(destinationAddr)
                                .type(msgType)
                                .ihrDisabled(ihrDisabled)
                                .bounce(bounce)
                                .bounced(bounced)
                                .value(toncoins)
                                .ihrFee(ihrFee)
                                .fwdFee(fwdFee)
                                .importFee(importFee)
                                .createdLt(createdLt)
                                .createdAt(createdAt)
                                .init(init)
                                .body(body)
                                .build());
                outMsgsField = outMsgsField.replace(message, "");
            }
        }
        return outMsgs;
    }

    private static String convertMsgType(String type) {
        switch (type) {
            case "int_msg_info":
                return "Internal";
            case "ext_in_msg_info":
                return "External In";
            case "ext_out_msg_info":
                return "External Out";
            default:
                return "Unknown";
        }
    }

    private static Transaction parseTransaction(String str, Boolean includeMessageBody) {

        String transaction = sbb(str, "(transaction account_addr");

        if (StringUtils.isNotEmpty(transaction)) {

            String accountAddr = sb(transaction, "account_addr:", "lt").trim();
            if (Strings.isNotEmpty(accountAddr)) {
                accountAddr = accountAddr.substring(1);
            }
            BigInteger lt = parseBigIntegerSpace(transaction, "lt:");
            Long now = parseLongSpace(transaction, "now:");
            BigInteger prevTxLt = parseBigIntegerSpace(transaction, "prev_trans_lt:");
            String prevTxHash = sb(transaction, "prev_trans_hash:", SPACE).trim();
            if (Strings.isNotEmpty(prevTxHash)) {
                prevTxHash = prevTxHash.substring(1);
            }
            Long outMsgsCnt = parseLongSpace(transaction, "outmsg_cnt:");
            String origStatus = sb(transaction, "orig_status:", SPACE).trim();
            String endStatus = sb(transaction, "end_status:", SPACE).trim();

            origStatus = convertAccountStatus(origStatus);
            endStatus = convertAccountStatus(endStatus);

            //String inMsgField = sb(transaction, "in_msg:", "))))") + ")))"; // small hack
            String inMsgField = sbb(transaction, "in_msg:"); // small hack
            Message inMsg = parseInMessage(inMsgField, includeMessageBody);

            //String outMsgsField = sb(transaction, "out_msgs:", "))))))") + ")))"; // small hack
            String outMsgsField = sbb(transaction, "out_msgs:");
            List<Message> outMsgs = parseOutMessages(outMsgsField, includeMessageBody);

            String stateUpdate = sbb(transaction, "state_update:(");
            String oldHash = sb(stateUpdate, "old_hash:", SPACE);
            if (Strings.isNotEmpty(oldHash)) {
                oldHash = oldHash.substring(1);
            }
            String newHash = sb(stateUpdate, "new_hash:", CLOSE);
            if (Strings.isNotEmpty(newHash)) {
                newHash = newHash.substring(1);
            }

            Value totalFees = readValue(sbb(transaction, "total_fees:(currencies"));

            String description = sbb(transaction, "description:(");

            TransactionDescription txdDescription = parseTransactionDescription(description);

            return Transaction.builder()
                    .accountAddr(accountAddr)
                    .now(now)
                    .lt(lt)
                    .prevTxHash(prevTxHash)
                    .prevTxLt(prevTxLt)
                    .outMsgsCount(outMsgsCnt)
                    .origStatus(origStatus)
                    .endStatus(endStatus)
                    .inMsg(inMsg)
                    .outMsgs(outMsgs)
                    .totalFees(totalFees)
                    .oldHash(oldHash)
                    .newHash(newHash)
                    .description(txdDescription)
                    .build();
        } else {
            return null;
        }
    }

    private static String convertAccountStatus(String origStatus) {
        String status;
        switch (origStatus) {
            case "acc_state_uninit":
                status = "Uninitialized";
                break;
            case "acc_state_frozen":
                status = "Frozen";
                break;
            case "acc_state_active":
                status = "Active";
                break;
            case "acc_state_nonexist":
                status = "Nonexistent";
                break;
            default:
                status = "Unknown";
        }
        return status;

    }

    private static TransactionDescription parseTransactionDescription(String description) {

        BigDecimal creditFirst = parseBigDecimalSpace(description, "credit_first:");
        String storageStr = sbb(description, "storage_ph:(");
        String creditStr = sbb(description, "credit_ph:(");
        String computeStr = sbb(description, "compute_ph:(");
        String actionStr = sbb(description, "action:(");
        Byte bounce = parseByteSpace(description, BOUNCE_COLON);
        Byte aborted = parseByteSpace(description, "aborted:");
        Byte destroyed = parseByteBracket(description, "destroyed:");

        Byte tock = parseByteSpace(description, "is_tock:");
        String type = "Tock";
        if (nonNull(tock)) {
            if (tock == 0) {
                type = "Tick";
            }
        } else {
            type = "Ordinary";
        }

        TransactionStorage txStorage = parseTransactionStorage(storageStr);
        TransactionCredit txCredit = parseTransactionCredit(creditStr);
        TransactionCompute txCompute = parseTransactionCompute(computeStr);
        TransactionAction txAction = parseTransactionAction(actionStr);

        return TransactionDescription.builder()
                .aborted(aborted)
                .bounce(bounce)
                .destroyed(destroyed)
                .action(txAction)
                .compute(txCompute)
                .credit(txCredit)
                .storage(txStorage)
                .creditFirst(creditFirst)
                .type(type)
                .build();
    }

    private static TransactionAction parseTransactionAction(String actionStr) {
        if (StringUtils.isEmpty(actionStr)) {
            return TransactionAction.builder().build();
        } else {
            Byte actionSuccess = parseByteSpace(actionStr, SUCCESS_COLON);
            Byte actionValid = parseByteSpace(actionStr, "valid:");
            Byte actionNoFunds = parseByteSpace(actionStr, "no_funds:");
            String statusChanged = sb(actionStr, "status_change:", SPACE);
            BigDecimal totalFwdFees = parseBigDecimalBracket(sbb(actionStr, "total_fwd_fees:("), VALUE_COLON);
            BigDecimal totalActionFees = parseBigDecimalBracket(sbb(actionStr, "total_action_fees:("), VALUE_COLON);
            BigInteger resultCode = parseBigIntegerSpace(actionStr, "result_code:");
            BigInteger resultArg = parseBigIntegerBracket(sbb(actionStr, "result_arg:("), VALUE_COLON);
            BigInteger totalActions = parseBigIntegerSpace(actionStr, "tot_actions:");
            BigInteger specActions = parseBigIntegerSpace(actionStr, "spec_actions:");
            BigInteger skippedActions = parseBigIntegerSpace(actionStr, "skipped_actions:");
            BigInteger msgsCreated = parseBigIntegerSpace(actionStr, "msgs_created:");
            String actionListHash = sb(actionStr, "action_list_hash:", SPACE);
            if (Strings.isNotEmpty(actionListHash)) {
                actionListHash = actionListHash.substring(1);
            }
            BigInteger totalMsgSizeCells = parseBigIntegerBracket(sbb(sbb(actionStr, "tot_msg_size:("), "cells:("), VALUE_COLON);
            BigInteger totalMsgSizeBits = parseBigIntegerBracket(sbb(sbb(actionStr, "tot_msg_size:("), "bits:("), VALUE_COLON);

            return TransactionAction.builder()
                    .success(actionSuccess)
                    .valid(actionValid)
                    .noFunds(actionNoFunds)
                    .statusChange(statusChanged)
                    .totalFwdFee(totalFwdFees)
                    .totalActionFee(totalActionFees)
                    .resultCode(resultCode)
                    .resultArg(resultArg)
                    .totActions(totalActions)
                    .specActions(specActions)
                    .skippedActions(skippedActions)
                    .msgsCreated(msgsCreated)
                    .actionListHash(actionListHash)
                    .totalMsgSizeCells(totalMsgSizeCells)
                    .totalMsgSizeBits(totalMsgSizeBits)
                    .build();
        }
    }

    private static TransactionCredit parseTransactionCredit(String creditStr) {
        if (StringUtils.isEmpty(creditStr)) {
            return TransactionCredit.builder().build();
        } else {
            BigDecimal creditDueFeesCollected = parseBigDecimalBracket(sbb(creditStr, "due_fees_collected:("), VALUE_COLON);
            Value credit = readValue(sbb(creditStr, "due_fees_collected"));
            return TransactionCredit.builder()
                    .credit(credit)
                    .dueFeesCollected(creditDueFeesCollected)
                    .build();
        }
    }

    private static TransactionStorage parseTransactionStorage(String storageStr) {
        if (StringUtils.isEmpty(storageStr)) {
            return TransactionStorage.builder().build();
        } else {
            BigDecimal storageFeesCollected = parseBigDecimalBracket(sbb(storageStr, "(tr_phase_storage"), VALUE_COLON);
            BigDecimal dueFees = parseBigDecimalBracket(sbb(storageStr, "storage_fees_due:("), VALUE_COLON);
            String storageSccountStatus = sb(storageStr, "status_change:", CLOSE);
            return TransactionStorage.builder()
                    .feesCollected(storageFeesCollected)
                    .feesDue(dueFees)
                    .statusChange(storageSccountStatus)
                    .build();
        }
    }

    private static TransactionCompute parseTransactionCompute(String computeStr) {
        if (StringUtils.isEmpty(computeStr)) {
            return TransactionCompute.builder().build();
        } else {
            BigDecimal gasFees = parseBigDecimalBracket(sbb(computeStr, "gas_fees:("), VALUE_COLON);
            Byte success = parseByteSpace(computeStr, SUCCESS_COLON);
            Byte msgStateUsed = parseByteSpace(computeStr, "msg_state_used:");
            Byte accountActivated = parseByteSpace(computeStr, "account_activated:");
            BigDecimal gasUsed = parseBigDecimalBracket(sbb(computeStr, "gas_used:("), VALUE_COLON);
            BigDecimal gasLimit = parseBigDecimalBracket(sbb(computeStr, "gas_limit:("), VALUE_COLON);
            BigDecimal gasCredit = parseBigDecimalBracket(sbb(computeStr, "gas_credit:("), VALUE_COLON); // TODO parse gas_credit:(just           value:(var_uint len:2 value:10000))
            String exitArgs = sb(computeStr, "exit_arg:", SPACE);
            BigInteger exitCode = parseBigIntegerSpace(computeStr, "exit_code:");
            BigInteger mode = parseBigIntegerSpace(computeStr, "mode:");
            BigInteger vmsSteps = parseBigIntegerSpace(computeStr, "vm_steps:");
            String vmInitStateHash = sb(computeStr, "vm_init_state_hash:", SPACE);
            if (Strings.isNotEmpty(vmInitStateHash)) {
                vmInitStateHash = vmInitStateHash.substring(1);
            }
            String vmFinalStateHash = sb(computeStr, "vm_final_state_hash:", CLOSE);
            if (Strings.isNotEmpty(vmFinalStateHash)) {
                vmFinalStateHash = vmFinalStateHash.substring(1);
            }
            return TransactionCompute.builder()
                    .gasFees(gasFees)
                    .gasCredit(gasCredit)
                    .gasUsed(gasUsed)
                    .gasLimit(gasLimit)
                    .accountActivated(accountActivated)
                    .msgStateUsed(msgStateUsed)
                    .success(success)
                    .mode(mode)
                    .vmSteps(vmsSteps)
                    .vmInitStateHash(vmInitStateHash)
                    .vmFinalStateHash(vmFinalStateHash)
                    .exitArg(exitArgs)
                    .exitCode(exitCode)
                    .build();
        }
    }

    private static ValueFlow parseValueFlow(String valueFlw) {

        Value prevBlock = readValue(sbb(valueFlw, "from_prev_blk:(currencies"));
        Value nextBlock = readValue(sbb(valueFlw, "to_next_blk:(currencies"));
        Value imported = readValue(sbb(valueFlw, "imported:(currencies"));
        Value exported = readValue(sbb(valueFlw, "exported:(currencies"));
        Value feesCollected = readValue(sbb(valueFlw, "fees_collected:(currencies"));
        Value feesImported = readValue(sbb(valueFlw, "fees_imported:(currencies"));
        Value recovered = readValue(sbb(valueFlw, "recovered:(currencies"));
        Value created = readValue(sbb(valueFlw, "created:(currencies"));
        Value minted = readValue(sbb(valueFlw, "minted:(currencies"));

        return ValueFlow.builder()
                .prevBlock(prevBlock)
                .nextBlock(nextBlock)
                .imported(imported)
                .exported(exported)
                .feesCollected(feesCollected)
                .feesImported(feesImported)
                .recovered(recovered)
                .created(created)
                .minted(minted)
                .build();
    }

    private static Value readValue(String str) {
        if (StringUtils.isEmpty(str))
            return Value.builder().toncoins(BigDecimal.ZERO).build();

        String res = sbb(str, "amount:(");
        BigDecimal grams = parseBigDecimalBracket(res, VALUE_COLON);

        List<Currency> otherCurrencies = new ArrayList<>();

        if (nonNull(str) && str.contains("node:(hmn_fork")) {
            // exist extra currencies
            List<String> currenciesLeft = findStringBlocks(str, "left:(hm_edge");
            List<String> currenciesRight = findStringBlocks(str, "right:(hm_edge");
            List<String> currencies = new ArrayList<>();
            currencies.addAll(currenciesLeft);
            currencies.addAll(currenciesRight);

            if (!currencies.isEmpty()) {
                for (String cur : currencies) {
                    Byte len = parseByteSpace(cur, "len:");
                    String label = sb(cur, "s:", CLOSE);
                    if (!StringUtils.isEmpty(label)) {
                        label = label.substring(1);
                    }
                    if (!isNull(len)) {
                        BigDecimal value = parseBigDecimalBracket(cur.substring(cur.indexOf("var_uint")), VALUE_COLON);

                        Currency currency = Currency.builder()
                                .label(label)
                                .len(len)
                                .value(value)
                                .build();
                        otherCurrencies.add(currency);
                    }
                }
            }
        }

        return Value.builder()
                .toncoins(grams)
                .otherCurrencies(otherCurrencies)
                .build();
    }

    private static List<Library> readLibraries(String str) {
        if (StringUtils.isEmpty(str))
            return List.of(Library.builder().build());

        List<Library> allLibraries = new ArrayList<>();

        if (str.contains("node:(hmn_fork")) {
            List<String> librariesLeft = findStringBlocks(str, "left:(hm_edge");
            List<String> librariesRight = findStringBlocks(str, "right:(hm_edge");
            List<String> libraries = new ArrayList<>();
            libraries.addAll(librariesLeft);
            libraries.addAll(librariesRight);

            if (!libraries.isEmpty()) {
                for (String lib : libraries) {
                    //Byte len = parseByteSpace(cur, "len:");
                    String label = sb(lib, "s:", CLOSE);
                    if (!StringUtils.isEmpty(label)) {
                        label = label.substring(1);
                    }
                    String type = sb(lib, "value:(", SPACE);
                    Long publicFlag = parseLongSpace(lib, "public:");

                    String raw = sbb(lib, "root:(");
                    String[] rawCells = isNull(raw) ? new String[0] : raw.split("x\\{");
                    ArrayList<String> rawLibrary = cleanCells(new ArrayList<>(Arrays.asList(rawCells)));

                    Library library = Library.builder()
                            .label(label)
                            .type(type)
                            .publicFlag(publicFlag)
                            .rawData(rawLibrary)
                            .build();

                    allLibraries.add(library);
                }
            }
        }

        return allLibraries;
    }

    private static Info parseBlockInfo(String blockInf) {

        BigInteger version = parseBigIntegerSpace(blockInf, "version:");
        Byte notMaster = parseByteSpace(blockInf, "not_master:");
        BigInteger keyBlock = parseBigIntegerSpace(blockInf, "key_block:");
        BigInteger vertSeqnoIncr = parseBigIntegerSpace(blockInf, "vert_seqno_incr:");
        BigInteger vertSeqno = parseBigIntegerSpace(blockInf, "vert_seq_no:");
        BigInteger genValidatorListHashShort = parseBigIntegerSpace(blockInf, "gen_validator_list_hash_short:");
        BigInteger genCatchainSeqno = parseBigIntegerSpace(blockInf, "gen_catchain_seqno:");
        BigInteger minRefMcSeqno = parseBigIntegerSpace(blockInf, "min_ref_mc_seqno:");
        BigInteger prevKeyBlockSeqno = parseBigIntegerSpace(blockInf, "prev_key_block_seqno:");
        Byte wantSplit = parseByteSpace(blockInf, "want_split:");
        Byte afterSplit = parseByteSpace(blockInf, "after_split:");
        Byte beforeSplit = parseByteSpace(blockInf, "before_split:");
        Byte afterMerge = parseByteSpace(blockInf, "after_merge:");
        Byte wantMerge = parseByteSpace(blockInf, "want_merge:");
        BigInteger seqno = parseBigIntegerSpace(blockInf, SEQ_NO_COLON);
        Long wc = parseLongSpace(blockInf, WORKCHAIN_ID_COLON);
        BigInteger startLt = parseBigIntegerSpace(blockInf, START_LT_COLON);
        BigInteger endLt = parseBigIntegerSpace(blockInf, END_LT_COLON);
        Long genUtime = parseLongSpace(blockInf, "gen_utime:");
        String prev = sbb(blockInf, "prev:(");
        Long prevSeqno = parseLongSpace(prev, SEQ_NO_COLON);
        BigInteger prevEndLt = parseBigIntegerSpace(prev, END_LT_COLON);
        String prevRootHash = sb(prev, "root_hash:", SPACE);
        if (Strings.isNotEmpty(prevRootHash)) {
            prevRootHash = prevRootHash.substring(1);
        }
        String prevFileHash = sb(prev, "file_hash:", CLOSE);
        if (Strings.isNotEmpty(prevFileHash)) {
            prevFileHash = prevFileHash.substring(1);
        }
        return Info.builder()
                .version(version)
                .wc(wc)
                .notMaster(notMaster)
                .keyBlock(keyBlock)
                .vertSeqno(vertSeqno)
                .vertSeqnoIncr(vertSeqnoIncr)
                .getValidatorListHashShort(genValidatorListHashShort)
                .getCatchainSeqno(genCatchainSeqno)
                .minRefMcSeqno(minRefMcSeqno)
                .prevKeyBlockSeqno(prevKeyBlockSeqno)
                .wantSplit(wantSplit)
                .wantMerge(wantMerge)
                .afterMerge(afterMerge)
                .afterSplit(afterSplit)
                .beforeSplit(beforeSplit)
                .seqNo(seqno)
                .startLt(startLt)
                .endLt(endLt)
                .genUtime(genUtime)
                .prevBlockSeqno(prevSeqno)
                .prevEndLt(prevEndLt)
                .prevRootHash(prevRootHash)
                .prevFileHash(prevFileHash)
                .build();
    }

    public static String sb(String str, String from, String to) {
        return StringUtils.substringBetween(str, from, to);
    }

    private static BigDecimal parseBigDecimalSpace(String str, String from) {
        try {
            String result = sb(str, from, SPACE);
            return isNull(result) ? null : new BigDecimal(result);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigInteger parseBigIntegerSpace(String str, String from) {
        try {
            String result = sb(str, from, SPACE);
            return isNull(result) ? null : new BigInteger(result);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseLongSpace(String str, String from) {
        try {
            String result = sb(str, from, SPACE);
            return isNull(result) ? null : Long.valueOf(result);
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseLongBracket(String str, String from) {
        if (isNull(str)) {
            return 0L;
        }
        try {
            String result = sb(str, from, CLOSE);
            if (StringUtils.isNotEmpty(result)) {
                result = result.trim();
            }
            return isNull(result) ? null : Long.valueOf(result);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static BigDecimal parseBigDecimalBracket(String str, String from) {
        if (isNull(str)) {
            return BigDecimal.ZERO;
        }
        try {
            String result = sb(str, from, CLOSE);
            if (StringUtils.isNotEmpty(result)) {
                result = result.trim();
            }
            return isNull(result) ? null : new BigDecimal(result);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigInteger parseBigIntegerBracket(String str, String from) {
        if (isNull(str)) {
            return BigInteger.ZERO;
        }
        try {
            String result = sb(str, from, CLOSE);
            if (StringUtils.isNotEmpty(result)) {
                result = result.trim();
            }
            return isNull(result) ? null : new BigInteger(result);
        } catch (Exception e) {
            return BigInteger.ZERO;
        }
    }

    private static Byte parseByteSpace(String str, String from) {
        try {
            String result = sb(str, from, SPACE);
            return isNull(result) ? null : Byte.parseByte(result);
        } catch (Exception e) {
            return null;
        }
    }

    private static Byte parseByteBracket(String str, String from) {
        String result = sb(str, from, CLOSE);
        return isNull(result) ? null : Byte.parseByte(result);
    }

    /**
     * Finds matching closing bracket in string starting with pattern
     */
    public static int findPosOfClosingBracket(String str, String pattern) {
        int i;
        int openBracketIndex = pattern.indexOf(OPEN);
        int index = str.indexOf(pattern) + openBracketIndex;

        ArrayDeque<Integer> st = new ArrayDeque<>();

        for (i = index; i < str.length(); i++) {
            if (str.charAt(i) == '(') {
                st.push((int) str.charAt(i));
            } else if (str.charAt(i) == ')') {
                if (st.isEmpty()) {
                    return i;
                }
                st.pop();
                if (st.isEmpty()) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Finds single string-block starting with pattern and ending with CLOSE
     */
    public static String sbb(String str, String pattern) {
        if (isNull(str) || !str.contains(pattern))
            return null;

        int openindex = str.indexOf(pattern) + pattern.indexOf(OPEN);
        int closeIndex = LiteClientParser.findPosOfClosingBracket(str, pattern);
        if (closeIndex == -1) {
            return null;
        }
        return str.substring(openindex, closeIndex + 1).trim();
    }

    /**
     * Finds multiple string-blocks starting with pattern and ending with CLOSE
     */
    public static List<String> findStringBlocks(String str, String pattern) {
        List<String> result = new ArrayList<>();

        if (isNull(str) || !str.contains(pattern))
            return result;

        int fromIndex = 0;
        int foundIndex = str.indexOf(pattern, fromIndex);

        while (foundIndex != -1) {
            foundIndex += pattern.indexOf(OPEN);
            String foundPattern = sbb(str.substring(fromIndex), pattern);
            if (nonNull(foundPattern)) {
                result.add(foundPattern);
                fromIndex = foundIndex + foundPattern.length();
                foundIndex = str.indexOf(pattern, fromIndex);
            } else {
                foundIndex = -1;
            }
        }
        return result;
    }

    /**
     * copy of the method findStringBlocks() but additionally prepends result with label value
     */
    public static List<String> findLeafsWithLabel(String str, String pattern) {
        List<String> result = new ArrayList<>();

        if (isNull(str) || !str.contains(pattern))
            return result;

        int fromIndex = 0;
        int foundIndex = str.indexOf(pattern, fromIndex);

        while (foundIndex != -1) {
            foundIndex += pattern.indexOf(OPEN);
            String foundPattern = sbb(str.substring(fromIndex), pattern);
            if (nonNull(foundPattern)) {
                result.add(str.substring(foundIndex - 150, foundIndex) + foundPattern);
                fromIndex = foundIndex + foundPattern.length();
                foundIndex = str.indexOf(pattern, fromIndex);
            } else {
                foundIndex = -1;
            }
        }

        return result;
    }


    public static LiteClientAccountState parseGetAccount(String stdout) {

        if (isNull(stdout)) {
            return LiteClientAccountState.builder().build();
        }

        if (stdout.contains("account state is empty")) {
            return LiteClientAccountState.builder().build();
        }

        try {
            String accountState = stdout.replace(EOLWIN, SPACE).replace(EOL, SPACE);

            String addr = sbb(accountState, "addr:(");
            String address = sb(addr, ADDRESS_COLON, CLOSE);
            if (StringUtils.isNotEmpty(address)) {
                address = address.substring(1).toUpperCase();
            }
            Long wc = parseLongSpace(addr, WORKCHAIN_ID_COLON);

            String storageStat = sbb(accountState, "storage_stat:(");
            String usedCellsStr = sbb(storageStat, "cells:(");
            String usedBitsStr = sbb(storageStat, "bits:(");
            String usedPublicCellsStr = sbb(storageStat, "public_cells:(");

            Long usedCells = parseLongBracket(usedCellsStr, VALUE_COLON);
            Long usedBits = parseLongBracket(usedBitsStr, VALUE_COLON);
            Long usedPublicCells = parseLongBracket(usedPublicCellsStr, VALUE_COLON);
            BigDecimal lastPaid = parseBigDecimalSpace(storageStat, "last_paid:");

            String storage = sbb(accountState, "storage:(");
            BigDecimal storageLastTxLt = parseBigDecimalSpace(storage, "last_trans_lt:");
            Value storageBalanceValue = readValue(storage);

            String state = sbb(accountState, "state:(");
            String stateAccountStatus;

            if (StringUtils.isNotEmpty(state)) { // state:(account_active
                stateAccountStatus = sb(accountState, "state:(", SPACE);
            } else {
                stateAccountStatus = sb(accountState, "state:", CLOSE);
            }

            if (stateAccountStatus.contains("account_active")) {
                stateAccountStatus = "Active";
            } else if (stateAccountStatus.contains("account_uninit")) {
                stateAccountStatus = "Uninitialized";
            } else {
                stateAccountStatus = "Frozen";
            }

            String code = sbb(state, "code:(");
            String[] codeCells = isNull(code) ? new String[0] : code.split("x\\{");
            ArrayList<String> stateAccountCode = cleanCells(new ArrayList<>(Arrays.asList(codeCells)));

            String data = sbb(state, "data:(");
            String[] dataCells = isNull(data) ? new String[0] : data.split("x\\{");
            ArrayList<String> stateAccountData = cleanCells(new ArrayList<>(Arrays.asList(dataCells)));

            String stateAccountLibrary = sbb(state, "library:(");
            List<Library> libraries = readLibraries(stateAccountLibrary);

            BigInteger lastTxLt = parseBigIntegerSpace(accountState, "last transaction lt = ");
            String lastTxHash = sb(accountState, "hash = ", SPACE);
            StorageInfo storageInfo = StorageInfo.builder()
                    .usedCells(usedCells)
                    .usedBits(usedBits)
                    .usedPublicCells(usedPublicCells)
                    .lastPaid(lastPaid)
                    .build();

            return LiteClientAccountState.builder()
                    .wc(wc)
                    .address(address)
                    .balance(storageBalanceValue)
                    .storageInfo(storageInfo)
                    .storageLastTxLt(storageLastTxLt)
                    .status(stateAccountStatus)
                    .stateCode(String.join("", stateAccountCode))
                    .stateData(String.join("", stateAccountData))
                    .stateLibrary(libraries)
                    .lastTxLt(lastTxLt)
                    .lastTxHash(lastTxHash)
                    .build();

        } catch (Exception e) {
            return LiteClientAccountState.builder().build();
        }
    }

    public static long parseRunMethodSeqno(String stdout) {
        try {
            return Long.parseLong(StringUtils.substringBetween(stdout, "result:  [", "]").trim());
        } catch (Exception e) {
            return -1L;
        }
    }

    public static List<ResultListParticipants> parseRunMethodParticipantList(String stdout) {

        if (StringUtils.isEmpty(stdout) || !stdout.contains("participant_list"))
            return Collections.emptyList();

        if (stdout.contains("cannot parse answer")) {
            return Collections.emptyList();
        }

        String result = StringUtils.substringBetween(stdout, "result:  [ (", ") ]");

        result = result.replace("] [", ",");
        result = result.replace("[", "");
        result = result.replace("]", "");
        String[] participants = result.split(",");

        List<ResultListParticipants> participantsList = new ArrayList<>();
        if (result.length() == 0) {
            return participantsList;
        }
        for (String participant : participants) {
            String[] entry = participant.split(" ");
            participantsList.add(ResultListParticipants.builder().pubkey(entry[0]).weight(entry[1]).build());
        }
        return participantsList;
    }

    public static ResultComputeReturnStake parseRunMethodComputeReturnStake(String stdout) {

        log.debug("parseRunMethodComputeReturnStake {}", stdout);

        if (StringUtils.isEmpty(stdout) || !stdout.contains("compute_returned_stake"))
            return ResultComputeReturnStake.builder()
                    .stake(new BigDecimal("-1"))
                    .build();

        if (stdout.contains("cannot parse answer")) {
            return ResultComputeReturnStake.builder()
                    .stake(new BigDecimal("-1"))
                    .build();
        }

        String result = StringUtils.substringBetween(stdout, "result:  [", "]");

        return ResultComputeReturnStake.builder()
                .stake(new BigDecimal(result.trim()))
                .build();
    }
}
