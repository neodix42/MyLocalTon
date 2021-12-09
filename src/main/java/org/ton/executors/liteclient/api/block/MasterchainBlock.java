package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * https://test.ton.org/tblkch.pdf
 * <p>
 * Masterchain blocks. In addition to shardchain blocks and their
 * states, the TON Blockchain contains masterchain blocks and the masterchain
 * state (also called the global state). The masterchain blocks and state are
 * quite similar to the shardchain blocks and state considered so far, with some
 * notable differences:
 * <p>
 * Consistency conditions
 * <p>
 * The masterchain cannot be split or merged, so a masterchain block
 * usually has exactly one immediate antecessor. The sole exception is the
 * “masterchain block zero”, distinguished by having a sequence number
 * equal to zero; it has no antecessors at all, and contains the initial
 * configuration of the whole TON Blockchain (e.g., the original set of
 * validators).
 * <p>
 * The masterchain blocks contain another important non-split structure:
 * ShardHashes, a binary tree with a list of all defined shardchains along
 * with the hashes of the latest block inside each of the listed shardchains.
 * It is the inclusion of a shardchain block into this structure that makes
 * a shardchain block “canonical”, and enables other shardchains’ blocks
 * to refer to data (e.g., outbound messages) contained in the shardchain
 * block.
 * <p>
 * The state of the masterchain contains global configuration parameters
 * of the whole TON Blockchain, such as the minimum and maximum
 * gas prices, the supported versions of TVM, the minimum stake for the
 * validator candidates, the list of alternative cryptocurrencies supported
 * in addition to Grams, the total amount of Grams issued so far, and
 * the current set of validators responsible for creating and signing new
 * blocks, along with their public keys.
 * <p>
 * The state of the masterchain also contains the code of the smart contracts
 * used to elect the subsequent sets of validators and to modify
 * the global configuration parameters. The code of these smart contracts
 * itself is a part of the global configuration parameters and can be modified
 * accordingly. In this respect, this code (along with the current
 * values of these parameters) functions like a “constitution” for the TON
 * Blockchain. It is initially established in masterchain block zero.
 * <p>
 * There are no transit messages through the masterchain: each inbound
 * message must have a destination inside the masterchain, and each outbound
 * message must have a source inside the masterchain.
 */
@Builder
@ToString
@Getter
@Setter
public class MasterchainBlock implements Serializable {
    Long wc;
    private List<ShardHash> shardHashes;
    private List<ShardFee> shardFees;
    RecoverCreateMessage recoverCreateMsg; // contains in_msg and txs
    MintMessage mintMsg; // contains in_msg and txs
}
