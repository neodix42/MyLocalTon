package org.ton.mylocalton.data;

import com.iwebpp.crypto.TweetNaclFast;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.highload.HighloadWallet;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.HighloadConfig;
import org.ton.ton4j.utils.Utils;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.mylocalton.data.db.DataWalletEntity;
import org.ton.mylocalton.data.db.DataWalletPk;
import org.ton.mylocalton.data.scenarios.Scenario;

@Slf4j
@Builder
public class Runner {

  private static final byte[] dataFaucetHighLoadPrvKey =
      Utils.hexToSignedBytes("f2480435871753a968ef08edabb24f5532dab4cd904dbdc683b8576fb45fa697");
  public static Address dataHighloadFaucetAddress;
  public static DataDB dataDB;
  private final List<String> scenarios = new ArrayList<>();
  private HighloadWallet dataHighloadFaucet;
  private long period;

  private AdnlLiteClient adnlLiteClient;

  private static Runnable errorHandlingWrapper(Runnable action) {
    return () -> {
      try {
        action.run();
      } catch (Throwable e) {
        e.printStackTrace();
      }
    };
  }

  public void run() {
    try {
      log.info("started data-generator runner");

      log.info(Utils.bytesToHex(dataFaucetHighLoadPrvKey));
      TweetNaclFast.Signature.KeyPair keyPair =
          Utils.generateSignatureKeyPairFromSeed(dataFaucetHighLoadPrvKey);

      dataHighloadFaucet =
          HighloadWallet.builder()
              .adnlLiteClient(adnlLiteClient)
              .keyPair(keyPair)
              .wc(-1)
              .walletId(42)
              .queryId(BigInteger.ZERO)
              .build();

      dataHighloadFaucetAddress = dataHighloadFaucet.getAddress();
      log.info(
          "highload faucet address {}, balance {}",
          dataHighloadFaucet.getAddress().toRaw(),
          Utils.formatNanoValue(dataHighloadFaucet.getBalance()));

      log.info(
          "highload faucet pending queue size {}, total queue size {}",
          DataDB.getDataWalletsToSend().size(),
          DataDB.getTotalDataWallets());
    } catch (Exception e) {
      log.error("Error: " + e.getMessage());
    }

    scenarios.add("Scenario1");
    scenarios.add("Scenario2");
    scenarios.add("Scenario3");
    scenarios.add("Scenario4");
    scenarios.add("Scenario5");
    scenarios.add("Scenario6");
    scenarios.add("Scenario7");
    scenarios.add("Scenario8");
    scenarios.add("Scenario9");
    scenarios.add("Scenario10");
    scenarios.add("Scenario11");
    scenarios.add("Scenario12");
    scenarios.add("Scenario13");

    //    scenarios.add(
    //        "Scenario14"); // works only when workchain enabled, order contract created at
    // workchain=0

    scenarios.add("Scenario15");
    scenarios.add("Scenario16");

    runTopUpQueueScheduler();
    runScenariosScheduler();
    runCleanQueueScheduler();

    log.info("data-app ready");
  }

  public void runTopUpQueueScheduler() {

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    Runnable topUpTask =
        errorHandlingWrapper(
            () -> {
              try {
                Thread.currentThread().setName("SendRequest");

                List<DataWalletEntity> walletRequests = DataDB.getDataWalletsToSend();
                log.info("triggered send to {} wallets", walletRequests.size());
                if (walletRequests.isEmpty()) {
                  log.info(
                      "queue is empty, nothing to do, was sent {}",
                      DataDB.getDataWalletsSent().size());
                  return;
                }

                if (dataHighloadFaucet.getBalance().longValue() <= 0) {
                  log.error("dataHighloadFaucet has no funds");
                  return;
                }

                List<Destination> destinations = new ArrayList<>();
                for (DataWalletEntity wallet : walletRequests) {
                  destinations.add(
                      Destination.builder()
                          .bounce(false)
                          .address(wallet.getWalletAddress())
                          .amount(wallet.getBalance())
                          .build());
                }

                List<Destination> destinations250 =
                    new ArrayList<>(destinations.subList(0, Math.min(destinations.size(), 250)));
                destinations.subList(0, Math.min(destinations.size(), 250)).clear();

                HighloadConfig config =
                    HighloadConfig.builder()
                        .walletId(42)
                        .queryId(BigInteger.valueOf(Instant.now().getEpochSecond() + 60L << 32))
                        .destinations(destinations250)
                        .build();

                SendResponse sendResponse = dataHighloadFaucet.send(config);
                log.info("sendResponse top up {}", sendResponse);

                for (Destination destination : destinations250) {
                  DataDB.updateDataWalletStatus(
                      DataWalletPk.builder().walletAddress(destination.getAddress()).build(),
                      "sent");
                }
                log.info("data-app faucet updated");
              } catch (Throwable e) {
                log.info("error in data-app " + e.getMessage());
              }
            });

    executor.scheduleAtFixedRate(topUpTask, 30, 15, TimeUnit.SECONDS);
  }

  public void runScenariosScheduler() {

    for (String scenario : scenarios) {

      Executors.newSingleThreadScheduledExecutor()
          .scheduleAtFixedRate(
              () -> {
                Thread.currentThread().setName(scenario);
                Executors.newSingleThreadExecutor()
                    .submit(
                        () -> {
                          Class<?> clazz;
                          try {

                            if (dataHighloadFaucet.getBalance().longValue() <= 0) {
                              log.error("dataHighloadFaucet has no funds");
                              return;
                            }
                            clazz = Class.forName("org.ton.mylocalton.data.scenarios." + scenario);
                            Constructor<?> constructor = clazz.getConstructor(AdnlLiteClient.class);
                            Object instance = constructor.newInstance(adnlLiteClient);
                            ((Scenario) instance).run();

                          } catch (Throwable e) {
                            log.info("error running scenario {}", e.getMessage());
                          }
                        });
              },
              1,
              period,
              TimeUnit.MINUTES);
    }
  }

  public void runCleanQueueScheduler() {

    Executors.newSingleThreadScheduledExecutor()
        .scheduleAtFixedRate(
            () -> {
              Thread.currentThread().setName("CleanQueue");
              DataDB.deleteExpiredDataWallets(60 * 10);
            },
            2,
            5,
            TimeUnit.MINUTES);
  }
}
