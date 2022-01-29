package org.ton.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
    long nextElections;
    String configAddr;
    String electorAddr;
    long electionDuration;
    long validationDuration;
    long holdPeriod;
    long minStake;
    long maxStake;
}
