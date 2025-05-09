package org.ton.mylocalton.data.utils;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.java.address.Address;
import org.ton.java.smartcontract.wallet.Contract;
import org.ton.java.smartcontract.wallet.v3.WalletV3R2;
import org.ton.java.tonlib.Tonlib;
import org.ton.java.utils.Utils;

@Slf4j
public class MyUtils {
  public BigInteger getBalance(Tonlib tonlib, Address address) {
    return new BigInteger(tonlib.getRawAccountState(address).getBalance());
  }

  /**
   * returns if balance has changed by tolerateNanoCoins either side within timeoutSeconds,
   * otherwise throws an error.
   *
   * @param timeoutSeconds timeout in seconds
   * @param tolerateNanoCoins tolerate value
   */
  public void waitForBalanceChangeWithTolerance(
      Tonlib tonlib, Address address, int timeoutSeconds, BigInteger tolerateNanoCoins) {

    BigInteger initialBalance = getBalance(tonlib, address);
    long diff;
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        break;
      }
      Utils.sleep(2);
      BigInteger currentBalance = getBalance(tonlib, address);

      diff =
          Math.max(currentBalance.longValue(), initialBalance.longValue())
              - Math.min(currentBalance.longValue(), initialBalance.longValue());
    } while (diff < tolerateNanoCoins.longValue());
  }

  public Contract deploy(Tonlib tonlib, BigInteger topUpAmount) {
    long walletId = Math.abs(Utils.getRandomInt());
    WalletV3R2 contract = WalletV3R2.builder().tonlib(tonlib).walletId(walletId).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, topUpAmount);
    tonlib.waitForBalanceChange(contract.getAddress(), 60);
    contract.deploy();
    contract.waitForDeployment();

    return contract;
  }
}
