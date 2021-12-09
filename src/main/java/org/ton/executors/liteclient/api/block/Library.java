package org.ton.executors.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Builder
@ToString
@Getter
public class Library implements Serializable {
    String label;
    String type;
    Long publicFlag;
    List<String> rawData;
}
