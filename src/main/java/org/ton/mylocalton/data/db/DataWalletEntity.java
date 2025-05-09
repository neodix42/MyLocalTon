package org.ton.mylocalton.data.db;

import java.math.BigInteger;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import lombok.*;

@Entity
@Builder
@Data
@IdClass(DataWalletPk.class)
public class DataWalletEntity {
  @Id String walletAddress;

  long createdAt;

  BigInteger balance;

  String status;

  public DataWalletPk getPrimaryKey() {
    return DataWalletPk.builder().walletAddress(walletAddress).build();
  }
}
