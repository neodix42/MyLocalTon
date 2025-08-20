package org.ton.mylocalton.db.entities;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.ton.mylocalton.utils.WalletVersion;
import org.ton.ton4j.tlb.Account;
import org.ton.mylocalton.wallet.WalletAddress;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@Builder
@Data
@IdClass(WalletPk.class)
public class WalletEntity {
  @Id Long wc;
  @Id String hexAddress;

  long seqno;

  WalletVersion walletVersion;
  WalletAddress wallet;
  Account accountState;
  String accountStatus;

  Boolean mainWalletInstalled;
  Boolean configWalletInstalled;
  Long createdAt;

  public WalletPk getPrimaryKey() {
    return WalletPk.builder().wc(wc).hexAddress(StringUtils.upperCase(hexAddress)).build();
  }

  public void setAccountState(Account accountState) {
    this.accountState = accountState;
    this.accountStatus = accountState.getAccountState();
  }

  public String getFullAddress() {
    return (wc + ":" + hexAddress).toUpperCase();
  }

  public String getAccountStatus() {
    return accountState.getAccountState();
  }
}
