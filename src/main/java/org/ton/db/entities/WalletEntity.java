package org.ton.db.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.ton.executors.liteclient.api.AccountState;
import org.ton.wallet.WalletAddress;
import org.ton.wallet.WalletVersion;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@Builder
@ToString
@Getter
@Setter
@IdClass(WalletPk.class)
public class WalletEntity {
    @Id
    Long wc;
    @Id
    String hexAddress;

    Long subWalletId;
    long seqno;

    WalletVersion walletVersion; //walletV1, walletV2, walletV3
    WalletAddress wallet;
    AccountState accountState; //status and balance
    Boolean preinstalled;
    Boolean mainWalletInstalled;
    Boolean configWalletInstalled;
    Long createdAt;

    public WalletPk getPrimaryKey() {
        return WalletPk.builder()
                .wc(wc)
                .hexAddress(StringUtils.upperCase(hexAddress))
                .build();
    }

    public String getFullAddress() {
        return (wc + ":" + hexAddress).toUpperCase();
    }
}