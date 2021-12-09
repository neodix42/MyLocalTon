package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * action - is absent if and only if the computing phase was unsuccessful.
 * The aborted flag is set either if there is no action phase or if the action phase was unsuccessful.
 * The bounce phase occurs only if the aborted flag is set and the inbound message was bounceable.
 */
@Builder
@ToString
@Getter
public class TransactionDescription implements Serializable {
    /*
    Tick transactions — Automatically invoked for certain special accounts
    (smart contracts) in the masterchain that have the tick flag set in
    their state, as the very first transactions in every masterchain block.
    They have no inbound message, but may generate outbound messages
    and change the account state. For instance, validator elections are
    performed by tick transactions of special smart contracts in the masterchain.

    Tock transactions — Similarly automatically invoked as the very last
    transactions in every masterchain block for certain special accounts.
     */

    String type; // tick, tock, ordinary

    TransactionStorage storage;
    TransactionCredit credit;
    TransactionCompute compute;
    TransactionAction action;
    // bounce phase omitted
    Byte aborted;
    Byte bounce;
    Byte destroyed;

    BigDecimal creditFirst;
}
