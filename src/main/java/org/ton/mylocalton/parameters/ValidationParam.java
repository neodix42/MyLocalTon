package org.ton.mylocalton.parameters;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.ton4j.adnl.Participant;
import org.ton.ton4j.tlb.ValidatorAddr;

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
  List<Participant> participants;
  List<ValidatorAddr> previousValidators;
  List<ValidatorAddr> currentValidators;
  List<ValidatorAddr> nextValidators;
}
