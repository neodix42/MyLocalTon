package org.ton.mylocalton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@ToString
@Getter
public class LiteClientAddress implements Serializable {
  Long wc;
  String addr;
}
