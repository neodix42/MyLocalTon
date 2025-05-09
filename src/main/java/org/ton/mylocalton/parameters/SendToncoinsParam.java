package org.ton.mylocalton.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.mylocalton.settings.Node;
import org.ton.mylocalton.wallet.WalletAddress;

import java.math.BigDecimal;
import java.math.BigInteger;

@Builder
@Setter
@Getter
public class SendToncoinsParam {
  Node executionNode;
  WalletAddress fromWallet;
  WalletVersion fromWalletVersion;
  Long workchain;
  Long fromSubWalletId;
  String destAddr;
  BigInteger amount;
  String bocLocation;
  String comment;
  Boolean forceBounce;
  Long timeout;

  //    Currency extraCurrency; // TODO
  public BigDecimal getToncoinsAmount() {
    return new BigDecimal(amount).divide(new BigDecimal(1_000_000_000));
  }
}
