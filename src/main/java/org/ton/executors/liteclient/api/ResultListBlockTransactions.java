package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@ToString
@Getter
@Setter
public class ResultListBlockTransactions implements Serializable {
    private String txSeqno;
    private String accountAddress;
    private String lt;
    private String hash;
}
