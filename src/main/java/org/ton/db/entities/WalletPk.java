package org.ton.db.entities;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@ToString
@Getter
@Setter
public class WalletPk {
    Long wc;
    String hexAddress;
}