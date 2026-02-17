package org.ton.mylocalton.data.utils;

import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.wallet.Contract;
import org.ton.ton4j.smartcontract.wallet.v3.WalletV3R2;
import org.ton.ton4j.utils.Utils;

@Slf4j
public class MyUtils {
  public BigInteger getBalance(AdnlLiteClient adnlLiteClient, Address address) {
    return adnlLiteClient.getBalance(address);
  }

  /**
   * returns if balance has changed by tolerateNanoCoins either side within timeoutSeconds,
   * otherwise throws an error.
   *
   * @param timeoutSeconds timeout in seconds
   * @param tolerateNanoCoins tolerate value
   */
  public void waitForBalanceChangeWithTolerance(
      AdnlLiteClient adnlLiteClient, Address address, int timeoutSeconds, BigInteger tolerateNanoCoins) {

    BigInteger initialBalance = getBalance(adnlLiteClient, address);
    long diff;
    int i = 0;
    do {
      if (++i * 2 >= timeoutSeconds) {
        break;
      }
      Utils.sleep(2);
      BigInteger currentBalance = getBalance(adnlLiteClient, address);

      diff =
          Math.max(currentBalance.longValue(), initialBalance.longValue())
              - Math.min(currentBalance.longValue(), initialBalance.longValue());
    } while (diff < tolerateNanoCoins.longValue());
  }

  public Contract deploy(AdnlLiteClient adnlLiteClient, BigInteger topUpAmount) {
    long walletId = Math.abs(Utils.getRandomInt());
    WalletV3R2 contract = WalletV3R2.builder().tonProvider(adnlLiteClient).walletId(walletId).build();

    String nonBounceableAddress = contract.getAddress().toNonBounceable();
    DataDB.addDataRequest(nonBounceableAddress, topUpAmount);
    Utils.sleep(20);
    contract.deploy();
    contract.waitForDeployment();

    return contract;
  }
}
