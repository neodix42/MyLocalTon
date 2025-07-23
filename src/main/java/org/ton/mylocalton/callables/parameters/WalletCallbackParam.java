package org.ton.mylocalton.callables.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.mylocalton.db.DB2;
import org.ton.mylocalton.db.entities.WalletEntity;
import org.ton.mylocalton.db.entities.WalletPk;
import org.ton.ton4j.smartcontract.types.WalletVersion;
import org.ton.ton4j.tonlib.types.RawAccountState;

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
