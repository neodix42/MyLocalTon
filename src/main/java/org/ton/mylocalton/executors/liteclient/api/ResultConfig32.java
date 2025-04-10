package org.ton.mylocalton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.mylocalton.executors.liteclient.api.config.Validators;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig32 implements Serializable {
  private Validators validators;
}
