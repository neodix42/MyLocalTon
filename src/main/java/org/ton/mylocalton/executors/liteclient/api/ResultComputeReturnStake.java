package org.ton.mylocalton.executors.liteclient.api;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@ToString
public class ResultComputeReturnStake implements Serializable {
  private BigInteger stake;
}
