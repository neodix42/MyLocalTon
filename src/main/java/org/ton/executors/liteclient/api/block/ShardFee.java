package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShardFee implements Serializable {
    String label;
    Value extraFees;
    Value extraCreate;
    Value valueFees;
    Value valueCreate;
}
