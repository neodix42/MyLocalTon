package org.ton.db.entities;

import lombok.*;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.wallet.WalletAddress;

import javax.persistence.Entity;

@Entity
@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WalletEntity {
    Long wc;
    String hexAddress;

    Long subWalletId;
    String walletVersion; //walletV1, walletV2, walletV3 - orientDB does not support enum
    WalletAddress wallet;
    AccountState accountState; //status and balance
    Boolean preinstalled;
    Boolean mainWalletInstalled;
    Boolean configWalletInstalled;
    Long createdAt;

    public WalletPk getPrimaryKey() {
        return WalletPk.builder()
                .wc(wc)
                .hexAddress(hexAddress)
                .build();
    }

    public String getFullAddress() {
        return wc + ":" + hexAddress;
    }
}