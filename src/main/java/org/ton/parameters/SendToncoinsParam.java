package org.ton.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.smartcontract.types.WalletVersion;
import org.ton.settings.Node;
import org.ton.wallet.WalletAddress;

import java.math.BigDecimal;
import java.math.BigInteger;

@Builder
@Setter
@Getter
public class SendToncoinsParam {
    Node executionNode;
    WalletAddress fromWallet;
    WalletVersion fromWalletVersion;
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
