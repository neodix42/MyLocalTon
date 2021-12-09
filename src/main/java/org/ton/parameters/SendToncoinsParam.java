package org.ton.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.ton.settings.Node;
import org.ton.wallet.WalletAddress;
import org.ton.wallet.WalletVersion;

import java.math.BigDecimal;

@Slf4j
@Builder
@Setter
@Getter
public class SendToncoinsParam {
    Node executionNode;
    WalletAddress fromWallet;
    WalletVersion fromWalletVersion;
    Long fromSubWalletId;
    String destAddr;
    BigDecimal amount;
    String bocLocation;
    String comment;
    Boolean clearBounce;
    Boolean forceBounce;
    Long timeout;
//    Currency extraCurrency; // TODO
}
