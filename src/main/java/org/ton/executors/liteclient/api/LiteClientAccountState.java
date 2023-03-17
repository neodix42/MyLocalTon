package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.executors.liteclient.api.block.Library;
import org.ton.executors.liteclient.api.block.Value;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Builder
@Getter
@ToString
public class LiteClientAccountState implements Serializable {
    private Long wc;
    private String address;
    private Value balance;
    private StorageInfo storageInfo;
    private BigDecimal storageLastTxLt;
    //duepayment TODO
    private String status;
    private String stateCode;
    private String stateData;
    private List<Library> stateLibrary;
    private BigInteger lastTxLt;
    private String lastTxHash;
}
