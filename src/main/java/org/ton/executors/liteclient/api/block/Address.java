package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@ToString
@Getter
public class Address implements Serializable {
    Long wc;
    String addr;
}
