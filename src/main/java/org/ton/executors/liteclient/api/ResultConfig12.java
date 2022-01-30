package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig12 implements Serializable {
    private long enabledSince;
    private long actualMinSplit;
    private long minSplit;
    private long maxSplit;
    private long basic;
    private long active;
    private long acceptMsg;
    private long flags;
    private String rootHash;
    private String fileHash;
    private long version;
}

