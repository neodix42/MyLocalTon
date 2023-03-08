package org.ton.callables.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.db.DB2;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.executors.liteclient.api.LiteClientAccountState;
import org.ton.java.smartcontract.types.WalletVersion;

import java.util.List;

@Builder
@Getter
@Setter
public class WalletCallbackParam {
    DB2 db;
    WalletPk walletPk;
    WalletEntity foundWallet;
    List<WalletEntity> foundWallets;
    LiteClientAccountState accountState;
    Long seqno;
    String status;
    String searchText;
    WalletVersion walletVersion;
    Long subWalletId;
}
