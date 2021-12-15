package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountBlock implements Serializable {
    //String accountAddr
    private List<Transaction> transactions;
    //String oldHash
    //String newHash
}
