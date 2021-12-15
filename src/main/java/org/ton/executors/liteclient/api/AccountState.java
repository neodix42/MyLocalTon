package org.ton.executors.liteclient.api;

import lombok.*;
import org.ton.executors.liteclient.api.block.Library;
import org.ton.executors.liteclient.api.block.Value;

import java.io.Serializable;
import java.util.List;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountState implements Serializable {
    private Long wc;
    private String address;
    private Value balance;
    private StorageInfo storageInfo;
    private String storageLastTxLt;
    //duepayment TODO
    private String status;
    private List<String> stateCode;
    private List<String> stateData;
    private List<Library> stateLibrary;
    private String lastTxLt;
    private String lastTxHash;
}
