package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;

@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Address implements Serializable {
    Long wc;
    String addr;
}
