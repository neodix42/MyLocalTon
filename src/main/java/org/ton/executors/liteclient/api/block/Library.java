package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Library implements Serializable {
    String label;
    String type;
    Long publicFlag;
    private List<String> rawData;
}
