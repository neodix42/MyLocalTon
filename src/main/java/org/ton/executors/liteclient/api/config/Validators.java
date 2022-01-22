package org.ton.executors.liteclient.api.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;
import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class Validators {
    long since; // utime_since
    long until; // utime_until
    long total; // total
    long main; //
    BigInteger totalWeight; // total_weight:100
    List<Validator> validators;
}
