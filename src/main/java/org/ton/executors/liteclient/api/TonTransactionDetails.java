package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class TonTransactionDetails implements Serializable {

    private boolean parsedOk;
    private String origStatus;
    private String endStatus;
    private byte msgIhrDisabled;
    private byte msgBounce;
    private byte msgBounced;
    private String src;
    private String dest;
    
    private long amount; // TODO BigDecimal
    private long gasUsed;
    private long gasLimit;
    private long totalFees;

    private byte actionSuccess;
    private byte actionValid;
    private byte actionNoFunds;
    private byte actionAborted;
    private byte actionDestroyed;
}
