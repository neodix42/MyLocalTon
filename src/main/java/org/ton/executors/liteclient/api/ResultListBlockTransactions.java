package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Getter
@ToString
public class ResultListBlockTransactions implements Serializable {
    private BigInteger txSeqno;
    private String accountAddress;
    private BigInteger lt;
    private String hash;
}
