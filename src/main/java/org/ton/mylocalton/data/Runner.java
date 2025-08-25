package org.ton.mylocalton.data;

import com.iwebpp.crypto.TweetNaclFast;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.ton.mylocalton.data.db.DataDB;
import org.ton.mylocalton.data.db.DataWalletEntity;
import org.ton.mylocalton.data.db.DataWalletPk;
import org.ton.mylocalton.data.scenarios.Scenario;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.adnl.AdnlLiteClient;
import org.ton.ton4j.smartcontract.SendResponse;
import org.ton.ton4j.smartcontract.highload.HighloadWallet;
import org.ton.ton4j.smartcontract.types.Destination;
import org.ton.ton4j.smartcontract.types.HighloadConfig;
import org.ton.ton4j.utils.Utils;

@Slf4j
@Builder
public class Runner {

  private static final byte[] dataFaucetHighLoadPrvKey =
      Utils.hexToSignedBytes("f2480435871753a968ef08edabb24f5532dab4cd904dbdc683b8576fb45fa697");
  public static Address dataHighloadFaucetAddress;
  public static DataDB dataDB;
  public static Queue<String> scenarios = new ConcurrentLinkedQueue<>();
  ScheduledExecutorService topUpExecutor;
  Map<String, ScheduledExecutorService> runScenarioExecutor;
  ScheduledExecutorService cleanQueueExecutor;
  private HighloadWallet dataHighloadFaucet;
  private long period;
  private boolean scenario1;
  private boolean scenario2;
  private boolean scenario3;
  private boolean scenario4;
  private boolean scenario5;
  private boolean scenario6;
  private boolean scenario7;
  private boolean scenario8;
  private boolean scenario9;
  private boolean scenario10;
  private boolean scenario11;
  private boolean scenario12;
  private boolean scenario13;
  private boolean scenario14;
  private boolean scenario15;
  private boolean scenario16;
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

  public void stop() {
    log.info("Stopping runner ...");
    topUpExecutor.shutdownNow();
    for (Map.Entry<String, ScheduledExecutorService> executor : runScenarioExecutor.entrySet()) {
      executor.getValue().shutdownNow();
    }
    cleanQueueExecutor.shutdownNow();
    log.info("Stopped runner");
  }

  public void run() {
    try {
      log.info("started data-generator runner");
      runScenarioExecutor = new HashMap<>();
      log.debug(Utils.bytesToHex(dataFaucetHighLoadPrvKey));
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

    if (scenario1) scenarios.add("Scenario1");
    if (scenario2) scenarios.add("Scenario2");
    if (scenario3) scenarios.add("Scenario3");
    if (scenario4) scenarios.add("Scenario4");
    if (scenario5) scenarios.add("Scenario5");
    if (scenario6) scenarios.add("Scenario6");
    if (scenario7) scenarios.add("Scenario7");
    if (scenario8) scenarios.add("Scenario8");
    if (scenario9) scenarios.add("Scenario9");
    if (scenario10) scenarios.add("Scenario10");
    if (scenario11) scenarios.add("Scenario11");
    if (scenario12) scenarios.add("Scenario12");
    if (scenario13) scenarios.add("Scenario13");
//    if (scenario14) scenarios.add("Scenario14"); // works only when workchain enabled, order contract created at
    if (scenario15) scenarios.add("Scenario15");
    if (scenario16) scenarios.add("Scenario16");

    runTopUpQueueScheduler();
    runScenariosScheduler();
    runCleanQueueScheduler();

    log.info("data-app ready");
  }

  public void runTopUpQueueScheduler() {

    topUpExecutor = Executors.newSingleThreadScheduledExecutor();
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

    topUpExecutor.scheduleAtFixedRate(topUpTask, 30, 15, TimeUnit.SECONDS);
  }

  public void runScenariosScheduler() {
    for (String scenario : scenarios) {
      runScenario(scenario);
    }
  }

  public void stopScenario(String scenario) {
    log.debug("stopped and disabled {}", scenario);
    runScenarioExecutor.get(scenario).shutdownNow();
  }

  public void runScenario(String scenario) {
    log.debug("started and enabled {}", scenario);

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    runScenarioExecutor.put(scenario, executor);
    executor.scheduleAtFixedRate(
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
        60,
        period,
        TimeUnit.SECONDS);
  }

  public void runCleanQueueScheduler() {

    cleanQueueExecutor = Executors.newSingleThreadScheduledExecutor();
    cleanQueueExecutor.scheduleAtFixedRate(
        () -> {
          Thread.currentThread().setName("CleanQueue");
          DataDB.deleteExpiredDataWallets(60 * 10);
        },
        2,
        5,
        TimeUnit.MINUTES);
  }
}
