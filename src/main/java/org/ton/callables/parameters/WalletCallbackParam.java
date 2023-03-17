package org.ton.callables.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.db.DB2;
import org.ton.db.entities.WalletEntity;
import org.ton.db.entities.WalletPk;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.java.tonlib.types.RawAccountState;

import java.util.List;

@Builder
@Getter
@Setter
public class WalletCallbackParam {
    DB2 db;
    WalletPk walletPk;
    WalletEntity foundWallet;
    List<WalletEntity> foundWallets;
    RawAccountState accountState;
    Long seqno;
    String status;
    String searchText;
    WalletVersion walletVersion;
    Long subWalletId;
}
