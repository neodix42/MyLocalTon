package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig15 implements Serializable {
    //    validators_elected_for:4000 elections_start_before:2000 elections_end_before:500 stake_held_for:1000
    private long validatorsElectedFor;
    private long electionsStartBefore;
    private long electionsEndBefore;
    private long stakeHeldFor;
}

