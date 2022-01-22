package org.ton.executors.liteclient.api;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.executors.liteclient.api.config.Validators;

import java.io.Serializable;

@Builder
@Getter
@ToString
public class ResultConfig34 implements Serializable {
    private Validators validators;
}

