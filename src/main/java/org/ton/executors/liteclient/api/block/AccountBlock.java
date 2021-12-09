package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Builder
@ToString
@Getter
public class AccountBlock implements Serializable {
    //String accountAddr
    private List<Transaction> transactions;
    //String oldHash
    //String newHash
}
