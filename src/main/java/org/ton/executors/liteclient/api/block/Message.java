package org.ton.executors.liteclient.api.block;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

/** located in block under in_msg_descr:value:in_msg */
@Builder
@ToString
@Getter
public class Message implements Serializable {
  LiteClientAddress srcAddr;
  LiteClientAddress destAddr;
  String type;
  Value value;
  BigInteger createdAt;
  BigInteger createdLt;
  Byte ihrDisabled;
  Byte bounce;
  Byte bounced;
  Body body;
  Init init;
  BigDecimal fwdFee;
  BigDecimal importFee;
  BigDecimal ihrFee;

  public String toJson() {
    Gson g = new GsonBuilder().setPrettyPrinting().create();
    return g.toJson(this);
  }
}
