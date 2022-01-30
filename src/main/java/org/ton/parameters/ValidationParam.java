package org.ton.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class ValidationParam {
    long totalNodes;
    long validatorNodes;
    long blockchainLaunchTime;
    long startCycle;
    long endCycle;
    long startElections;
    long endElections;
    long nextElections;
    String minterAddr;
    String configAddr;
    String electorAddr;
    long electionDuration;
    long validationDuration;
    long holdPeriod;
    BigInteger minStake;
    BigInteger maxStake;
}
