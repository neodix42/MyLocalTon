package org.ton.db.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.ton.executors.liteclient.api.LiteClientAccountState;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.tonlib.types.AccountState;
import org.ton.wallet.WalletAddress;

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

    long seqno;

    WalletVersion walletVersion;
    WalletAddress wallet;
    LiteClientAccountState accountState;
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

    private String getStateFromCode(AccountState state) {
        if (StringUtils.isEmpty(state.getCode())) {
            if (StringUtils.isEmpty(state.getFrozen_hash())) {
                return "uninitialized";
            } else {
                return "frozen";
            }
        } else {
            return "active";
        }
    }
}