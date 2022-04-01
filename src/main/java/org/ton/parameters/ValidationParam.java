package org.ton.parameters;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.executors.liteclient.api.ResultListParticipants;
import org.ton.executors.liteclient.api.config.Validator;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class ValidationParam implements Serializable {
    Long totalNodes;
    Long blockchainLaunchTime;
    Long startValidationCycle;
    Long endValidationCycle;
    Long startElections;
    Long endElections;
    Long nextElections;
    Long startElectionsBefore;
    Long endElectionsBefore;
    String minterAddr;
    String configAddr;
    String electorAddr;
    Long electionDuration;
    Long validationDuration;
    Long holdPeriod;
    BigInteger minStake;
    BigInteger maxStake;
    List<ResultListParticipants> participants;
    List<Validator> previousValidators;
    List<Validator> currentValidators;
    List<Validator> nextValidators;
}
