package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Builder
@ToString
@Getter
public class Init implements Serializable {
    List<String> code;
    List<String> data;
    List<String> library;
}
