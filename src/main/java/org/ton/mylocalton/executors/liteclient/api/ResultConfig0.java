package org.ton.mylocalton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig0 implements Serializable {
  private String configSmcAddr;
}
