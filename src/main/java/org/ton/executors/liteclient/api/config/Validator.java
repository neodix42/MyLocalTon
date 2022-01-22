package org.ton.executors.liteclient.api.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class Validator {
    String publicKey;
    String adnlAddress;
    BigInteger weight;
}
