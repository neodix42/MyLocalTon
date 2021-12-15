package org.ton.executors.liteclient.api.block;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * located in block under in_msg_descr:value:in_msg
 */
@Builder
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Message implements Serializable {
    Address srcAddr;
    Address destAddr;
    String type;
    Value value;
    Long createdAt;
    String createdLt;
    Byte ihrDisabled;
    Byte bounce;
    Byte bounced;
    Body body;
    Init init;
    BigDecimal fwdFee;
    BigDecimal importFee;
    BigDecimal ihrFee;
}
