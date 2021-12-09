package org.ton.db.entities;

import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class WalletPk {
    Long wc;
    String hexAddress;
}