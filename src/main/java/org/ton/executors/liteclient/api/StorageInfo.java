package org.ton.executors.liteclient.api;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StorageInfo implements Serializable {
    private Long usedCells;
    private Long usedBits;
    private Long usedPublicCells;
    private BigDecimal lastPaid;
}
